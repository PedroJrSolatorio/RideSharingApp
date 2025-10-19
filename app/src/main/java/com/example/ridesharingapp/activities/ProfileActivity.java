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
    private static final String UPLOAD_URL = "https://ridesharingbackend-hi94.onrender.com/api/upload_profile";

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
        mProgressDialog.setMessage("Uploading profile...");
        mProgressDialog.show();

        // **FIXED: Using the 4-argument constructor variant that does not require an explicit 'headers' map.**
        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(
                Request.Method.POST,
                UPLOAD_URL,
                new Response.Listener<NetworkResponse>() {
                    @Override
                    public void onResponse(NetworkResponse response) {
                        mProgressDialog.dismiss();
                        try {
                            String result = new String(response.data);
                            JSONObject jsonObject = new JSONObject(result);
                            Toast.makeText(ProfileActivity.this, "Upload Success: " + jsonObject.getString("message"), Toast.LENGTH_LONG).show();
                            Log.d(TAG, "Response: " + jsonObject.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                            // Handle case where server returns a non-JSON success message
                            Toast.makeText(ProfileActivity.this, "Upload Success, response malformed.", Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        mProgressDialog.dismiss();
                        // Better error message extraction for Volley
                        String errorMsg = (error.networkResponse != null && error.networkResponse.data != null)
                                ? new String(error.networkResponse.data) : error.getMessage();
                        Toast.makeText(ProfileActivity.this, "Upload Failed: " + errorMsg, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Volley Error: " + errorMsg);
                    }
                }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                // Text parameters
                Map<String, String> params = new HashMap<>();
                params.put("user_id", "456"); // Example user ID
                params.put("email", "jane.doe@example.com"); // Example email
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                // File parameters
                Map<String, DataPart> params = new HashMap<>();
                try {
                    // Convert Uri to byte array
                    byte[] imageBytes = AppHelper.getBytesFromUri(ProfileActivity.this, mImageUri);

                    // "profile_pic" is the field name the server expects for the file
                    params.put("profile_pic", new DataPart(
                            "profile_image.jpg", // File name sent to server
                            imageBytes,          // File content as byte array
                            "image/jpeg"));      // MIME type

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(ProfileActivity.this, "Error converting image.", Toast.LENGTH_LONG).show();
                }
                return params;
            }
        };

        // Add the request to the RequestQueue
        VolleySingleton.getInstance(this).addToRequestQueue(multipartRequest);
    }
}
