package com.example.ridesharingapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.ridesharingapp.R;
import com.example.ridesharingapp.services.DriverService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.gestures.MoveGestureDetector;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.maps.plugin.gestures.GesturesPlugin;
import com.mapbox.maps.plugin.gestures.OnMoveListener;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils;

import android.content.IntentSender;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.*;
import com.google.android.gms.tasks.Task;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_LOCATION = 1001;

    private MapView mapView;
    private MapboxMap mapboxMap;
    private LocationEngine locationEngine;
    private LocationChangeCallback locationCallback;
    private DriverService driverService;
    private String driverId = "driver123"; // Replace with actual logged-in driverId
    private static final int REQUEST_CHECK_SETTINGS = 2001;

    private boolean isFollowingUser = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        driverService = new DriverService();
        locationCallback = new LocationChangeCallback(this);

        // Load the map style
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
            this.mapboxMap = mapView.getMapboxMap();
            enableLocationComponent();

            // ✅ Gesture listener: disable auto-follow when user drags map
            GesturesPlugin gesturesPlugin = (GesturesPlugin) mapView.getPlugin(Plugin.MAPBOX_GESTURES_PLUGIN_ID);
            if (gesturesPlugin != null) {
                gesturesPlugin.addOnMoveListener(new OnMoveListener() {
                    @Override
                    public void onMoveBegin(@NonNull MoveGestureDetector detector) {
                        // Stop following the user when they start moving the map
                        isFollowingUser = false;
                    }

                    @Override
                    public boolean onMove(@NonNull MoveGestureDetector detector) {
                        // Return false to let Mapbox continue handling the gesture
                        return false;
                    }

                    @Override
                    public void onMoveEnd(@NonNull MoveGestureDetector detector) {
                        // You can handle map move end here if needed
                    }
                });
            }
        });

        // ---------- RE-CENTER BUTTON ----------
        FloatingActionButton btnRecenter = findViewById(R.id.btnRecenter);
        btnRecenter.setOnClickListener(v -> {
            isFollowingUser = true;
            if (mapboxMap != null && locationEngine != null) {
                locationEngine.getLastLocation(new LocationChangeCallback(this));
            }
        });

        // ---------- TOP APP BAR ----------
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setNavigationOnClickListener(v -> {
            // handle menu icon or app title tap
            Toast.makeText(this, "App name clicked", Toast.LENGTH_SHORT).show();
        });
        topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_profile) {
                // Open profile activity
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        // ---------- BOTTOM NAV ----------
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                // Already in MainActivity
                return true;
            } else if (id == R.id.nav_map) {
                // Example: reload the map
                Toast.makeText(this, "Map selected", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_rides) {
                // Example: navigate to ProfileActivity
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }

            return false;
        });

        checkLocationPermission();
    }

    // -------------------- LOCATION PERMISSIONS --------------------
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_LOCATION);
        } else {
            checkLocationEnabled();
        }
    }

    private void checkLocationEnabled() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true); // This makes the dialog appear

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> {
            // GPS is ON — safe to continue
            setupLocationEngine();
        });

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    // Show the system dialog to enable location
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    sendEx.printStackTrace();
                }
            } else {
                Toast.makeText(this, "Please enable location manually in settings", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationEnabled();
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // -------------------- ENABLE LOCATION COMPONENT --------------------
    private void enableLocationComponent() {
        LocationComponentPlugin locationComponent = LocationComponentUtils.getLocationComponent(mapView);
        locationComponent.setEnabled(true);
        // Default location puck will be used automatically
    }

    // -------------------- SETUP LOCATION ENGINE --------------------
    private void setupLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);

        LocationEngineRequest request = new LocationEngineRequest.Builder(1000)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(5000)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationEngine.requestLocationUpdates(request, locationCallback, getMainLooper());
            locationEngine.getLastLocation(locationCallback);
        }
    }

    // -------------------- UPDATE DRIVER LOCATION --------------------
    private void updateDriverLocation(double latitude, double longitude) {
        driverService.updateLocation(driverId, latitude, longitude);
    }

    // -------------------- LIFECYCLE METHODS --------------------
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                // User turned on GPS
                setupLocationEngine();
            } else {
                Toast.makeText(this, "Location is required to use this app", Toast.LENGTH_LONG).show();
            }
        }
    }

    // -------------------- LOCATION CALLBACK --------------------
    private static class LocationChangeCallback implements LocationEngineCallback<LocationEngineResult> {
        private final WeakReference<MainActivity> activityRef;

        LocationChangeCallback(MainActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        public void onSuccess(LocationEngineResult result) {
            MainActivity activity = activityRef.get();
            if (activity != null && result.getLastLocation() != null) {
                Location location = result.getLastLocation();

                // Update driver location
                activity.updateDriverLocation(location.getLatitude(), location.getLongitude());

                if (activity.isFollowingUser && activity.mapboxMap != null) {
                    CameraOptions cameraOptions = new CameraOptions.Builder()
                            .center(Point.fromLngLat(location.getLongitude(), location.getLatitude()))
                            .zoom(14.0)
                            .build();
                    activity.mapboxMap.setCamera(cameraOptions);
                }
            }
        }

        @Override
        public void onFailure(@NonNull Exception exception) {
            // Handle location failure
            exception.printStackTrace();
        }
    }
}
