package com.example.camerax

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.Image
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import com.example.camerax.databinding.ActivityMainBinding
import com.example.objectdetectionapp.ObjectsDetection
import org.tensorflow.lite.support.image.TensorImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

open class CameraActivity(): AppCompatActivity() {



    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    protected fun viewBinding_(){
        ActivityMainBinding.inflate(layoutInflater).also { viewBinding = it }
        setContentView(viewBinding.root)
        Executors.newSingleThreadExecutor().also { cameraExecutor = it }
    }

    private fun toBitmap(image: Image): Bitmap? {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }

    @SuppressLint("UnsafeOptInUsageError")
    protected fun startCamera(objectsDetection: ObjectsDetection) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val imageAnalysis = ImageAnalysis.Builder()
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it ->
                    it.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { it ->
                        val rotationDegrees = it.imageInfo.rotationDegrees

                        try {
                            if (it.image!=null) {
                                val bitmap=toBitmap(it.image!!)!!.let {
                                    rotateBitmap(it,rotationDegrees.toFloat()).let {it1->
                                        it1!!.scale(viewBinding.imageView.width,viewBinding.imageView.height)
                                    }
                                }
                                TensorImage.fromBitmap(bitmap).let {
                                    objectsDetection.runObjectDetection(it)}

                                //objectsDetection.debugPrint()
                                this@CameraActivity.runOnUiThread(Runnable {
                                    this.viewBinding.imageView.setImageBitmap(objectsDetection.drawBoundingBoxWithText(bitmap)) })
                            }
                        }catch (exc: Exception){
                            Log.e("ObjectDetection","Fail detect objects on image",exc)
                        }
                        it.close()
                    })
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview,imageAnalysis)
            } catch(exc: Exception) {
                Log.e(CameraActivity.TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }
    fun rotateBitmap(source: Bitmap, angle: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }


    companion object {
        private const val TAG = "CameraXApp"
    }
}