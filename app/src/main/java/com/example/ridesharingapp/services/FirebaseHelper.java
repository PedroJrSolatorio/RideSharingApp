package com.example.ridesharingapp.services;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.example.ridesharingapp.models.User;
import com.example.ridesharingapp.models.Ride;
import com.example.ridesharingapp.models.Driver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseHelper {
    private DatabaseReference realtimeDb;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    public FirebaseHelper() {
        // Realtime Database for live driver locations
        realtimeDb = FirebaseDatabase.getInstance(
                "https://ridesharingapp-ee55d-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference();

        // Firestore for all persistent data
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    // ========== USER METHODS (FIRESTORE) ==========

    public void createUser(User user, OnCompleteListener<Void> listener) {
        firestore.collection("users")
                .document(user.getUserId())
                .set(user)
                .addOnCompleteListener(listener);
    }

    public void updateUser(User user, OnCompleteListener<Void> listener) {
        firestore.collection("users")
                .document(user.getUserId())
                .set(user)
                .addOnCompleteListener(listener);
    }

    public void getUserById(String userId, OnCompleteListener<com.google.firebase.firestore.DocumentSnapshot> listener) {
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(listener);
    }

    // ========== DRIVER LOCATION METHODS (REALTIME DATABASE) ==========

    // Update driver's live location
    public void updateDriverLocation(String driverId, double latitude, double longitude) {
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("lat", latitude);
        locationData.put("lng", longitude);
        locationData.put("timestamp", System.currentTimeMillis());

        realtimeDb.child("activeDrivers").child(driverId).updateChildren(locationData);
    }

    // Set driver availability (affects both databases)
    public void setDriverAvailability(String driverId, boolean isAvailable) {
        // Update in Realtime DB for quick access
        realtimeDb.child("activeDrivers").child(driverId).child("status")
                .setValue(isAvailable ? "available" : "offline");

        // Update in Firestore for persistent record
        firestore.collection("users").document(driverId)
                .update("isAvailable", isAvailable);
    }

    // Listen to driver location changes
    public void listenToDriverLocation(String driverId, ValueEventListener listener) {
        realtimeDb.child("activeDrivers").child(driverId)
                .addValueEventListener(listener);
    }

    // Remove driver from active list (when going offline)
    public void removeDriverFromActive(String driverId) {
        realtimeDb.child("activeDrivers").child(driverId).removeValue();
    }

    // ========== RIDE METHODS (FIRESTORE) ==========

    public void requestRide(Ride ride, OnCompleteListener<Void> listener) {
        String rideId = firestore.collection("rides").document().getId();
        ride.setRideId(rideId);

        firestore.collection("rides")
                .document(rideId)
                .set(ride)
                .addOnCompleteListener(listener);
    }

    public void updateRideStatus(String rideId, String status) {
        firestore.collection("rides")
                .document(rideId)
                .update("status", status);
    }

    // Listen to ride updates using Firestore snapshot listener
    public void listenToRideUpdates(String rideId,
                                    com.google.firebase.firestore.EventListener<com.google.firebase.firestore.DocumentSnapshot> listener) {
        firestore.collection("rides")
                .document(rideId)
                .addSnapshotListener(listener);
    }

    // Get ride history for a user
    public void getUserRideHistory(String userId, OnCompleteListener<QuerySnapshot> listener) {
        firestore.collection("rides")
                .whereEqualTo("riderId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnCompleteListener(listener);
    }

    // ========== FIND NEARBY DRIVERS (HYBRID APPROACH) ==========

    public void findNearbyDrivers(double latitude, double longitude, double radiusKm,
                                  final NearbyDriversCallback callback) {
        // Step 1: Get active drivers from Realtime Database
        realtimeDb.child("activeDrivers")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<String> nearbyDriverIds = new ArrayList<>();

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Double lat = ds.child("lat").getValue(Double.class);
                            Double lng = ds.child("lng").getValue(Double.class);
                            String status = ds.child("status").getValue(String.class);

                            if (lat != null && lng != null && "available".equals(status)) {
                                double distance = distanceBetween(latitude, longitude, lat, lng);
                                if (distance <= radiusKm) {
                                    nearbyDriverIds.add(ds.getKey());
                                }
                            }
                        }

                        // Step 2: Get full driver details from Firestore
                        if (!nearbyDriverIds.isEmpty()) {
                            fetchDriverDetails(nearbyDriverIds, callback);
                        } else {
                            callback.onDriversFound(new ArrayList<>());
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    private void fetchDriverDetails(List<String> driverIds, final NearbyDriversCallback callback) {
        firestore.collection("users")
                .whereIn("userId", driverIds)
                .whereEqualTo("userType", "driver")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Driver> drivers = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        Driver driver = doc.toObject(Driver.class);
                        if (driver != null) {
                            drivers.add(driver);
                        }
                    }
                    callback.onDriversFound(drivers);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ========== HELPER METHODS ==========

    private double distanceBetween(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Callback interface for nearby drivers
    public interface NearbyDriversCallback {
        void onDriversFound(List<Driver> drivers);
        void onError(String error);
    }
}
