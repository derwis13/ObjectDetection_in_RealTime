package com.example.camerax

import android.app.Service
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import java.util.*

class SensorService(private var sensorManager: SensorManager,lifecycle: Lifecycle): LifecycleObserver,SensorEventListener {

    private var mGyroscope: Sensor? = null
    private lateinit var gyroscopeResult:Triple<Float,Float,Float>

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val temp_rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)


    init {
        lifecycle.addObserver(this)

    }

    // Get updates from the accelerometer and magnetometer at a constant rate.
    // To make batch operations more efficient and reduce power consumption,
    // provide support for delaying updates to the application.
    //
    // In this example, the sensor reporting delay is small enough such that
    // the application receives an update before the system checks the sensor
    // readings again.
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onReasume(){
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                this,
                magneticField,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause(){
        sensorManager.unregisterListener(this)
    }
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    override fun onSensorChanged(event: SensorEvent) {

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    fun updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        // "rotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // "orientationAngles" now has up-to-date information.
    }

    fun degrees(rad:Float):Double{return rad*180/Math.PI}
    fun degrees(rad:FloatArray):DoubleArray{
        return doubleArrayOf(rad[0]*180/Math.PI,rad[1]*180/Math.PI,rad[2]*180/Math.PI)
    }

    fun getDegreesSensorResult():DoubleArray{
        updateOrientationAngles()
        return degrees(orientationAngles)
    }
    fun getSensorResult():FloatArray{
        updateOrientationAngles()
        return orientationAngles
    }
}

