package com.example.camerax

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.location.Location
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.util.SizeF
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.camerax.DataBase.*
import com.example.camerax.databinding.CameraLayoutBinding
import com.example.objectdetectionapp.ObjectsDetection
import kotlinx.coroutines.*
import org.tensorflow.lite.support.image.TensorImage
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class CameraActivity(): AppCompatActivity(){
    private lateinit var objectsDetection: ObjectsDetection
    private lateinit var viewBinding: CameraLayoutBinding

    private lateinit var cameraExecutor: ExecutorService

    private var imageCapture: ImageCapture? = null

    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private var photoUri:Uri?=null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private var isPhotoMode=false

    private var name:String=""

    private lateinit var locationService:LocationService
    private lateinit var cameraManager: CameraManager
    private lateinit var sensorManager:SensorManager

    private lateinit var sensorService:SensorService

    private var boundboxStrokeWidth:Float = 0.0f
    private var textSize:Float = 0.0f

    private var data2Save:ArrayList<ListOfDetectedObjects>?=null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        actionBar?.hide()

        CameraLayoutBinding.inflate(layoutInflater).also { viewBinding=it}
        setContentView(viewBinding.root)

        locationService=LocationService(applicationContext)

        cameraManager=getSystemService(Context.CAMERA_SERVICE) as CameraManager

        sensorManager= getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorService=SensorService(sensorManager,this.lifecycle,this)


        intent.getFloatArrayExtra("name")?.let {
            objectsDetection= ObjectsDetection(
                this,
                it.component1(),
                it.component2().toInt()
            )
            boundboxStrokeWidth=it.component3()
            textSize=it.component4()
        }

        if (allPermissionsGranted()) {
            startCamera(objectsDetection)
            locationService.requestLocationUpdates()
        }
        else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        viewBinding.videoCaptureButton.setOnClickListener {
            if(isPhotoMode)
                takePhoto()
            else
                captureVideo()
        }

        Executors.newSingleThreadExecutor().also {cameraExecutor = it }
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
    // Implements VideoCapture use case, including start and stop capturing.
    @SuppressLint("MissingPermission", "RestrictedApi")
    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return

        viewBinding.videoCaptureButton.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            return
        }

        // create and start a new recording session
        name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(applicationContext,
                        Manifest.permission.RECORD_AUDIO) ==
                    PermissionChecker.PERMISSION_GRANTED)
                {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        viewBinding.videoCaptureButton.apply {
                            text = getString(R.string.stop_capture)
                            isEnabled = true
                        }
                    }
                    is VideoRecordEvent.Status ->{
                        videoCapture.resolutionInfo?.let { onRecordingProcess(it.resolution) }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
                                .show()
                            Log.d(TAG, msg)
                            videoCaptureButton(recordEvent.outputResults.outputUri,name,"mp4","videos/")
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: " +
                                    "${recordEvent.error}")
                        }
                        viewBinding.videoCaptureButton.apply {
                            text = getString(R.string.start_capture)
                            isEnabled = true
                        }
                    }
                }
            }
    }

    private fun takePhoto(){
        // Get a stable reference of the modifiable image capture use case

        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()
        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    photoUri=output.savedUri

                    GlobalScope.async {
                        imageCapture.resolutionInfo?.let { onRecordingProcess(it.resolution) }
                        delay(1000)

                        photoUri?.let {
                            videoCaptureButton(it,name,"jpg","images/")
                        }

                    }

                }
            }
        )
    }
    fun toBitmap(image: Image): Bitmap {
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

    fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera(objectsDetection: ObjectsDetection) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }


            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST, FallbackStrategy.higherQualityOrLowerThan(
                    Quality.SD)))
                .build()

            imageCapture = ImageCapture.Builder()
                .build()
            videoCapture = VideoCapture.withOutput(recorder)



            val imageAnalysis = ImageAnalysis.Builder()
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { image ->
                        val rotationDegrees = image.imageInfo.rotationDegrees

                        //detect object on image
                        image.image?.let { it ->
                            val bitmap = toBitmap(it).let { bitmap ->
                                rotateBitmap(
                                    bitmap,
                                    rotationDegrees.toFloat()
                                )
                            }

                            try {
                                TensorImage.fromBitmap(bitmap).also {
                                    objectsDetection.runObjectDetection(it)
                                }
                            } catch (exc: Exception) {
                                Log.e("ObjectDetection", "Fail detect objects on image", exc)
                            }


                            val boundingBox=BoundingBox(objectsDetection.getResults(),applicationContext).also {
                                val add_x = -(bitmap.height - image.height) / 2
                                val add_y = -(bitmap.width - image.height) / 2
                                it.moveResultBoundBox(add_x.toFloat(),add_y.toFloat())
                                it.setTargetImageResolution(Size(viewBinding.viewFinder.width,viewBinding.viewFinder.height))
                                it.setScale(
                                    SizeF(it.getTargetImageResolution()!!.width.toFloat()/objectsDetection.getTargetImageResolution()!!.width,
                                        it.getTargetImageResolution()!!.height.toFloat()/objectsDetection.getTargetImageResolution()!!.height))
                                if(it.getScale().width>it.getScale().height)
                                    it.scaleResultBoundBox(it.getScale().width)
                                else
                                    it.scaleResultBoundBox(it.getScale().height)
                            }
                            val boundedBitmap=boundingBox.drawBoundingBoxWithText(boundboxStrokeWidth,textSize)
                            objectsDetection.setRotationDegrees(rotationDegrees)



                            sensorService.updateOrientationAngles()

                            this@CameraActivity.runOnUiThread(Runnable {
                                this.viewBinding.imageView.setImageBitmap(boundedBitmap)
                            })

                        }

                        image.close()
                    })
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector,imageAnalysis,videoCapture,preview)
            } catch(exc: Exception) {
                Log.e(CameraActivity.TAG, "Use case binding failed", exc)
            }

            viewBinding.button.setOnClickListener {
                isPhotoMode = !isPhotoMode
                if (isPhotoMode) {
                    viewBinding.videoCaptureButton.setText(R.string.take_photo)
                    viewBinding.button.setText("video")
                    try {
                        cameraProvider.unbind(videoCapture)
                        cameraProvider.bindToLifecycle(this, cameraSelector,imageAnalysis,imageCapture,preview)
                    } catch(exc: Exception) {
                        Log.e(CameraActivity.TAG, "Use case binding failed", exc)
                    }

                } else{
                    viewBinding.videoCaptureButton.setText(R.string.start_capture)
                    viewBinding.button.setText("photo")
                    try {
                        cameraProvider.unbind(imageCapture)
                        cameraProvider.bindToLifecycle(this, cameraSelector,imageAnalysis,videoCapture,preview)
                    } catch(exc: Exception) {
                        Log.e(CameraActivity.TAG, "Use case binding failed", exc)
                    }

                }
            }

        }, ContextCompat.getMainExecutor(this))
    }



    fun videoCaptureButton(uri:Uri, name:String, extension:String, pathToSave:String){

        CloudConnection(applicationContext).upload(uri,pathToSave,name+"."+extension)

        data2Save?.let{
            FirebaseDataBase(this).writeNewMediaData(MediaData(extension,it),name)
        }
        data2Save=null


    }

    fun onRecordingProcess(resolution:Size) {


        val boundingbox=BoundingBox(objectsDetection.getResults(), applicationContext).also {

            if(objectsDetection.getRotationDegrees()!=90)
                it.setTargetImageResolution(resolution)
            else
                it.setTargetImageResolution(Size(resolution.height,resolution.width))



            it.setScale(SizeF(
                it.getTargetImageResolution()!!.width.toFloat()/objectsDetection.getTargetImageResolution()!!.width,
                it.getTargetImageResolution()!!.height.toFloat()/objectsDetection.getTargetImageResolution()!!.height))
            if(it.getScale().width!=it.getScale().height) {
                val add_y =
                    -(objectsDetection.getTargetImageResolution()!!.height - objectsDetection.getTargetImageResolution()!!.height) / 2
                val add_x =
                    -(objectsDetection.getTargetImageResolution()!!.height - objectsDetection.getTargetImageResolution()!!.width) / 2
                it.moveResultBoundBox(add_x.toFloat(), add_y.toFloat())
            }
            if(it.getScale().width>it.getScale().height)
                it.scaleResultBoundBox(it.getScale().width)
            else
                it.scaleResultBoundBox(it.getScale().height)

        }

        locationService.getLastLocation()
            .addOnSuccessListener { location: Location ->
                var listOfDetectedObject: ArrayList<DetectedObject>?=null
                val listOfDeviceLocation = ListOfDeviceLocation(
                    location.altitude,
                    location.latitude,
                    location.longitude,
                    location.accuracy.toDouble()
                )

                val listOfDeviceRotation = ListOfDeviceRotation(
                    sensorService.getSensorResult()[0],
                    sensorService.getSensorResult()[1],
                    sensorService.getSensorResult()[2]
                )

                for (i in 0 until objectsDetection.getCountOfResults()) {
                    val result=boundingbox.getBoundingBox(i)

                    result?.let {
                        var sign_size = 600f
                        if (result.categories[0].index == 1)
                            sign_size = 800f
                        if (result.categories[0].index == 2)
                            sign_size = 600f
                        if (result.categories[0].index == 3)
                            sign_size = 600f

                        val dist = CameraCalibration(cameraManager).calculateDistanseFromObject(
                            sign_size,
                            result.boundingBox.bottom - result.boundingBox.top,
                            boundingbox.getTargetImageResolution()!!.height
                        )

                        val scale = CameraCalibration(cameraManager).calculateScale(
                            sign_size,
                            result.boundingBox.bottom - result.boundingBox.top
                        )

                        val angle = CameraCalibration(cameraManager).calculateAngleFromCameraCenter(
                            Size(
                                boundingbox.getTargetImageResolution()!!.width,
                                boundingbox.getTargetImageResolution()!!.height
                                 ),
                            result.boundingBox,
                            dist,
                            scale
                        )

                        if(listOfDetectedObject==null)
                            listOfDetectedObject = arrayListOf(
                                DetectedObject(
                                    "${result.categories[0].label}",
                                    "${result.categories[0].index}",
                                    result.categories[0].score,
                                    dist,
                                    angle[0],
                                    angle[1],
                                    sign_size,
                                    boundingbox.getBoundingBox(i)!!.boundingBox
                                )
                            )
                        else
                            listOfDetectedObject!!.add(
                                DetectedObject(
                                    "${result.categories[0].label}",
                                    "${result.categories[0].index}",
                                    result.categories[0].score,
                                    dist,
                                    angle[0],
                                    angle[1],
                                    sign_size,
                                    boundingbox.getBoundingBox(i)!!.boundingBox
                                )
                            )
                    }
                }

                if(data2Save==null){
                    data2Save= arrayListOf(ListOfDetectedObjects(
                        listOfDetectedObject,
                        listOfDeviceLocation,
                        listOfDeviceRotation))
                }
                else
                    data2Save!!.add(ListOfDetectedObjects(
                        listOfDetectedObject,
                        listOfDeviceLocation,
                        listOfDeviceRotation
                    ))
            }
    }

    override fun onResume() {
        locationService.requestLocationUpdates()
        super.onResume()
    }
    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    companion object {
        private const val TAG = "ObjectDetectionApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION

            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }


}