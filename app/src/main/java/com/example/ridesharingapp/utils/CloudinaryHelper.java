package com.example.ridesharingapp.utils;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

public class CloudinaryHelper {
    private static final String TAG = "CloudinaryHelper";
    private static final String SIGNATURE_URL = "https://ridesharingbackend-hi94.onrender.com/api/upload/signature";

    // Increased timeout for cold starts
    private static final int TIMEOUT_MS = 60000; // 60 seconds
    private static final int MAX_RETRIES = 2;

    public interface SignatureCallback {
        void onSuccess(JSONObject response);
        void onFailure(String error);
        void onColdStart(); // New callback for UI feedback
    }

    public static void getUploadSignature(Context context, SignatureCallback callback) {
        // Notify UI that server might be cold starting
        callback.onColdStart();

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                SIGNATURE_URL,
                null,
                response -> {
                    Log.d(TAG, "Signature obtained successfully");
                    callback.onSuccess(response);
                },
                error -> {
                    Log.e(TAG, "Failed to get signature: " + error.toString());
                    String errorMsg = "Server error";

                    if (error.networkResponse != null) {
                        errorMsg = "Server returned error code: " + error.networkResponse.statusCode;
                    } else if (error.getMessage() != null) {
                        errorMsg = error.getMessage();
                    }

                    callback.onFailure(errorMsg);
                }
        );

        // Set longer timeout for cold starts
        request.setRetryPolicy(new DefaultRetryPolicy(
                TIMEOUT_MS,
                MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        VolleySingleton.getInstance(context).addToRequestQueue(request);
    }

    // Ping server to wake it up
    public static void wakeUpServer(Context context) {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                SIGNATURE_URL.replace("/api/upload/signature", "/"),
                null,
                response -> Log.d(TAG, "Server is awake"),
                error -> Log.d(TAG, "Waking up server...")
        );

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000, // 30 seconds
                0, // Don't retry wake-up calls
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        VolleySingleton.getInstance(context).addToRequestQueue(request);
    }
}
