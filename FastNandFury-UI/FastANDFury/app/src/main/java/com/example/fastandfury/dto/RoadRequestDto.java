package com.example.fastandfury.dto;

import com.example.fastandfury.RoadData;

public final class RoadRequestDto {

    private String streetName;
    private String placeId;
    private String congestionLevel;
    private double deviation;
    private double rating;
    private int photoCount;

    public RoadRequestDto(RoadData roadData) {
        this.streetName = roadData.getStreetName();
        this.placeId = roadData.getPlaceId();
        this.congestionLevel = roadData.getCongestionLevel();
        this.deviation = roadData.getDeviation();
        this.rating = roadData.getRating();
        this.photoCount = roadData.getPhotoCount();
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getCongestionLevel() {
        return congestionLevel;
    }

    public void setCongestionLevel(String congestionLevel) {
        this.congestionLevel = congestionLevel;
    }

    public double getDeviation() {
        return deviation;
    }

    public void setDeviation(double deviation) {
        this.deviation = deviation;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getPhotoCount() {
        return photoCount;
    }

    public void setPhotoCount(int photoCount) {
        this.photoCount = photoCount;
    }
}
