package com.example.ridesharingapp.services;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.ridesharingapp.models.Driver;

public class DriverService {
    private DatabaseReference database;

    public DriverService() {
        database = FirebaseDatabase.getInstance("https://ridesharingapp-ee55d-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference().child("drivers");
    }

    // Update Driver Location
    public void updateLocation(String driverId, double latitude, double longitude) {
        database.child(driverId).child("currentLatitude").setValue(latitude);
        database.child(driverId).child("currentLongitude").setValue(longitude);
    }

    // Update Driver Availability
    public void setAvailability(String driverId, boolean isAvailable) {
        database.child(driverId).child("isAvailable").setValue(isAvailable);
    }

    // Update Vehicle Info
    public void updateVehicleInfo(String driverId, String model, String plate, String license, String type) {
        database.child(driverId).child("vehicleModel").setValue(model);
        database.child(driverId).child("licensePlate").setValue(plate);
        database.child(driverId).child("driverLicense").setValue(license);
        database.child(driverId).child("vehicleType").setValue(type);
    }

    // Update Full Driver Object
    public void updateDriver(Driver driver) {
        database.child(driver.getUserId()).setValue(driver);
    }
}
