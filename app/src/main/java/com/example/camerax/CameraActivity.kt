package com.example.camerax

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Display
import android.view.View
import android.view.ViewOverlay
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import com.example.camerax.databinding.CameraLayoutBinding
import com.example.objectdetectionapp.ObjectsDetection
import com.google.firebase.FirebaseApp
import org.tensorflow.lite.support.image.TensorImage
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors



class CameraActivity(): AppCompatActivity() {
    private lateinit var objectsDetection: ObjectsDetection
    private lateinit var viewBinding: CameraLayoutBinding
    private lateinit var cameraExecutor: ExecutorService
    //private lateinit var outputStream: FileOutputStream
    private var imageCapture: ImageCapture? = null

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
        //viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
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

            imageCapture = ImageCapture.Builder().build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it ->
                    it.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { it ->
                        val rotationDegrees = it.imageInfo.rotationDegrees


                        try {
                            if (it.image!=null) {
                                var bitmap=toBitmap(it.image!!).let {
                                    rotateBitmap(it!!,rotationDegrees.toFloat()).let { it1 ->
                                        it1!!.scale(viewBinding.imageView.width,viewBinding.imageView.height)
                                    }
                                }
                                TensorImage.fromBitmap(bitmap).also {
                                        objectsDetection.runObjectDetection(it)}


                                objectsDetection.debugPrint()
                                bitmap=objectsDetection.drawBoundingBoxWithText(bitmap)
                                viewBinding.imageCaptureButton.setOnClickListener {

                                    DataSave(applicationContext).let {
                                        it.saveBitmapAsJPEG(bitmap)
                                        Log.d("Uri_d","${it.getUri()}")
                                        it.getUri()?.let { CloudConnection(applicationContext).upload(it,"bounded_images/") }
                                    }

                                    //ataSave(applicationContext).saveBitmapAsJPEG(bitmap)
                                    //cloudConnection.upload(DataSave(applicationContext).getUri()!!)
                                    //CloudConnection(applicationContext).upload(DataSave(applicationContext).getUri()!!)

                                }

                                this@CameraActivity.runOnUiThread(Runnable {
                                    this.viewBinding.imageView.scaleType=ImageView.ScaleType.CENTER
                                    this.viewBinding.imageView.setImageBitmap(bitmap)

                                })
                            }
                        }catch (exc: Exception){
                            Log.e("ObjectDetection","Fail detect objects on image",exc)
                        }
                        it.close()
                    })
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview,imageAnalysis,imageCapture)
            } catch(exc: Exception) {
                Log.e(CameraActivity.TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

//    @SuppressLint("RestrictedApi")
//    private fun takePhoto() {
//        // Get a stable reference of the modifiable image capture use case
//        val imageCapture = imageCapture ?: return
//
//        // Create time stamped name and MediaStore entry.
//        val name=DataTextSave(applicationContext).getNameFile()
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
//            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
//            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
//            }
//        }
//
//        // Create output options object which contains file + metadata
//        val outputOptions = ImageCapture.OutputFileOptions
//            .Builder(contentResolver,
//                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                contentValues)
//            .build()
//
//        // Set up image capture listener, which is triggered after photo has
//        // been taken
//
//        imageCapture.takePicture(
//            outputOptions,
//            ContextCompat.getMainExecutor(this),
//            object : ImageCapture.OnImageSavedCallback {
//                override fun onError(exc: ImageCaptureException) {
//                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
//                }
//
//                @SuppressLint("MissingPermission")
//                override fun
//                        onImageSaved(output: ImageCapture.OutputFileResults){
//                    val msg = "Photo capture succeeded: ${output.savedUri}"
//                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
//                    Log.d(TAG, msg)
////                    fusedLocationClient.lastLocation
////                        .addOnSuccessListener { location: Location? ->
////                            Log.d("loc_tag", "$location")
////                            dataTextSave.writeFileExternalStorage(
////                                " ${location!!.latitude.toString()}" +
////                                        " ${location!!.longitude.toString()} \n"
////                            )
////                        }
//                }
//            }
//        )
//    }
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
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE

            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}