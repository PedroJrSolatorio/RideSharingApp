package com.example.ridesharingapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.ridesharingapp.BuildConfig;
import com.example.ridesharingapp.R;
import com.example.ridesharingapp.models.Driver;
import com.example.ridesharingapp.services.FirebaseHelper;
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
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationPluginImplKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManagerKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.gestures.GesturesPlugin;
import com.mapbox.maps.plugin.gestures.OnMoveListener;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import android.location.Location;
import java.lang.ref.WeakReference;

import java.util.ArrayList;
import java.util.List;

public class RiderMainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_LOCATION = 1002;

    private MapView mapView;
    private MapboxMap mapboxMap;
    private LocationEngine locationEngine;
    private LocationChangeCallback locationCallback;
    private PointAnnotationManager pointAnnotationManager;
    private FirebaseHelper firebaseHelper;
    private boolean isFollowingUser = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_rider_main);

//        // DEBUG: Check if token is being read
//        String token = BuildConfig.MAPBOX_ACCESS_TOKEN;
//        Log.d("MapboxToken", "Token: " + token);
//        Log.d("MapboxToken", "Token length: " + token.length());
//        Log.d("MapboxToken", "Starts with pk: " + token.startsWith("pk."));

        mapView = findViewById(R.id.mapViewRider);
        firebaseHelper = new FirebaseHelper();

        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
            this.mapboxMap = mapView.getMapboxMap();

            // Convert vector drawable to bitmap
            Bitmap driverIconBitmap = getBitmapFromVectorDrawable(R.drawable.driver_icon);
            if (driverIconBitmap != null) {
                style.addImage("driver_icon", driverIconBitmap);
            } else {
                Log.e("RiderMainActivity", "Failed to load driver_icon");
            }

            enableLocationComponent();
            setupDriverMarkers();

            // Detect when user moves the map
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
                        return false;
                    }

                    @Override
                    public void onMoveEnd(@NonNull MoveGestureDetector detector) {
                        // Optional: handle map move end
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

    // -------------------- LOCATION PERMISSION --------------------
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_LOCATION);
        } else {
            setupLocationEngine();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocationComponent();
                setupLocationEngine();
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // -------------------- ENABLE RIDER LOCATION --------------------
    private void enableLocationComponent() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationComponentPlugin locationComponent = LocationComponentUtils.getLocationComponent(mapView);
            locationComponent.setEnabled(true);
        }
    }

    // -------------------- SETUP DRIVER MARKERS --------------------
    private void setupDriverMarkers() {
        if (mapboxMap == null) return;

        // Get annotations plugin first, then create PointAnnotationManager
        AnnotationPlugin annotationApi = AnnotationPluginImplKt.getAnnotations(mapView);
        pointAnnotationManager = PointAnnotationManagerKt.createPointAnnotationManager(
                annotationApi,
                mapView
        );
    }

    // -------------------- FETCH NEARBY DRIVERS --------------------
    private void fetchNearbyDrivers(double riderLat, double riderLng) {

        double radiusKm = 5; // Example: 5km radius

        firebaseHelper.findNearbyDrivers(riderLat, riderLng, radiusKm,
                new FirebaseHelper.NearbyDriversCallback() {
                    @Override
                    public void onDriversFound(List<Driver> drivers) {
                        runOnUiThread(() -> {
                            // Clear old markers
                            if (pointAnnotationManager != null) {
                                pointAnnotationManager.deleteAll();
                            }

                            if (drivers.isEmpty()) {
                                Toast.makeText(RiderMainActivity.this,
                                        "No drivers nearby",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Add driver markers
                            List<PointAnnotationOptions> annotationOptions = new ArrayList<>();
                            for (Driver driver : drivers) {
                                Point driverPoint = Point.fromLngLat(
                                        driver.getCurrentLongitude(),
                                        driver.getCurrentLatitude()
                                );
                                PointAnnotationOptions options = new PointAnnotationOptions()
                                        .withPoint(driverPoint)
                                        .withIconImage("driver_icon");
                                annotationOptions.add(options);
                            }

                            if (pointAnnotationManager != null && !annotationOptions.isEmpty()) {
                                pointAnnotationManager.create(annotationOptions);
                                Log.d("RiderMainActivity", "Added " + drivers.size() + " driver markers");
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(RiderMainActivity.this,
                                    "Error loading drivers: " + error,
                                    Toast.LENGTH_SHORT).show();
                            Log.e("RiderMainActivity", "Error fetching drivers: " + error);
                        });
                    }
                });
    }

    // -------------------- SETUP LOCATION ENGINE --------------------
    private void setupLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);
        locationCallback = new LocationChangeCallback(this);

        LocationEngineRequest request = new LocationEngineRequest.Builder(5000)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(10000)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationEngine.requestLocationUpdates(request, locationCallback, getMainLooper());
            locationEngine.getLastLocation(locationCallback);
        }
    }

    // -------------------- HELPER METHOD --------------------
    private Bitmap getBitmapFromVectorDrawable(int drawableId) {
        android.graphics.drawable.Drawable drawable = ContextCompat.getDrawable(this, drawableId);
        if (drawable == null) {
            return null;
        }

        if (drawable instanceof android.graphics.drawable.BitmapDrawable) {
            return ((android.graphics.drawable.BitmapDrawable) drawable).getBitmap();
        }

        // For vector drawables
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    // -------------------- LIFECYCLE --------------------
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
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
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    // -------------------- LOCATION CALLBACK --------------------
    private static class LocationChangeCallback implements LocationEngineCallback<LocationEngineResult> {
        private final WeakReference<RiderMainActivity> activityRef;

        LocationChangeCallback(RiderMainActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        public void onSuccess(LocationEngineResult result) {
            RiderMainActivity activity = activityRef.get();
            if (activity != null) {
                Location location = result.getLastLocation();
                if (location != null) {
                    // Only move camera if user hasn't manually moved the map
                    if (activity.isFollowingUser && activity.mapboxMap != null) {
                        CameraOptions cameraOptions = new CameraOptions.Builder()
                                .center(Point.fromLngLat(location.getLongitude(), location.getLatitude()))
                                .zoom(15.0)
                                .build();
                        activity.mapboxMap.setCamera(cameraOptions);
                    }
                    // Update the nearby drivers search with actual rider location
                    activity.fetchNearbyDrivers(location.getLatitude(), location.getLongitude());
                }
            }
        }

        @Override
        public void onFailure(@NonNull Exception exception) {
            exception.printStackTrace();
        }
    }
}
