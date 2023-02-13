package com.example.camerax

import android.content.Context
import android.graphics.*
import android.util.Size
import android.util.SizeF
import com.example.objectdetectionapp.ObjectsDetection
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.task.vision.detector.Detection

class BoundingBox(private var results: MutableList<Detection>,
                  context: Context) {

    private val lab= FileUtil.loadLabels(context,"labelmap.txt")

    private val boundboxStrokeColor = Color.BLACK
    private var targetImageResolution:Size?=null
    private var scale= SizeF(1f,1f)


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

    fun drawBoundingBoxWithText(size: Size,i:Int,boundboxStrokeWidth:Float,
                                textSize:Float): Bitmap {

        val bitmap= Bitmap.createBitmap(size.width,size.height, Bitmap.Config.ARGB_8888)

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

    fun drawBoundingBoxWithText(boundboxStrokeWidth:Float,
                                textSize:Float): Bitmap {

        val bitmap= Bitmap.createBitmap(targetImageResolution!!.width,targetImageResolution!!.height,
            Bitmap.Config.ARGB_8888)

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
    fun setTargetImageResolution(resolution:Size){
        targetImageResolution=resolution
    }

    fun getScale(): SizeF {
        return scale
    }

    fun getTargetImageResolution():Size?{
        return targetImageResolution
    }
    fun setScale(scale:SizeF){
        this.scale=scale
    }
    fun getBoundingBox(index:Int):Detection?{
        if(results.size>index)
            return results[index]
        else
            return null
    }
}