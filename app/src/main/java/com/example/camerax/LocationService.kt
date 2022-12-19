package com.example.camerax

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

import android.location.Location
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.gms.location.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.Task

class LocationService(private var context: Context){

    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 1000
        fastestInterval = 1000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        maxWaitTime = 2000
    }

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val locationList = locationResult.locations
            if (locationList.isNotEmpty()) {
                val location = locationList.last()
                //Toast.makeText(context, "${location.longitude},${location.latitude}", Toast.LENGTH_LONG).show()
            }
        }
    }
    @SuppressLint("MissingPermission")
    fun requestLocationUpdates(){
        fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper())
    }
    @SuppressLint("MissingPermission")
    fun getLastLocation(): Task<Location> {
        return fusedLocationClient.lastLocation
    }

}