package com.example.camerax.DataBase;

import android.graphics.RectF;

public class DetectedObject {

    private String name;
    private String id;
    private Float score;
    private Float distance;
    private Double alpha;
    private Double beta;
    private Float realRoadSize;
    private RectF boundBox;

    public DetectedObject(String name,
                           String id,
                           Float score,
                           Float distance,
                          Double alpha,
                          Double beta,
                           Float realRoadSize,
                           RectF boundBox){
        setName(name);
        setId(id);
        setScore(score);
        setDistance(distance);
        setAlpha(alpha);
        setBeta(beta);
        setRealRoadSize(realRoadSize);
        setBoundBox(boundBox);
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    public Float getDistance() {
        return distance;
    }

    public void setDistance(Float distance) {
        this.distance = distance;
    }

    public Double getAlpha() {
        return alpha;
    }

    public void setAlpha(Double alpha) {
        this.alpha = alpha;
    }

    public Double getBeta() {
        return beta;
    }

    public void setBeta(Double beta) {
        this.beta = beta;
    }

    public Float getRealRoadSize() {
        return realRoadSize;
    }

    public void setRealRoadSize(Float realRoadSize) {
        this.realRoadSize = realRoadSize;
    }

    public RectF getBoundBox() {
        return boundBox;
    }

    public void setBoundBox(RectF boundBox) {
        this.boundBox = boundBox;
    }
}
