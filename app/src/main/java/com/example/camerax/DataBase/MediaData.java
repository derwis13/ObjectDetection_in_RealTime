package com.example.camerax.DataBase;

import java.util.ArrayList;

public class MediaData {

    private String extension;
    private ArrayList<ListOfDetectedObjects> frames;


    public MediaData(String extension,ArrayList<ListOfDetectedObjects> listOfDetectedObjects){
        setExtension(extension);
        setFrames(listOfDetectedObjects);
    }
    public MediaData(){
    }


    public void setFrames(ArrayList<ListOfDetectedObjects> frames) {
        this.frames = frames;
    }
    public ArrayList<ListOfDetectedObjects> getFrames(){
        return frames;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }
}
