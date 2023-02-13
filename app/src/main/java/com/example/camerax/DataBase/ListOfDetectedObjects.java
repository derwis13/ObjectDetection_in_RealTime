package com.example.camerax.DataBase;


import java.util.ArrayList;
import java.util.List;

public class ListOfDetectedObjects {
    private ArrayList<DetectedObject> detectedObjects;
    private ListOfDeviceLocation deviceLocations;
    private ListOfDeviceRotation deviceRotations;

    public ListOfDetectedObjects(ArrayList detectedObjects,
                                 ListOfDeviceLocation deviceLocations,
                                 ListOfDeviceRotation deviceRotations)
    {
        setDetectedObjects(detectedObjects);
        setDeviceLocations(deviceLocations);
        setDeviceRotations(deviceRotations);

    }

    public List<DetectedObject> getDetectedObjects() {
        return detectedObjects;
    }

    public void setDetectedObjects(ArrayList<DetectedObject> detectedObjectsList) {
        this.detectedObjects = detectedObjectsList;
    }

    public ListOfDeviceLocation getDeviceLocations() {
        return deviceLocations;
    }

    public void setDeviceLocations(ListOfDeviceLocation deviceLocations) {
        this.deviceLocations = deviceLocations;
    }

    public ListOfDeviceRotation getDeviceRotations() {
        return deviceRotations;
    }

    public void setDeviceRotations(ListOfDeviceRotation deviceRotations) {
        this.deviceRotations = deviceRotations;
    }
}
