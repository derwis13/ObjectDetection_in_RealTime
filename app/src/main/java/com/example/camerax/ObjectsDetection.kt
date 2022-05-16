package com.example.objectdetectionapp

import android.content.Context
import android.graphics.*
import android.util.Log
import com.google.android.odml.image.MlImage
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.util.ArrayList

class ObjectsDetection(context_: Context) {
    private var context:Context = context_
    //val list= listOf<String>("remote")
    private val options= ObjectDetector.ObjectDetectorOptions.builder()
        .setMaxResults(maxResults)
        .setScoreThreshold(scoreThreshold)
        //.setLabelAllowList(list)
        .build()
    private val detector= ObjectDetector.createFromFileAndOptions(
        context,
        modelPath,
        options
    )
    private val lab=FileUtil.loadLabels(context,"labelmap.txt")
    //private val d=ObjectDetector.

    private var orientation:Pair<ImageProcessingOptions.Orientation,Int> =
        Pair(ImageProcessingOptions.Orientation.TOP_LEFT,0)
    private var imageOption=ImageProcessingOptions.builder()
        .setOrientation(orientation.first)
        .build()

    private lateinit var results: MutableList<Detection>

    companion object {
        //model
        private const val modelPath = "model_1.tflite"
        private const val maxResults = 15
        private const val scoreThreshold = 0.5f
        private val labels= mapOf(Pair(1,"apple"),Pair(81,"tv"))


        //boundbox
        private lateinit var mutableList: MutableList<Pair<RectF,String>>
        private lateinit var rectF: RectF
        private lateinit var text: String
        private const val boundboxStrokeWidth = 5f
        private const val boundboxStrokeColor = Color.BLACK
        private const val textSize = 50f


    }
    fun checkOrientation(orient:Int)
    {
        if(orient!=orientation.second) {
            if (orient==180)
                orientation= Pair(ImageProcessingOptions.Orientation.LEFT_BOTTOM,180)
            else
                orientation= Pair(ImageProcessingOptions.Orientation.TOP_LEFT,0)
        imageProcessingBuilder()
        }
    }
    fun imageProcessingBuilder()
    {
        imageOption=ImageProcessingOptions.builder()
            .setOrientation(orientation.first)
            .build()
    }

    fun runObjectDetection(image: MlImage) {

        mutableList= mutableListOf()

        checkOrientation(image.rotation)


        //TOP_LEFT <-
        //LEFT_BOTTOM ->

        //img_opt.setOrientation(ImageProcessingOptions.Orientation.LEFT_BOTTOM)
        results=detector.detect(image,imageOption)//img_opt
        //Log.d("index","${lab.withIndex().iterator()}")
        results.map {

            val category = it.categories.first()
            //Log.i("detect_TAG","${results.size}")
            //Log.d("index","${lab[category.index-1]}")
            //Log.d("index","${labels[category.index]}")

            text = "${lab[category.index]}, ${category.score.times(100).toInt()}%"
            rectF=it.boundingBox
            mutableList.add(Pair(rectF, text))
        }


    }
    fun runObjectDetection(bitmap: Bitmap) {

        val image=TensorImage.fromBitmap(bitmap)


        results=detector.detect(image,)

        results.map {
            val category = it.categories.first()

            text = "${category.label}, ${category.score.times(100).toInt()}%"
            rectF=it.boundingBox
        }
    }
    fun debugPrint() {

        for ((i, obj) in results.withIndex()) {
            val box = obj.boundingBox

            Log.d("detect_TAG", "Detected object: ${i} ")
            Log.d(
                "detect_TAG",
                "  boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})"
            )
            for ((j, category) in obj.categories.withIndex()) {
                Log.d("detect_TAG", "    Label $j: ${category.label}")
                val confidence: Int = category.score.times(100).toInt()
                Log.d("detect_TAG", "    Confidence: ${confidence}%")
            }
        }
    }

    fun drawBoundingBoxWithText(bitmap1: Bitmap):Bitmap {

        //val bitmap = imageView.drawToBitmap(Bitmap.Config.ARGB_8888)
        //val bitmap=Bitmap.createBitmap(bitmap1.width,bitmap1.height,Bitmap.Config.ARGB_8888)
        val bitmap=Bitmap.createBitmap(bitmap1.width,bitmap1.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)


        mutableList.forEach{
            paint.color = boundboxStrokeColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = boundboxStrokeWidth
            canvas.drawRect(it.first, paint)

            val margin=15f
            paint.textAlign= Paint.Align.LEFT
            paint.textSize= textSize
            paint.style= Paint.Style.FILL
            paint.getTextBounds(it.second,0,it.second.length, Rect(0,0,0,0))
            canvas.drawText(it.second,it.first.left+margin,it.first.top-margin,paint)
        }

        return bitmap
    }

}