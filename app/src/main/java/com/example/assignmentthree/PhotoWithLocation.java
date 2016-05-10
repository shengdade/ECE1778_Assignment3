package com.example.assignmentthree;

import android.location.Location;

import java.io.Serializable;

/**
 * Created by dade on 03/02/16.
 */
public class PhotoWithLocation implements Serializable {

    public byte[] data;
    public double latitude;
    public double longitude;

    public PhotoWithLocation(byte[] data, Location location) {
        this.data = data;
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
    }

    public PhotoWithLocation(byte[] data, double latitude, double longitude) {
        this.data = data;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
