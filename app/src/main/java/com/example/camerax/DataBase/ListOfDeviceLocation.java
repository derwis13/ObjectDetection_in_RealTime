package com.example.camerax.DataBase;

public class ListOfDeviceLocation {
    private Double latitude;
    private Double longitude;
    private Double altitude;
    private Double accuracy;

    public ListOfDeviceLocation(Double altitude, Double latitude, Double longitude, Double accuracy){
        setAltitude(altitude);
        setLatitude(latitude);
        setLongitude(longitude);
        setAccuracy(accuracy);
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public Double getAccuracy() { return accuracy; }

    public void setAccuracy(Double accurancy) { this.accuracy = accurancy; }
}
