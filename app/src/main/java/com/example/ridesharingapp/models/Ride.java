package com.example.ridesharingapp.models;

public class Ride {
    private String rideId;
    private String riderId;
    private String driverId;
    private String pickupLocation;
    private String destination;
    private double pickupLatitude;
    private double pickupLongitude;
    private double destinationLatitude;
    private double destinationLongitude;
    private String status; // "requested", "accepted", "ongoing", "completed", "cancelled"
    private double fare;
    private long timestamp;
    private String paymentMethod;

    // Default constructor required for Firebase
    public Ride() {
    }

    // Full constructor
    public Ride(String rideId, String riderId, String driverId,
                String pickupLocation, String destination,
                double pickupLatitude, double pickupLongitude,
                double destinationLatitude, double destinationLongitude,
                String status, double fare, long timestamp, String paymentMethod) {
        this.rideId = rideId;
        this.riderId = riderId;
        this.driverId = driverId;
        this.pickupLocation = pickupLocation;
        this.destination = destination;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.status = status;
        this.fare = fare;
        this.timestamp = timestamp;
        this.paymentMethod = paymentMethod;
    }

    // Getters and Setters
    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public String getRiderId() {
        return riderId;
    }

    public void setRiderId(String riderId) {
        this.riderId = riderId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public double getPickupLatitude() {
        return pickupLatitude;
    }

    public void setPickupLatitude(double pickupLatitude) {
        this.pickupLatitude = pickupLatitude;
    }

    public double getPickupLongitude() {
        return pickupLongitude;
    }

    public void setPickupLongitude(double pickupLongitude) {
        this.pickupLongitude = pickupLongitude;
    }

    public double getDestinationLatitude() {
        return destinationLatitude;
    }

    public void setDestinationLatitude(double destinationLatitude) {
        this.destinationLatitude = destinationLatitude;
    }

    public double getDestinationLongitude() {
        return destinationLongitude;
    }

    public void setDestinationLongitude(double destinationLongitude) {
        this.destinationLongitude = destinationLongitude;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getFare() {
        return fare;
    }

    public void setFare(double fare) {
        this.fare = fare;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
