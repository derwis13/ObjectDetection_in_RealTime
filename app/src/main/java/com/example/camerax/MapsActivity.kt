package com.example.camerax

import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.example.camerax.databinding.ActivityMapBinding
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapBinding

    private var signCategoryList= arrayListOf<String>(
        "trafficlight",
        "speedlimit",
        "crosswalk",
        "stop")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        actionBar?.hide()

        FirebaseApp.initializeApp(this)

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    fun readDatabase(){

        val database: DatabaseReference = Firebase.database.reference
        database.get().addOnCompleteListener {
            if(it.result.exists()){
                val dataSnapshot=it.result
                var sign=LatLng(0.0,0.0)
                var name=""
                var score=0.0

                dataSnapshot.children.forEach{
                    var temp_longitude=0.1
                    var temp_latitude=0.1

                    it.child("frames").children.forEach {

                        var longitude=0.0
                        var latitude=0.0
                        var distance=0.0
                        var accuracy=0.0

                        var angle=0.0
                        var azimuth=0.0
                        var pitch=0.0
                        var roll=0.0
                        it.child("detectedObjects").children.forEach{
                            it.children.forEach{
                                when(it.key){
                                    "distance"->{ distance= it.value.toString().toDouble() }
                                    "name" -> {name=it.value as String}
                                    "score" -> {score=it.value.toString().toDouble()}
                                    "alpha" -> {angle=it.value.toString().toDouble()}
                                    else -> {}
                                }
                            }
                        }
                        it.child("deviceLocations").children.forEach{

                            when (it.key) {
                                 "latitude"-> {
                                     latitude=it.value as Double
                                     if(latitude==temp_latitude){
                                         latitude=0.0
                                     }else
                                         temp_latitude=latitude

                                 }
                                 "longitude"-> {
                                     longitude = it.value as Double

                                     if(longitude==temp_longitude) {
                                         longitude=0.0
                                     }else
                                         temp_longitude=longitude
                                 }
                                "accuracy"->{
                                    accuracy=it.value as Double
                                }
                                else -> {}
                            }
                        }

                        it.child("deviceRotations").children.forEach{
                            when (it.key) {
                                "azimuth"-> {azimuth=it.value as Double }
                                "pitch"-> {pitch= it.value as Double }
                                "roll"-> {roll= it.value as Double }
                                else -> {}
                            }
                        }
                        if(it.hasChild("detectedObjects") && score>0.00 && longitude!=0.0) {
                            sign=calculateToCoordinates(
                                LatLng(latitude,longitude),
                                floatArrayOf(-azimuth.toFloat(),pitch.toFloat(),roll.toFloat()),
                                distance/1000,
                                angle
                            )
                        }

                    }
                    mMap.addMarker(
                        MarkerOptions().position(sign)
                            .title("${name} ${score}"))
                }


            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    fun calculateToCoordinates(coordinate:LatLng, angle:FloatArray, distance:Double, angle1:Double):LatLng{
        val rotationMatrix=FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix,angle)

        val distance= doubleArrayOf(distance*Math.cos(angle1),-distance*Math.sin(angle1),0.0)

        val dx=distance[0]*Math.cos(angle[0].toDouble())*Math.cos(angle[1].toDouble())+
                distance[1]*Math.sin(angle[0].toDouble())+Math.cos(angle[1].toDouble())-
                distance[2]*Math.sin(angle[1].toDouble())
        val dy=distance[0]*(Math.cos(angle[0].toDouble())*Math.sin(angle[1].toDouble())*Math.sin(angle[2].toDouble())-
                Math.sin(angle[0].toDouble())*Math.cos(angle[2].toDouble()))+
                distance[1]*(Math.sin(angle[0].toDouble())*Math.sin(angle[1].toDouble())*Math.sin(angle[2].toDouble())+
                Math.cos(angle[0].toDouble())*Math.cos(angle[2].toDouble()))+
                distance[2]*Math.cos(angle[1].toDouble())*Math.sin(angle[2].toDouble())
        var dz=distance[0]*(Math.cos(angle[0].toDouble())*Math.sin(angle[1].toDouble())*Math.cos(angle[2].toDouble())+
                Math.sin(angle[0].toDouble())*Math.sin(angle[2].toDouble()))+
                distance[1]*(Math.sin(angle[0].toDouble())*Math.sin(angle[1].toDouble())*Math.cos(angle[2].toDouble())-
                Math.cos(angle[0].toDouble())*Math.sin(angle[2].toDouble()))+
                distance[2]*Math.cos(angle[1].toDouble())*Math.cos(angle[2].toDouble())


        val lat=coordinate.latitude+(180.0/Math.PI)*(dx/6378137.0)
        val lon=coordinate.longitude+(180.0/Math.PI)*(dy/6378137.0)/Math.cos(Math.PI/180.0*coordinate.latitude)


        return LatLng(lat,lon)

    }
    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap

        mMap.setMapType(com.google.android.gms.maps.GoogleMap.MAP_TYPE_SATELLITE)

        readDatabase()

    }
}