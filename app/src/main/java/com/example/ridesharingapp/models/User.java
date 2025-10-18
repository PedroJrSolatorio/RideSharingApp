package com.example.ridesharingapp.models;

public class User {
    private String userId;
    private String name;
    private String email;
    private String phone;
    private String profileImageUrl;
    private String userType; // "rider" or "driver"
    private String birthdate;
    private String validIdUrl;
    private boolean isVerified;
    private double rating;
    private boolean isActive;
    private String gender; // "Male", "Female", "Other"

    // Default constructor required for Firebase
    public User(){}

    // Full constructor
    public User(String userId, String name, String email, String phone, String profileImageUrl, String userType, String birthdate, String validIdUrl, boolean isVerified, double rating, boolean isActive){
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.profileImageUrl = profileImageUrl;
        this.userType = userType;
        this.birthdate = birthdate;
        this.validIdUrl = validIdUrl;
        this.isVerified = isVerified;
        this.rating = rating;
        this.isActive = isActive;
    }

    // Getters and Setters
    public String getUserId(){
        return userId;
    }

    public void setUserId(String userId){
        this.userId = userId;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getEmail(){
        return email;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public String getPhone(){
        return phone;
    }

    public void setPhone(String phone){
        this.phone = phone;
    }

    public String getProfileImageUrl(){
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl){
        this.profileImageUrl = profileImageUrl;
    }

    public String getUserType(){
        return userType;
    }

    public void setUserType(String userType){
        this.userType = userType;
    }

    public String getBirthdate(){
        return birthdate;
    }

    public void setBirthdate(String birthdate){
        this.birthdate = birthdate;
    }

    public String getValidIdUrl(){
        return validIdUrl;
    }

    public void setValidIdUrl(String validIdUrl){
        this.validIdUrl = validIdUrl;
    }

    public boolean isVerified(){
        return isVerified;
    }

    public void setVerified(boolean verified){
        isVerified = verified;
    }

    public double getRating(){
        return rating;
    }

    public void setRating(double rating){
        this.rating = rating;
    }

    public boolean isActive(){
        return isActive;
    }

    public void setActive(boolean active){
        isActive = active;
    }

    public String getGender(){
        return gender;
    }

    public void setGender(String gender){
        this.gender = gender;
    }
}
