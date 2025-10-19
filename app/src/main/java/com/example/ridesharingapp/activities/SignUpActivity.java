package com.example.ridesharingapp.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.example.ridesharingapp.R;
import com.example.ridesharingapp.models.User;
import com.example.ridesharingapp.models.Driver;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.ridesharingapp.utils.VolleyMultipartRequest;
import com.example.ridesharingapp.utils.VolleySingleton;
import com.example.ridesharingapp.utils.DataPart;
import com.example.ridesharingapp.utils.AppHelper;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etConfirmPassword, etName, etPhone, etBirthdate;
    private RadioGroup rgUserType, rgGender;
    private Button btnSignUp, btnBackToLogin;
    private ProgressBar progressBar;
    private LinearLayout llRiderDocs, llDriverDocs;

    // Rider document buttons
    private Button btnUploadValidId;
    private TextView tvValidIdStatus;

    // Driver document buttons
    private Button btnUploadLicense, btnUploadVehiclePic, btnUploadOrCr, btnUploadCertification;
    private TextView tvLicenseStatus, tvVehiclePicStatus, tvOrCrStatus, tvCertificationStatus;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    // Document URIs
    private Uri validIdUri;
    private Uri licenseUri, vehiclePicUri, orCrUri, certificationUri;

    // Current document type being uploaded
    private String currentUploadType;

    // Image picker launcher
    private ActivityResultLauncher<String> imagePickerLauncher;

    private static final String SIGNATURE_URL = "https://ridesharingbackend-hi94.onrender.com/api/upload/signature";
    private static final String CLOUDINARY_UPLOAD_URL = "https://api.cloudinary.com/v1_1/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initializeViews();
        initializeFirebase();
        setupImagePicker();
        setupListeners();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etBirthdate = findViewById(R.id.etBirthdate);
        rgUserType = findViewById(R.id.rgUserType);
        rgGender = findViewById(R.id.rgGender);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);
        progressBar = findViewById(R.id.progressBar);

        // Document sections
        llRiderDocs = findViewById(R.id.llRiderDocs);
        llDriverDocs = findViewById(R.id.llDriverDocs);

        // Rider documents
        btnUploadValidId = findViewById(R.id.btnUploadValidId);
        tvValidIdStatus = findViewById(R.id.tvValidIdStatus);

        // Driver documents
        btnUploadLicense = findViewById(R.id.btnUploadLicense);
        tvLicenseStatus = findViewById(R.id.tvLicenseStatus);
        btnUploadVehiclePic = findViewById(R.id.btnUploadVehiclePic);
        tvVehiclePicStatus = findViewById(R.id.tvVehiclePicStatus);
        btnUploadOrCr = findViewById(R.id.btnUploadOrCr);
        tvOrCrStatus = findViewById(R.id.tvOrCrStatus);
        btnUploadCertification = findViewById(R.id.btnUploadCertification);
        tvCertificationStatus = findViewById(R.id.tvCertificationStatus);
    }

    private void initializeFirebase() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleImageSelected(uri);
                    }
                });
    }

    private void setupListeners() {
        setupDatePicker();

        // User type selection - show/hide appropriate document sections
        rgUserType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbRider) {
                llRiderDocs.setVisibility(View.VISIBLE);
                llDriverDocs.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbDriver) {
                llRiderDocs.setVisibility(View.GONE);
                llDriverDocs.setVisibility(View.VISIBLE);
            }
        });

        // Pre-select Rider
        ((RadioButton)findViewById(R.id.rbRider)).setChecked(true);
        llRiderDocs.setVisibility(View.VISIBLE);

        // Rider document upload
        btnUploadValidId.setOnClickListener(v -> {
            currentUploadType = "validId";
            imagePickerLauncher.launch("image/*");
        });

        // Driver document uploads
        btnUploadLicense.setOnClickListener(v -> {
            currentUploadType = "license";
            imagePickerLauncher.launch("image/*");
        });

        btnUploadVehiclePic.setOnClickListener(v -> {
            currentUploadType = "vehiclePic";
            imagePickerLauncher.launch("image/*");
        });

        btnUploadOrCr.setOnClickListener(v -> {
            currentUploadType = "orCr";
            imagePickerLauncher.launch("image/*");
        });

        btnUploadCertification.setOnClickListener(v -> {
            currentUploadType = "certification";
            imagePickerLauncher.launch("image/*");
        });

        btnSignUp.setOnClickListener(v -> signUpUser());

        btnBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void handleImageSelected(Uri uri) {
        switch (currentUploadType) {
            case "validId":
                validIdUri = uri;
                tvValidIdStatus.setText("✓ Selected");
                tvValidIdStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                break;
            case "license":
                licenseUri = uri;
                tvLicenseStatus.setText("✓ Selected");
                tvLicenseStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                break;
            case "vehiclePic":
                vehiclePicUri = uri;
                tvVehiclePicStatus.setText("✓ Selected");
                tvVehiclePicStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                break;
            case "orCr":
                orCrUri = uri;
                tvOrCrStatus.setText("✓ Selected");
                tvOrCrStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                break;
            case "certification":
                certificationUri = uri;
                tvCertificationStatus.setText("✓ Selected");
                tvCertificationStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                break;
        }
    }

    private void setupDatePicker() {
        etBirthdate.setOnClickListener(v -> {
            // Get current date
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            // Create DatePickerDialog
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    SignUpActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        // Format: DD/MM/YYYY
                        String formattedDate = String.format("%02d/%02d/%04d",
                                selectedDay, selectedMonth + 1, selectedYear);
                        etBirthdate.setText(formattedDate);
                    },
                    year, month, day
            );

            // Set max date to today (can't select future dates)
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

            // Optional: Set min date (e.g., must be at least 18 years old)
            Calendar minDate = Calendar.getInstance();
            minDate.set(Calendar.YEAR, year - 100); // Max 100 years old
            datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());

            datePickerDialog.show();
        });
    }

    private void signUpUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String birthdate = etBirthdate.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(name)) {
            etName.setError("Name required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email required");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Phone number required");
            return;
        }
        if (TextUtils.isEmpty(birthdate)) {
            etBirthdate.setError("Birthdate required");
            Toast.makeText(this, "Please select your birthdate", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isAtLeast18YearsOld(birthdate)) {
            etBirthdate.setError("Must be 18 or older");
            Toast.makeText(this, "You must be at least 18 years old to register", Toast.LENGTH_LONG).show();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password required");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        // Gender validation
        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        if (selectedGenderId == -1) {
            Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton selectedGenderButton = findViewById(selectedGenderId);
        String gender = selectedGenderButton.getText().toString();

        // User type validation
        int selectedId = rgUserType.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Please select user type", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton selectedRadioButton = findViewById(selectedId);
        String userType = selectedRadioButton.getText().toString().toLowerCase();

        // Document validation
        if ("rider".equals(userType)) {
            if (validIdUri == null) {
                Toast.makeText(this, "Please upload your Valid ID", Toast.LENGTH_SHORT).show();
                return;
            }
        } else if ("driver".equals(userType)) {
            if (licenseUri == null || vehiclePicUri == null || orCrUri == null || certificationUri == null) {
                Toast.makeText(this, "Please upload all required driver documents", Toast.LENGTH_LONG).show();
                return;
            }
        }

        progressBar.setVisibility(ProgressBar.VISIBLE);

        // Create user account
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Save user data to database
                        String uid = auth.getCurrentUser().getUid();

                        // Upload documents first, then create user profile
                        if ("rider".equals(userType)) {
                            uploadRiderDocuments(uid, name, email, phone, birthdate, gender, userType);
                        } else {
                            uploadDriverDocuments(uid, name, email, phone, birthdate, gender, userType);
                        }
                    }   else {
                        progressBar.setVisibility(ProgressBar.GONE);
                        Toast.makeText(SignUpActivity.this,
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadRiderDocuments(String uid, String name, String email, String phone,
                                      String birthdate, String gender, String userType) {
        // Upload valid ID to Cloudinary
        uploadToCloudinary(validIdUri, "validId", new CloudinaryUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                createRiderProfile(uid, name, email, phone, birthdate, gender, userType, imageUrl);
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(ProgressBar.GONE);
                auth.getCurrentUser().delete();
                Toast.makeText(SignUpActivity.this,
                        "Failed to upload Valid ID: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadDriverDocuments(String uid, String name, String email, String phone,
                                       String birthdate, String gender, String userType) {
        final String[] urls = new String[4]; // license, vehiclePic, orCr, certification
        final int[] uploadCount = {0};

        // Upload all 4 documents
        uploadToCloudinary(licenseUri, "license", new CloudinaryUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                urls[0] = imageUrl;
                uploadCount[0]++;
                checkAllUploadsComplete(urls, uploadCount[0], uid, name, email, phone, birthdate, gender, userType);
            }

            @Override
            public void onFailure(String error) {
                handleUploadFailure("Driver's License", error);
            }
        });

        uploadToCloudinary(vehiclePicUri, "vehiclePic", new CloudinaryUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                urls[1] = imageUrl;
                uploadCount[0]++;
                checkAllUploadsComplete(urls, uploadCount[0], uid, name, email, phone, birthdate, gender, userType);
            }

            @Override
            public void onFailure(String error) {
                handleUploadFailure("Vehicle Picture", error);
            }
        });

        uploadToCloudinary(orCrUri, "orCr", new CloudinaryUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                urls[2] = imageUrl;
                uploadCount[0]++;
                checkAllUploadsComplete(urls, uploadCount[0], uid, name, email, phone, birthdate, gender, userType);
            }

            @Override
            public void onFailure(String error) {
                handleUploadFailure("OR/CR", error);
            }
        });

        uploadToCloudinary(certificationUri, "certification", new CloudinaryUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                urls[3] = imageUrl;
                uploadCount[0]++;
                checkAllUploadsComplete(urls, uploadCount[0], uid, name, email, phone, birthdate, gender, userType);
            }

            @Override
            public void onFailure(String error) {
                handleUploadFailure("Certification", error);
            }
        });
    }

    private void checkAllUploadsComplete(String[] urls, int uploadCount, String uid, String name,
                                         String email, String phone, String birthdate, String gender, String userType) {
        if (uploadCount == 4) {
            // All uploads complete
            createDriverProfile(uid, name, email, phone, birthdate, gender, userType,
                    "pending", urls[0], urls[1], urls[2], urls[3]);
        }
    }

    private void handleUploadFailure(String documentName, String error) {
        progressBar.setVisibility(ProgressBar.GONE);
        auth.getCurrentUser().delete();
        Toast.makeText(SignUpActivity.this,
                "Failed to upload " + documentName + ": " + error,
                Toast.LENGTH_SHORT).show();
    }

    // Generic Cloudinary upload method
    private void uploadToCloudinary(Uri fileUri, String documentType, CloudinaryUploadCallback callback) {
        // STEP 1: Get signature from your server
        JsonObjectRequest signatureRequest = new JsonObjectRequest(
                Request.Method.GET,
                SIGNATURE_URL,
                null,
                response -> {
                    try {
                        String signature = response.getString("signature");
                        String timestamp = response.getString("timestamp");
                        String cloudName = response.getString("cloudName");
                        String apiKey = response.getString("apiKey");
                        String folder = response.getString("folder");

                        // STEP 2: Upload to Cloudinary
                        uploadFileToCloudinary(fileUri, documentType, signature, timestamp,
                                cloudName, apiKey, folder, callback);

                    } catch (JSONException e) {
                        callback.onFailure("Error parsing signature response: " + e.getMessage());
                    }
                },
                error -> callback.onFailure("Failed to get upload signature: " + error.toString())
        );

        VolleySingleton.getInstance(this).addToRequestQueue(signatureRequest);
    }

    private void uploadFileToCloudinary(Uri fileUri, String documentType, String signature,
                                        String timestamp, String cloudName, String apiKey,
                                        String folder, CloudinaryUploadCallback callback) {
        final String finalCloudinaryUrl = CLOUDINARY_UPLOAD_URL + cloudName + "/image/upload";

        VolleyMultipartRequest cloudinaryRequest = new VolleyMultipartRequest(
                Request.Method.POST,
                finalCloudinaryUrl,
                response -> {
                    try {
                        String result = new String(response.data);
                        JSONObject jsonObject = new JSONObject(result);
                        String secureUrl = jsonObject.getString("secure_url");
                        callback.onSuccess(secureUrl);
                    } catch (JSONException e) {
                        callback.onFailure("Error parsing Cloudinary response: " + e.getMessage());
                    }
                },
                error -> {
                    String errorMsg = (error.networkResponse != null && error.networkResponse.data != null)
                            ? new String(error.networkResponse.data) : error.getMessage();
                    callback.onFailure(errorMsg);
                }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("api_key", apiKey);
                params.put("timestamp", timestamp);
                params.put("signature", signature);
                params.put("folder", folder);
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                try {
                    byte[] imageBytes = AppHelper.getBytesFromUri(SignUpActivity.this, fileUri);
                    params.put("file", new DataPart(
                            documentType + ".jpg",
                            imageBytes,
                            "image/jpeg"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return params;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(cloudinaryRequest);
    }

    // Callback interface for Cloudinary uploads
    interface CloudinaryUploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String error);
    }

    // Update the create profile methods - remove Firebase Storage references
    private void createRiderProfile(String uid, String name, String email, String phone,
                                    String birthdate, String gender, String userType, String validIdUrl) {
        boolean isVerified = false;
        double defaultRating = 5.0;
        boolean isActive = true;

        User user = new User(uid, name, email, phone, "", userType, birthdate, validIdUrl,
                isVerified, defaultRating, isActive);
        user.setGender(gender);

        firestore.collection("users")
                .document(uid)
                .set(user)
                .addOnCompleteListener(dbTask -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    if (dbTask.isSuccessful()) {
                        Toast.makeText(SignUpActivity.this,
                                "Account created successfully!",
                                Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignUpActivity.this, RiderMainActivity.class));
                        finish();
                    } else {
                        auth.getCurrentUser().delete();
                        Toast.makeText(SignUpActivity.this,
                                "Failed to save user data: " + dbTask.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createDriverProfile(String uid, String name, String email, String phone,
                                     String birthdate, String gender, String userType,
                                     String validIdUrl, String licenseUrl, String vehiclePicUrl,
                                     String orCrUrl, String certificationUrl) {
        boolean isVerified = false;
        double defaultRating = 5.0;
        boolean isActive = true;

        Driver driver = new Driver(
                uid, name, email, phone, "", userType,
                birthdate, validIdUrl, isVerified, defaultRating, isActive,
                "", "", "", false, 0.0, 0.0, "",
                licenseUrl, vehiclePicUrl, orCrUrl, certificationUrl
        );
        driver.setGender(gender);

        firestore.collection("users")
                .document(uid)
                .set(driver)
                .addOnCompleteListener(dbTask -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    if (dbTask.isSuccessful()) {
                        Toast.makeText(SignUpActivity.this,
                                "Account created successfully!",
                                Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                        finish();
                    } else {
                        auth.getCurrentUser().delete();
                        Toast.makeText(SignUpActivity.this,
                                "Failed to save user data: " + dbTask.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isAtLeast18YearsOld(String birthdate) {
        try {
            // Parse the birthdate string (DD/MM/YYYY format)
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date birthDate = sdf.parse(birthdate);

            if (birthDate == null) {
                return false;
            }

            // Calculate 18 years ago from today
            Calendar today = Calendar.getInstance();
            Calendar eighteenYearsAgo = Calendar.getInstance();
            eighteenYearsAgo.add(Calendar.YEAR, -18);

            // Convert birthdate to Calendar
            Calendar birthCal = Calendar.getInstance();
            birthCal.setTime(birthDate);

            // Check if birthdate is before or equal to 18 years ago
            return birthCal.compareTo(eighteenYearsAgo) <= 0;

        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }
}