package com.example.ridesharingapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.ridesharingapp.R;
import com.example.ridesharingapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private FirebaseStorage storage;
    private TextView tvName, tvEmail;
    private Button btnLogout, btnChangePic;
    CircleImageView ivProfilePic;
    private String currentUserId;
    private User currentUser;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth =FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance("https://ridesharingapp-ee55d-default-rtdb.asia-southeast1.firebasedatabase.app/");
        storage = FirebaseStorage.getInstance();

        // initial setup check
        if(auth.getCurrentUser() == null){
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        currentUserId = auth.getCurrentUser().getUid();

        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        btnLogout = findViewById(R.id.btnLogout);
        btnChangePic = findViewById(R.id.btnChangePic);
        ivProfilePic = findViewById(R.id.ivProfilePic);

        // initialize the Image Picker Launcher
        setupImagePickerLauncher();

        loadUserProfile();

        btnLogout.setOnClickListener(v -> logoutUser());
        btnChangePic.setOnClickListener(v -> selectImage());
    }

    private void setupImagePickerLauncher(){
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if(uri != null){
                        // Image selected, now upload it
                        uploadImageToFirebase(uri);
                    }
                }
        );
    }

    private void selectImage() {
        // Launches the system's image picker to get content of type "image/*"
        imagePickerLauncher.launch("image/*");
    }

    private void uploadImageToFirebase(Uri imageUri) {
        if (imageUri == null) return;

        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();

        // Create a unique file path in Firebase Storage: users/{uid}/profile_pic/{unique_id}.jpg
        StorageReference storageRef = storage.getReference()
                .child("users")
                .child(currentUserId)
                .child("profile_pic/" + UUID.randomUUID().toString());

        // Upload the file
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL once upload is successful
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();

                        // 4. Update the user's profileImageUrl in Firebase Database
                        updateProfilePicInDatabase(downloadUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateProfilePicInDatabase(String downloadUrl) {
        if (currentUser == null) return;

        // Update the profileImageUrl field
        database.getReference("users")
                .child(currentUserId)
                .child("profileImageUrl")
                .setValue(downloadUrl)
                .addOnSuccessListener(aVoid -> {
                    // Update local user object and UI
                    currentUser.setProfileImageUrl(downloadUrl);
                    loadProfilePic(downloadUrl);
                    Toast.makeText(ProfileActivity.this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Failed to update profile URL.", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserProfile(){
        if(auth.getCurrentUser() == null){
            Toast.makeText(this, "Not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        database.getReference("users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if(user != null){
                            tvName.setText("Name: " + user.getName());
                            tvEmail.setText("Email: " + user.getEmail());
                            // Load profile picture here using Glide/Picasso
                        }else{
                            Toast.makeText(ProfileActivity.this, "User data error.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ProfileActivity.this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadProfilePic(String url) {
        if (url != null && !url.isEmpty()) {
            Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.ic_default_profile) // Add a default drawable here
                    .error(R.drawable.ic_default_profile)       // Add a default drawable here
                    .into(ivProfilePic);
        } else {
            // Set default image if URL is empty
            ivProfilePic.setImageResource(R.drawable.ic_default_profile);
        }
    }


    private void logoutUser() {
        auth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Navigate back to LoginActivity and clear the stack
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
