package com.example.ridesharingapp.activities;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.ridesharingapp.R;
import com.example.ridesharingapp.utils.AppHelper;
import com.example.ridesharingapp.utils.CloudinaryHelper;
import com.example.ridesharingapp.utils.DataPart;
import com.example.ridesharingapp.utils.VolleyMultipartRequest;
import com.example.ridesharingapp.utils.VolleySingleton;
import com.bumptech.glide.Glide;
import de.hdodenhof.circleimageview.CircleImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.widget.TextView;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    // Render public url + actual API endpoint

    // URL to get signature from your Node.js server
    private static final String SIGNATURE_URL = "https://ridesharingbackend-hi94.onrender.com/api/upload/signature";
    // Base URL for direct upload
    private static final String CLOUDINARY_UPLOAD_URL = "https://api.cloudinary.com/v1_1/";

    private FirebaseFirestore firestore;
    private Button mChangePicButton;
    private Uri mImageUri;
    private ProgressDialog mProgressDialog;
    private TextView mTvName;
    private TextView mTvEmail;
    private CircleImageView mProfilePic;

    // Use ActivityResultLauncher for image selection
    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    mImageUri = uri;
                    // Optional: Show the selected local file immediately
                    Glide.with(this).load(mImageUri).into(mProfilePic);

                    mChangePicButton.setText("Upload Photo"); // Update button text
                    Toast.makeText(this, "Image selected. Click 'Upload Photo'.", Toast.LENGTH_LONG).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Correctly referencing the XML layout
        setContentView(R.layout.activity_profile);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize all views
        mTvName = findViewById(R.id.tvName);
        mTvEmail = findViewById(R.id.tvEmail);
        mProfilePic = findViewById(R.id.ivProfilePic);
        mChangePicButton = findViewById(R.id.btnChangePic);

        mProfilePic = findViewById(R.id.ivProfilePic);

        mChangePicButton.setOnClickListener(v -> {
            if (mImageUri == null) {
                // If no image is selected, open file chooser
                openFileChooser();
            } else {
                // If an image is selected, initiate the upload
                uploadProfileData();
            }
        });

        // NEW: Call a method to load user data on startup
        loadUserData();
    }

    private void loadUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Load from Firestore
            firestore.collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String email = documentSnapshot.getString("email");
                            String name = documentSnapshot.getString("name");
                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                            mTvEmail.setText("Email: " + (email != null ? email : "N/A"));
                            mTvName.setText("Name: " + (name != null && !name.isEmpty() ? name : "Please set your name"));

                            // Load profile picture if available
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                Glide.with(this).load(profileImageUrl).into(mProfilePic);
                            }
                        } else {
                            mTvName.setText("Name: User data not found");
                            mTvEmail.setText("Email: User data not found");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Handle case where user is not logged in (e.g., redirect to login screen)
            mTvName.setText("Name: (Not Logged In)");
            mTvEmail.setText("Email: (Not Logged In)");
        }
    }

    private void openFileChooser() {
        mGetContent.launch("image/*");
    }

    private void uploadProfileData() {
        if (mImageUri == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Connecting to server...");
        mProgressDialog.show();

        CloudinaryHelper.getUploadSignature(this, new CloudinaryHelper.SignatureCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    String signature = response.getString("signature");
                    String timestamp = response.getString("timestamp");
                    String cloudName = response.getString("cloudName");
                    String apiKey = response.getString("apiKey");
                    String folder = response.getString("folder");

                    mProgressDialog.setMessage("Uploading photo...");
                    uploadToCloudinary(signature, timestamp, cloudName, apiKey, folder);

                } catch (JSONException e) {
                    mProgressDialog.dismiss();
                    Toast.makeText(ProfileActivity.this,
                            "Error parsing response: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(String error) {
                mProgressDialog.dismiss();
                Toast.makeText(ProfileActivity.this,
                        "Failed to connect: " + error,
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onColdStart() {
                mProgressDialog.setMessage("Waking up server, please wait...");
            }
        });
    }

    // New method for the second request (Direct Upload)
    private void uploadToCloudinary(String signature, String timestamp, String cloudName, String apiKey, String folder) {
        // Build the final Cloudinary upload URL
        final String finalCloudinaryUrl = CLOUDINARY_UPLOAD_URL + cloudName + "/image/upload";

        VolleyMultipartRequest cloudinaryRequest = new VolleyMultipartRequest(
                Request.Method.POST,
                finalCloudinaryUrl,
                new Response.Listener<NetworkResponse>() {
                    @Override
                    public void onResponse(NetworkResponse response) {
                        mProgressDialog.dismiss();
                        try {
                            String result = new String(response.data);
                            JSONObject jsonObject = new JSONObject(result);
                            String secureUrl = jsonObject.getString("secure_url");

                            // Save URL to Firestore
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                firestore.collection("users")
                                        .document(user.getUid())
                                        .update("profileImageUrl", secureUrl)
                                        .addOnSuccessListener(aVoid -> {
                                            mImageUri = null;
                                            mChangePicButton.setText("Change Photo");
                                            displayNewProfilePicture(secureUrl);
                                            Toast.makeText(ProfileActivity.this,
                                                    "Profile picture updated!",
                                                    Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(ProfileActivity.this,
                                                    "Failed to save URL: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        });
                            }

                            Log.d(TAG, "Cloudinary Response: " + jsonObject.toString());

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(ProfileActivity.this, "Upload Success, but Cloudinary JSON response error.", Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        mProgressDialog.dismiss();
                        String errorMsg = (error.networkResponse != null && error.networkResponse.data != null)
                                ? new String(error.networkResponse.data) : error.getMessage();
                        Toast.makeText(ProfileActivity.this, "Cloudinary Upload Failed: " + errorMsg, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Cloudinary Volley Error: " + errorMsg);
                    }
                }) {

            // Text parameters (Cloudinary required fields)
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("api_key", apiKey);
                params.put("timestamp", timestamp);
                params.put("signature", signature);
                params.put("folder", folder);
                // You can add more parameters here like public_id
                // params.put("public_id", "user_456_profile");
                return params;
            }

            // File parameters (The actual image)
            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                try {
                    byte[] imageBytes = AppHelper.getBytesFromUri(ProfileActivity.this, mImageUri);
                    // The field name for the image file MUST be 'file' for Cloudinary
                    params.put("file", new DataPart(
                            "profile_image.jpg",
                            imageBytes,
                            "image/jpeg"));
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(ProfileActivity.this, "Error converting image for upload.", Toast.LENGTH_LONG).show();
                }
                return params;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(cloudinaryRequest);
    }

    // function to handle displaying the uploaded image
    private void displayNewProfilePicture(String secureUrl) {
        // Use Glide to load the image from the Cloudinary URL into the CircleImageView
        Glide.with(this)
                .load(secureUrl)
                // Optional: Add a placeholder or error image
                // .placeholder(R.drawable.default_placeholder)
                .into(mProfilePic);
    }
}
