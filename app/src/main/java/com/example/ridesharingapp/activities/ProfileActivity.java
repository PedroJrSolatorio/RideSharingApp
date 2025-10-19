package com.example.ridesharingapp.activities;

import android.app.ProgressDialog;
import android.content.Intent;
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
import com.example.ridesharingapp.utils.DataPart;
import com.example.ridesharingapp.utils.VolleyMultipartRequest;
import com.example.ridesharingapp.utils.VolleySingleton;

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

    private Button mChangePicButton;
    private Uri mImageUri;
    private ProgressDialog mProgressDialog;

    // Use ActivityResultLauncher for image selection
    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    mImageUri = uri;
                    Toast.makeText(this, "Image selected. Ready to upload.", Toast.LENGTH_SHORT).show();
                    // Optional: Load image into CircleImageView here
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Correctly referencing the XML layout
        setContentView(R.layout.activity_profile);

        // Correctly referencing the Button ID from the XML
        mChangePicButton = findViewById(R.id.btnChangePic);

        mChangePicButton.setOnClickListener(v -> {
            if (mImageUri == null) {
                // If no image is selected, open file chooser
                openFileChooser();
            } else {
                // If an image is selected, initiate the upload
                uploadProfileData();
            }
        });

        // Example data display (optional)
        // findViewById(R.id.tvName).setText("Name: Jane Doe");
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
        mProgressDialog.setMessage("Requesting upload signature...");
        mProgressDialog.show();

        // STEP 1: Request the signed signature from your Node.js server
        JsonObjectRequest signatureRequest = new JsonObjectRequest(
                Request.Method.GET,
                SIGNATURE_URL,
                null, // No body needed for a GET request
                response -> {
                    try {
                        // Parse the response from your server.js
                        String signature = response.getString("signature");
                        String timestamp = response.getString("timestamp");
                        String cloudName = response.getString("cloudName"); // Use the cloud name to build the final URL
                        String apiKey = response.getString("apiKey");
                        String folder = response.getString("folder");

                        // Now proceed to the direct upload using the signature
                        mProgressDialog.setMessage("Uploading photo directly to Cloudinary...");
                        uploadToCloudinary(signature, timestamp, cloudName, apiKey, folder);

                    } catch (JSONException e) {
                        mProgressDialog.dismiss();
                        Toast.makeText(ProfileActivity.this, "Error parsing signature response: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Signature JSON Error: ", e);
                    }
                },
                error -> {
                    mProgressDialog.dismiss();
                    Toast.makeText(ProfileActivity.this, "Failed to get upload signature.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Signature Request Volley Error: " + error.toString());
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(signatureRequest);
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

                            // Display success message and the returned URL
                            Toast.makeText(ProfileActivity.this, "Upload Success! URL: " + secureUrl, Toast.LENGTH_LONG).show();
                            // HERE you can load the secureUrl into your ivProfilePic
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
}
