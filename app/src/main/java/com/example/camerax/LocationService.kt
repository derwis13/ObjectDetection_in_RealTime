package com.example.camerax

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.annotation.RequiresApi
import com.google.android.gms.location.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.Task

class LocationService(private var context: Context){


    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    val locationRequest=LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,1)
        .setWaitForAccurateLocation(true)
        .build()


    private var locationCallback: LocationCallback = object : LocationCallback() {
        @RequiresApi(33)
        override fun onLocationResult(locationResult: LocationResult) {
            val locationList = locationResult.locations
            if (locationList.isNotEmpty()) {
                val location = locationList.last()
            }
        }
    }
    @SuppressLint("MissingPermission")
    fun requestLocationUpdates(){
        fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback, Looper.getMainLooper())
    }
    @SuppressLint("MissingPermission")
    fun getLastLocation(): Task<Location> {
        return fusedLocationClient.lastLocation
    }

}