package com.example.ridesharingapp.models;

public class Driver extends User {
    private String vehicleModel;
    private String licensePlate;
    private String driverLicense;
    private boolean isAvailable;
    private double currentLatitude;
    private double currentLongitude;
    private String vehicleType; // "economy", "premium", "suv"
    private String licenseUrl;
    private String vehiclePicUrl;
    private String orCrUrl;
    private String certificationUrl;

    // Default constructor (required for Firebase)
    public Driver() {
        super();
    }

    // Full constructor - matches SignUpActivity call order
    // Parameters 1-11 are User fields, 12-22 are Driver-specific fields
    public Driver(String userId, String name, String email, String phone,
                  String profileImageUrl, String userType,
                  String birthdate, String validIdUrl, boolean isVerified,
                  double rating, boolean isActive,
                  String vehicleModel, String licensePlate, String driverLicense,
                  boolean isAvailable, double currentLatitude, double currentLongitude,
                  String vehicleType,
                  String licenseUrl, String vehiclePicUrl, String orCrUrl, String certificationUrl) {
        // Call parent User constructor with first 11 parameters
        super(userId, name, email, phone, profileImageUrl, userType, birthdate, validIdUrl, isVerified, rating, isActive);

        // Initialize Driver-specific fields (parameters 12-22)
        this.vehicleModel = vehicleModel;
        this.licensePlate = licensePlate;
        this.driverLicense = driverLicense;
        this.isAvailable = isAvailable;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.vehicleType = vehicleType;
        this.licenseUrl = licenseUrl;
        this.vehiclePicUrl = vehiclePicUrl;
        this.orCrUrl = orCrUrl;
        this.certificationUrl = certificationUrl;
    }

    // Getters and Setters
    public String getVehicleModel() {
        return vehicleModel;
    }
    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public String getLicensePlate() {
        return licensePlate;
    }
    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getDriverLicense() {
        return driverLicense;
    }
    public void setDriverLicense(String driverLicense) {
        this.driverLicense = driverLicense;
    }

    public boolean isAvailable() {
        return isAvailable;
    }
    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public double getCurrentLatitude() {
        return currentLatitude;
    }
    public void setCurrentLatitude(double currentLatitude) {
        this.currentLatitude = currentLatitude;
    }

    public double getCurrentLongitude() {
        return currentLongitude;
    }
    public void setCurrentLongitude(double currentLongitude) {
        this.currentLongitude = currentLongitude;
    }

    public String getVehicleType() {
        return vehicleType;
    }
    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getLicenseUrl(){return licenseUrl;}
    public void setLicenseUrl(String licenseUrl){this.licenseUrl = licenseUrl;}

    public String getVehiclePicUrl(){return vehiclePicUrl;}
    public void setVehiclePicUrl(String vehiclePicUrl){this.vehiclePicUrl = vehiclePicUrl;}

    public String getOrCrUrl(){return orCrUrl;}
    public void setOrCrUrl(String orCrUrl){this.orCrUrl = orCrUrl;}

    public String getCertificationUrl(){return certificationUrl;}
    public void setCertificationUrl(String certificationUrl){this.certificationUrl = certificationUrl;}
}
