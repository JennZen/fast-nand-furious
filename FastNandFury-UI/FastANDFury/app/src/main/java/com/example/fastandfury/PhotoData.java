package com.example.fastandfury;

public class PhotoData {
    private String address;
    private double latitude;
    private double longitude;
    private String photoBase64;
    private long timestamp;

    public PhotoData(String address, double latitude, double longitude, String photoBase64, long timestamp) {
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.photoBase64 = photoBase64;
        this.timestamp = timestamp;
    }

    // Геттеры и сеттеры
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getPhotoBase64() { return photoBase64; }
    public void setPhotoBase64(String photoBase64) { this.photoBase64 = photoBase64; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}