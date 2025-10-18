package com.example.ridesharingapp.services;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.example.ridesharingapp.models.User;
import com.example.ridesharingapp.models.Ride;
import com.example.ridesharingapp.models.Driver;

import java.util.ArrayList;
import java.util.List;

public class FirebaseHelper {
    private DatabaseReference database;
    private FirebaseAuth auth;

    public FirebaseHelper() {
        database = FirebaseDatabase.getInstance("https://ridesharingapp-ee55d-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();
        auth = FirebaseAuth.getInstance();
    }

    // User Methods
    public void createUser(User user, OnCompleteListener<Void> listener) {
        database.child("users").child(user.getUserId()).setValue(user)
                .addOnCompleteListener(listener);
    }

    public void updateUser(User user, OnCompleteListener<Void> listener){
        database.child("users").child(user.getUserId())
                .setValue(user)
                .addOnCompleteListener(listener);
    }

    // Ride Methods
    public void requestRide(Ride ride, OnCompleteListener<Void> listener) {
        String rideId = database.child("rides").push().getKey();
        ride.setRideId(rideId);
        database.child("rides").child(rideId).setValue(ride)
                .addOnCompleteListener(listener);
    }

    public void updateRideStatus(String rideId, String status) {
        database.child("rides").child(rideId)
                .child("status").setValue(status);
    }

    public void listenRideUpdates(String rideId, ValueEventListener listener) {
        database.child("rides").child(rideId)
                .addValueEventListener(listener);
    }

    public void findNearbyDrivers(double latitude, double longitude, double radiusKm,
                                  ValueEventListener listener) {
        database.child("drivers")
                .orderByChild("isAvailable")
                .equalTo(true)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<Driver> nearbyDrivers = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Driver driver = ds.getValue(Driver.class);
                            if (driver != null) {
                                double distance = distanceBetween(
                                        latitude, longitude,
                                        driver.getCurrentLatitude(),
                                        driver.getCurrentLongitude()
                                );
                                if (distance <= radiusKm) {
                                    nearbyDrivers.add(driver);
                                }
                            }
                        }
                        // Wrap nearby drivers in a fake DataSnapshot-like callback
                        // You can also implement a custom listener for real use
                        listener.onDataChange(snapshot);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        listener.onCancelled(error);
                    }
                });
    }

    // Helper Methods
    private double distanceBetween(double lat1, double lon1, double lat2, double lon2){
        final int R = 6371; //Earth radius in km
        double latDistance = Math.toRadians(lat2-lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // distance in km
    }
}
