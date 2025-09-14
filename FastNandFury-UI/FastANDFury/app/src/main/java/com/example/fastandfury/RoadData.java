package com.example.fastandfury;

public class RoadData {
    private String streetName;
    private String placeId;
    private double latitude;
    private double longitude;
    private String congestionLevel; // ← должно быть это поле
    private double deviation;
    private double rating;
    private int photoCount;
    private long timestamp;

    public RoadData() {
        // Пустой конструктор
    }

    public RoadData(String streetName, String placeId, double latitude, double longitude,
                    String congestionLevel, double deviation, double rating, int photoCount) {
        this.streetName = streetName;
        this.placeId = placeId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.congestionLevel = congestionLevel; // ← инициализируем
        this.deviation = deviation;
        this.rating = rating;
        this.photoCount = photoCount;
        this.timestamp = System.currentTimeMillis();
    }

    // Геттеры и сеттеры
    public String getStreetName() { return streetName; }
    public void setStreetName(String streetName) { this.streetName = streetName; }

    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    // ↓↓↓ ДОБАВЛЯЕМ ЭТОТ МЕТОД ↓↓↓
    public String getCongestionLevel() { return congestionLevel; }
    public void setCongestionLevel(String congestionLevel) { this.congestionLevel = congestionLevel; }

    public double getDeviation() { return deviation; }
    public void setDeviation(double deviation) { this.deviation = deviation; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getPhotoCount() { return photoCount; }
    public void setPhotoCount(int photoCount) { this.photoCount = photoCount; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return String.format(
                "Улица: %s\nID: %s\nКоординаты: %.6f, %.6f\nПробки: %s\n" +
                        "Отклонение: %.2f\nРейтинг: %.1f\nФото: %d\nВремя: %s",
                streetName, placeId, latitude, longitude, congestionLevel,
                deviation, rating, photoCount, new java.util.Date(timestamp)
        );
    }
}