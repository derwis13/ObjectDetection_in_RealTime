package com.example.camerax.DataBase;

public class ListOfDeviceRotation {

    private Float azimuth;
    private Float pitch;
    private Float roll;

    public ListOfDeviceRotation(Float azimuth,Float pitch, Float roll){
        setAzimuth(azimuth);
        setPitch(pitch);
        setRoll(roll);
    }

    public Float getAzimuth() {
        return azimuth;
    }

    public void setAzimuth(Float azimuth) {
        this.azimuth = azimuth;
    }

    public Float getPitch() {
        return pitch;
    }

    public void setPitch(Float pitch) {
        this.pitch = pitch;
    }

    public Float getRoll() {
        return roll;
    }

    public void setRoll(Float roll) {
        this.roll = roll;
    }

}
