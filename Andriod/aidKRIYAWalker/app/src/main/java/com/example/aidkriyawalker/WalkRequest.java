package com.example.aidkriyawalker;

public class WalkRequest {
    public String id;
    public String userId;
    public String walkDateTime;
    public String location;
    public int duration;
    public String preferences;
    public String notes;
    public String status;
    public String walkerId;

    public WalkRequest() {} // Firestore requires public no-arg constructor
}