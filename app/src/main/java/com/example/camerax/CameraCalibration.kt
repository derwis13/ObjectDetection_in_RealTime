package com.example.camerax

import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.util.Size
import android.util.SizeF

class CameraCalibration(cameraManager: CameraManager) {
    private var cameraCharacteristics: CameraCharacteristics =cameraManager.getCameraCharacteristics(cameraManager.cameraIdList[0])
    private var sensor_dimension:SizeF=cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)!!
    private var focal_length:Float=cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)!![0]


    fun getSensorDimension(): SizeF {
        return sensor_dimension
    }
    fun getFocalLength(): Float {
        return focal_length
    }
    fun getCameraDimension():Float{
        return (sensor_dimension.width+sensor_dimension.height)/2
    }
    fun calculateDistanseFromObject(
        objectHeight_mm: Float,
        objectHeight_px: Float,
        imageHeight_px: Int
    ): Float {
        return focal_length * objectHeight_mm * imageHeight_px / (objectHeight_px * (sensor_dimension.width+sensor_dimension.height)/2)
            }

    fun calculateScale(
        objectHeight_mm: Float,
        objectHeight_px: Float
    ):Float{
      return objectHeight_mm/objectHeight_px
    }

    fun calculateAngleFromCameraCenter(imageSize: Size,boundBox:RectF,dist:Float,scale:Float):DoubleArray{
        val imageCenterPoint=Point(imageSize.width/2,imageSize.height/2)
        val objectCenterPoint=PointF(boundBox.centerX(),boundBox.centerY())

        val dx=objectCenterPoint.x-imageCenterPoint.x
        val dy=objectCenterPoint.y-imageCenterPoint.y

        val alpha=Math.asin(dx.toDouble()*scale/dist.toDouble())
        val beta=Math.asin(dy.toDouble()*scale/dist.toDouble())

        return doubleArrayOf(alpha,beta)

    }
    fun calculateSizeObjectOnPicture(
        deltaS_mm: Float,
        objectHeight_px: Float,
        imageHeight_px: Int
    ):Float {
        return (sensor_dimension.width+sensor_dimension.height)/2*deltaS_mm/(objectHeight_px*imageHeight_px*focal_length)
    }
    fun degrees(rad:DoubleArray):DoubleArray{
        return doubleArrayOf(rad[0]*180/Math.PI,rad[1]*180/Math.PI)}
}