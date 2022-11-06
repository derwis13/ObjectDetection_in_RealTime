package com.example.camerax

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import com.example.camerax.databinding.CameraLayoutBinding
import com.example.objectdetectionapp.ObjectsDetection
import org.tensorflow.lite.support.image.TensorImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraActivity(): AppCompatActivity() {
    private lateinit var objectsDetection: ObjectsDetection
    private lateinit var viewBinding: CameraLayoutBinding
    private lateinit var cameraExecutor: ExecutorService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CameraLayoutBinding.inflate(layoutInflater).also { viewBinding=it}
        setContentView(viewBinding.root)
        Executors.newSingleThreadExecutor().also {cameraExecutor = it }

        val intent = intent
        val floatarray=intent.getFloatArrayExtra("name")
        objectsDetection=ObjectsDetection(this,
                floatarray!!.component1(),
                floatarray!!.component2().toInt(),
                floatarray!!.component3(),
                floatarray!!.component4())

        if (allPermissionsGranted()) {
            startCamera(objectsDetection)

        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera(objectsDetection)
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
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
                                val bitmap=toBitmap(it.image!!).let {
                                    rotateBitmap(it!!,rotationDegrees.toFloat()).let { it1 ->
                                        it1!!.scale(viewBinding.imageView.width,viewBinding.imageView.height)
                                    }
                                }
                                TensorImage.fromBitmap(bitmap).also {
                                        objectsDetection.runObjectDetection(it)}

                                objectsDetection.debugPrint()

                                this@CameraActivity.runOnUiThread(Runnable {
                                    this.viewBinding.imageView.scaleType=ImageView.ScaleType.CENTER
                                    this.viewBinding.imageView.setImageBitmap(objectsDetection.drawBoundingBoxWithText(bitmap))})
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
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}