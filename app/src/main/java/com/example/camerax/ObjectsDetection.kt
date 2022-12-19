package com.example.objectdetectionapp

import android.R
import android.content.Context
import android.graphics.*
import android.util.Log
import android.util.Size
import com.google.android.odml.image.MlImage
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector


class ObjectsDetection(context: Context,
                       scoreThreshold_:Float,
                       maxResults_:Int,
                       boundboxStrokeWidth_:Float,
                       textSize_:Float) {

    private val maxResults=maxResults_
    private val scoreThreshold=scoreThreshold_
    private val boundboxStrokeWidth=boundboxStrokeWidth_
    private val textSize=textSize_


    private val options= ObjectDetector.ObjectDetectorOptions.builder()
        .setMaxResults(maxResults)
        .setScoreThreshold(scoreThreshold)
        .build()
    private val detector= ObjectDetector.createFromFileAndOptions(
        context,
        modelPath,
        options
    )
    private val lab=FileUtil.loadLabels(context,"labelmap.txt")


    private lateinit var results: MutableList<Detection>

    companion object {
        //model
        private const val modelPath = "model_1.tflite"
        //private const val maxResults = 30
        //private const val scoreThreshold = 0.2f


        private const val boundboxStrokeColor = Color.BLACK
        //private const val textSize = 50f

    }

    fun runObjectDetection(image: MlImage) {
        results=detector.detect(image)
    }

    fun runObjectDetection(image: TensorImage) {
        results=detector.detect(image)
    }
    fun debugPrint() {

        for ((i, obj) in results.withIndex()) {
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
    fun scaleResultBoundBox(scale:Float){
        results.forEach {
            it.boundingBox.bottom=it.boundingBox.bottom*scale
            it.boundingBox.top=it.boundingBox.top*scale
            it.boundingBox.left=it.boundingBox.left*scale
            it.boundingBox.right=it.boundingBox.right*scale
        }
    }
    fun scaleResultBoundBox(scale_x:Float,scale_y:Float){
        results.forEach {
            it.boundingBox.bottom=it.boundingBox.bottom*scale_y
            it.boundingBox.top=it.boundingBox.top*scale_y
            it.boundingBox.left=it.boundingBox.left*scale_x
            it.boundingBox.right=it.boundingBox.right*scale_x
        }
    }
    fun moveResultBoundBox(x:Float,y:Float) {
        results.forEach {
            it.boundingBox.bottom = it.boundingBox.bottom + y
            it.boundingBox.top = it.boundingBox.top + y
            it.boundingBox.left = it.boundingBox.left + x
            it.boundingBox.right = it.boundingBox.right + x
            when{
                it.boundingBox.bottom<0.0->it.boundingBox.bottom= 0.0F
                it.boundingBox.top<0.0->it.boundingBox.top= 0.0F
                it.boundingBox.right<0.0->it.boundingBox.right= 0.0F
                it.boundingBox.left<0.0->it.boundingBox.left= 0.0F
            }
        }
    }

    fun drawBoundingBoxWithText(size: Size):Bitmap {

        val bitmap=Bitmap.createBitmap(size.width,size.height,Bitmap.Config.ARGB_8888)


        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for ((i,obj) in results.withIndex()){
            val box=obj.boundingBox
            for ((j, category) in obj.categories.withIndex()) {
                val text = "${lab[category.index]}, ${category.score.times(100).toInt()}%"

                paint.color = boundboxStrokeColor
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = boundboxStrokeWidth
                canvas.drawRect(box.left,box.top,
                    box.right,box.bottom,paint)

                val margin=15f
                paint.textAlign= Paint.Align.LEFT
                paint.textSize= textSize
                paint.style= Paint.Style.FILL
                paint.getTextBounds(text,0,text.length, Rect(0,0,0,0))
                canvas.drawText(text,box.left+margin,box.top-margin,paint)
            }
        }
        return bitmap
    }

    fun getCountOfResults():Int{
        return results.size
    }
    fun getResult(index:Int):Detection {
        return results[index]
    }
    fun drawBoundingBoxWithText(width:Int,height:Int):Canvas{
        val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for ((i,obj) in results.withIndex()) {
            val box = obj.boundingBox
            for ((j, category) in obj.categories.withIndex()) {
                val text = "${lab[category.index]}, ${category.score.times(100).toInt()}%"

                paint.color = boundboxStrokeColor
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = boundboxStrokeWidth
                canvas.drawRect(
                    box.left, box.top,
                    box.right, box.bottom, paint
                )

                val margin = 15f
                paint.textAlign = Paint.Align.LEFT
                paint.textSize = textSize
                paint.style = Paint.Style.FILL
                paint.getTextBounds(text, 0, text.length, Rect(0, 0, 0, 0))
                canvas.drawText(text, box.left + margin, box.top - margin, paint)

            }
        }
        return canvas
    }

}