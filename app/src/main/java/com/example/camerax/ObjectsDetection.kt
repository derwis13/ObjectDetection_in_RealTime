package com.example.objectdetectionapp

import android.R
import android.content.Context
import android.graphics.*
import android.util.Log
import android.util.Size
import android.util.SizeF
import com.google.android.odml.image.MlImage
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector


class ObjectsDetection(context: Context,
                       private var scoreThreshold:Float,
                       private var maxResults:Int) {


    private var targetImageResolution:Size?=null
    private var rotationDegrees=0

    private val options= ObjectDetector.ObjectDetectorOptions.builder()
        .setMaxResults(maxResults)
        .setScoreThreshold(scoreThreshold)
        .build()
    private val detector= ObjectDetector.createFromFileAndOptions(
        context,
        modelPath,
        options
    )

    private lateinit var results: MutableList<Detection>

    companion object {
        //model
        private const val modelPath = "model_1.tflite"

    }

    fun runObjectDetection(image: MlImage) {
        setTargetImageResolution(Size(image.width,image.height))
        results=detector.detect(image)
    }


    fun runObjectDetection(image: TensorImage) {
        setTargetImageResolution(Size(image.width,image.height))
        results=detector.detect(image)
    }
    fun debugPrint() {

        for ((i, obj) in results!!.withIndex()) {
            val box = obj.boundingBox
            Log.d("detect_TAG", "Detected object: ${i} ")
            Log.d(
                "detect_TAG",
                "  boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})")
            for ((j, category) in obj.categories.withIndex()) {
                Log.d("detect_TAG", "    Label $j: ${category.label}")
                val confidence: Int = category.score.times(100).toInt()
                Log.d("detect_TAG", "    Confidence: ${confidence}%")
            }
        }
    }

    fun getCountOfResults():Int{
        return results!!.size
    }
    fun getResult(index:Int):Detection? {
        if(getCountOfResults()>index)
            return results!![index]
        else
            return null
    }
    fun getResults():MutableList<Detection>{
        var res= mutableListOf<Detection>()

        results.forEach {
            res.add(Detection.create(it.boundingBox,it.categories))
        }
        return res
    }

    fun setTargetImageResolution(resolution:Size){
        targetImageResolution=resolution
    }

    fun getTargetImageResolution():Size?{
        return targetImageResolution
    }

    fun setRotationDegrees(degrees:Int){
        rotationDegrees=degrees
    }

    fun getRotationDegrees():Int{
        return rotationDegrees
    }

    fun getOrginalResults():MutableList<Detection>{
        return results
    }


}