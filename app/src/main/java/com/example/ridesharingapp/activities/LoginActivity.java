package com.example.ridesharingapp.activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ridesharingapp.R;
import com.example.ridesharingapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnSignUp;
    private ProgressBar progressBar;

    private FirebaseAuth auth;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp);
        progressBar = findViewById(R.id.progressBar);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnLogin.setOnClickListener(v -> loginUser());
        btnSignUp.setOnClickListener(v -> {
            // Navigate to SignUpActivity
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });

        // --- Toggle password visibility ---
        final boolean[] visible = {false};
        etPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2; // index for drawableEnd
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable[] drawables = etPassword.getCompoundDrawablesRelative();
                Drawable drawableEnd = drawables[DRAWABLE_END];
                if (drawableEnd != null) {
                    int width = etPassword.getWidth();
                    int paddingEnd = etPassword.getPaddingEnd();
                    int touchX = (int) event.getX();
                    int iconAreaStart = width - paddingEnd - drawableEnd.getBounds().width();

                    if (touchX >= iconAreaStart) {
                        // toggle visibility
                        visible[0] = !visible[0];
                        if (visible[0]) {
                            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        } else {
                            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        }
                        etPassword.setSelection(etPassword.getText().length()); // keep cursor at end
                        return true; // consume event
                    }
                }
            }
            return false;
        });
    }

    @Override
    public void onStart(){
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            // We must check their user type before directing them.
            checkUserType(currentUser.getUid());
        }
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password required");
            return;
        }

        progressBar.setVisibility(ProgressBar.VISIBLE);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    if (task.isSuccessful()) {
                        checkUserType(auth.getCurrentUser().getUid());
                    } else {
                        Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserType(String uid) {
        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            Intent intent;
                            if ("driver".equals(user.getUserType())) {
                                intent = new Intent(LoginActivity.this, MainActivity.class);
                            } else {
                                intent = new Intent(LoginActivity.this, RiderMainActivity.class);
                            }

                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "User profile does not exist", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, "Database error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}
