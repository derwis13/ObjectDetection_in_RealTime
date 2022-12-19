package com.example.camerax

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
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
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
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
import androidx.core.graphics.scale
import com.example.camerax.databinding.CameraLayoutBinding
import com.example.objectdetectionapp.ObjectsDetection
import org.tensorflow.lite.support.image.TensorImage
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class CameraActivity(): AppCompatActivity() {
    private lateinit var objectsDetection: ObjectsDetection
    private lateinit var viewBinding: CameraLayoutBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var photoUri:Uri?=null

    private var dataToSave:ArrayList<String>?=null
    private var counterFrameOnRecording:Int=0

    private var displaySize:Size= Size(0,0)

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private var name:String=""


    private lateinit var locationService:LocationService
    private lateinit var cameraManager: CameraManager
    private lateinit var sensorManager:SensorManager

    private lateinit var sensorService:SensorService



    fun draw():Canvas{

        val canvas=Canvas()
        val paint = Paint()

        paint.setStyle(Paint.Style.STROKE)
        paint.setColor(Color.RED)
        paint.setStrokeWidth(10f)

        //center

        //center
        val x0 = canvas.width / 2
        val y0 = canvas.height / 2
        val dx = canvas.height / 3
        val dy = canvas.height / 3

        canvas.drawRect((x0-dx).toFloat(), (y0-dy).toFloat(), (x0+dx).toFloat(),
            (y0+dy).toFloat(), paint)

        return canvas
    }
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
        sensorService=SensorService(sensorManager,this.lifecycle)


        if(savedInstanceState==null){
            //CloudConnection(applicationContext).download("annotation/","annotations_images.txt","/annotations/")
        }

        intent.getFloatArrayExtra("name")?.let {
            objectsDetection= ObjectsDetection(
                this,
                it.component1(),
                it.component2().toInt(),
                it.component3(),
                it.component4()
            )
        }

        if (allPermissionsGranted()) {
            locationService.requestLocationUpdates()
            startCamera(objectsDetection)

        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        viewBinding.imageCaptureButton.setOnClickListener { takePhoto()
        }
        viewBinding.videoCaptureButton.setOnClickListener {
            captureVideo()
//            dataToSave?.let {
//                it.forEach {
//                    Log.d("recorded_data","${it}")
//                }
//            }


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
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
                                .show()
                            Log.d(TAG, msg)
                            videoCaptureButton(recordEvent.outputResults.outputUri)
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

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    photoUri=output.savedUri
                    photoUri?.let {
                        imageCaptureButton(it)
                    }
                }
            }
        )

    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera(objectsDetection: ObjectsDetection) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST, FallbackStrategy.higherQualityOrLowerThan(
                    Quality.SD)))
                .build()

            imageCapture = ImageCapture.Builder().build()
            videoCapture = VideoCapture.withOutput(recorder)


            val imageAnalysis = ImageAnalysis.Builder()
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it ->
                    it.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->


                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                        //detect object on image
                        imageProxy.image?.let { image->
                            var bitmap=toBitmap(image).let { bitmap->
                                rotateBitmap(bitmap,rotationDegrees.toFloat())//.scale(viewBinding.viewFinder.width,viewBinding.viewFinder.height)
                            }

                            try {
                                TensorImage.fromBitmap(bitmap).also {
                                    objectsDetection.runObjectDetection(it)
                                }
                            }catch(exc: Exception){
                                Log.e("ObjectDetection","Fail detect objects on image",exc)
                            }



                            if(rotationDegrees!=90)
                                displaySize=Size(viewBinding.viewFinder.height,viewBinding.viewFinder.width)
                            else
                                displaySize=Size(viewBinding.viewFinder.width,viewBinding.viewFinder.height)

                            val add_x=-(bitmap.height-image.height)/2
                            val add_y=-(bitmap.width-image.height)/2
                            Log.d("value","${image.height} ${image.width}" +
                                    "${bitmap.height} ${bitmap.width} ${viewBinding.viewFinder.width} ${viewBinding.viewFinder.height}")


                            objectsDetection.moveResultBoundBox(add_x.toFloat(),add_y.toFloat())

                            var scale: Float
                            if(viewBinding.viewFinder.height>viewBinding.viewFinder.width)
                                scale=viewBinding.viewFinder.height.toFloat() / image.width
                            else
                                scale=viewBinding.viewFinder.width.toFloat() / image.width


                            objectsDetection.scaleResultBoundBox(scale)

                            val boundedBitmap=objectsDetection.drawBoundingBoxWithText(Size(viewBinding.viewFinder.width,viewBinding.viewFinder.height))


                            if(recording!=null)
                            {
                                counterFrameOnRecording++
                                collectData()
                            }

                            //on Click button
                            //viewBinding.imageCaptureButton.setOnClickListener { imageCaptureButton(boundedBitmap,bitmap)}

                            sensorService.updateOrientationAngles()



                            this@CameraActivity.runOnUiThread(Runnable {
                                this.viewBinding.imageView.setImageBitmap(boundedBitmap)
                            })
                            }
                        imageProxy.close()
                    })
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector,imageAnalysis,imageCapture,preview)
            } catch(exc: Exception) {
                Log.e(CameraActivity.TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
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


    @SuppressLint("MissingPermission")
    fun imageCaptureButton(boundedBitmap: Bitmap,bitmap: Bitmap){

        val dataSave= DataSave(applicationContext)

        annotationsUploadProcess(bitmap,dataSave)

        //upload bounded image
        imageUploadProcess(boundedBitmap,dataSave,"/bounded_images/","bounded_images/")

        //upload image
        imageUploadProcess(bitmap,dataSave,"/saved_images/","images/")

    }
    fun imageCaptureButton(uri:Uri){

        val dataSave= DataSave(applicationContext)

        annotationsUploadProcess(uri,dataSave)

        //upload image
        imageUploadProcess(uri,dataSave,"images/")

    }
    fun videoCaptureButton(uri:Uri){
        val dataSave= DataSave(applicationContext)
        var text=""
        dataToSave?.let {
            it.forEach {
                text+=it
                }
            }
        dataSave.writeFileExternalStorage(text,"annotations_videos")
        dataSave.getUri()?.let {
            CloudConnection(applicationContext).upload(it,"annotation/","annotations_videos.txt")
        }
        imageUploadProcess(uri,dataSave,"videos/")

            //upload image
            //imageUploadProcess(uri,dataSave,"images/")

    }

    fun collectData(){
        val dataSave= DataSave(applicationContext)
        locationService.getLastLocation()
            .addOnSuccessListener { location: Location ->
                for (i in 0 until objectsDetection.getCountOfResults()) {
                    val result=objectsDetection.getResult(i)
                    var sing_size=600f
                    if(result.categories[0].index==1)
                        sing_size=800f
                    if (result.categories[0].index==2)
                        sing_size=600f
                    if (result.categories[0].index==3)
                        sing_size=800f

                    val dist=CameraCalibration(cameraManager).calculateDistanseFromObject(
                        sing_size,
                        result.boundingBox.bottom-result.boundingBox.top,
                        displaySize.height)

                    val scale=CameraCalibration(cameraManager).calculateScale(
                        sing_size,
                        result.boundingBox.bottom-result.boundingBox.top
                    )

                    val angle=CameraCalibration(cameraManager).calculateAngleFromCameraCenter(
                        Size(displaySize.width,displaySize.height),
                        result.boundingBox,dist,scale)

                    //Toast.makeText(applicationContext,"${angle[0]}, ${angle[1]}, $dist",Toast.LENGTH_SHORT).show()


                    if(dataToSave==null){
                        dataToSave= arrayListOf("${name}.mp4;"+
                                "$counterFrameOnRecording;"+
                                "${result.boundingBox.left};"+
                                "${result.boundingBox.top};"+
                                "${result.boundingBox.right};"+
                                "${result.boundingBox.bottom};"+
                                "${result.categories.first().index};"+
                                "${result.categories.first().score};"+
                                "${location.latitude};"+
                                "${location.longitude};"+
                                "${location.altitude};"+
                                "${sensorService.getSensorResult()[0]};"+
                                "${sensorService.getSensorResult()[1]};"+
                                "${sensorService.getSensorResult()[2]};"+
                                "${dist};"+
                                "${angle[0]};"+
                                "${angle[1]}\n")
                    }
                    else{
                        dataToSave!!.add("${name}.mp4;"+
                                "$counterFrameOnRecording;"+
                                "${result.boundingBox.left};"+
                                "${result.boundingBox.top};"+
                                "${result.boundingBox.right};"+
                                "${result.boundingBox.bottom};"+
                                "${result.categories.first().index};"+
                                "${result.categories.first().score};"+
                                "${location.latitude};"+
                                "${location.longitude};"+
                                "${location.altitude};"+
                                "${sensorService.getSensorResult()[0]};"+
                                "${sensorService.getSensorResult()[1]};"+
                                "${sensorService.getSensorResult()[2]};"+
                                "${dist};"+
                                "${angle[0]};"+
                                "${angle[1]}\n")
                    }
                }
            }

    }

    fun annotationsUploadProcess(bitmap: Bitmap, dataSave: DataSave){

        //get location of device with 0.01m precision [7 decimal places]
        locationService.getLastLocation()
            .addOnSuccessListener { location: Location ->

                for (i in 0 until objectsDetection.getCountOfResults()) {
                    val result=objectsDetection.getResult(i)

                    val dist=CameraCalibration(cameraManager).calculateDistanseFromObject(
                        265f,
                        result.boundingBox.bottom-result.boundingBox.top,
                        bitmap.height)

                    val scale=CameraCalibration(cameraManager).calculateScale(
                        265f,
                        result.boundingBox.bottom-result.boundingBox.top
                    )


                    val angle=CameraCalibration(cameraManager).calculateAngleFromCameraCenter(
                        Size(bitmap.width,bitmap.height),
                        result.boundingBox,dist,scale)

                    //Toast.makeText(applicationContext,"${angle[0]}, ${angle[1]}, $dist",Toast.LENGTH_SHORT).show()


                    dataSave.writeFileExternalStorage(
                        "${dataSave.getNameFile()}.jpg;"+
                                "${result.boundingBox.left};"+
                                "${result.boundingBox.top};"+
                                "${result.boundingBox.right};"+
                                "${result.boundingBox.bottom};"+
                                "${result.categories.first().index};"+
                                "${result.categories.first().score};"+
                                "${location.latitude};"+
                                "${location.longitude};"+
                                "${location.altitude};"+
                                "${sensorService.getSensorResult()[0]};"+
                                "${sensorService.getSensorResult()[1]};"+
                                "${sensorService.getSensorResult()[2]};"+
                                "${dist};"+
                                "${angle[0]};"+
                                "${angle[1]}\n",
                        "annotations_images")
                }
                //upload annotation
                dataSave.getUri()?.let {
                    CloudConnection(applicationContext).upload(it,"annotation/","annotations_images.txt")
                }
            }

    }
    fun annotationsUploadProcess(uri:Uri, dataSave: DataSave){

        //get location of device with 0.01m precision [7 decimal places]
        locationService.getLastLocation()
            .addOnSuccessListener { location: Location ->

                for (i in 0 until objectsDetection.getCountOfResults()) {
                    val result=objectsDetection.getResult(i)

                    val dist=CameraCalibration(cameraManager).calculateDistanseFromObject(
                        265f,
                        result.boundingBox.bottom-result.boundingBox.top,
                        displaySize.height)

                    val scale=CameraCalibration(cameraManager).calculateScale(
                        265f,
                        result.boundingBox.bottom-result.boundingBox.top
                    )


                    val angle=CameraCalibration(cameraManager).calculateAngleFromCameraCenter(
                        Size(displaySize.width,displaySize.height),
                        result.boundingBox,dist,scale)

                    //Toast.makeText(applicationContext,"${angle[0]}, ${angle[1]}, $dist",Toast.LENGTH_SHORT).show()



                    dataSave.writeFileExternalStorage(
                        "${dataSave.getNameFile()}.jpg;"+
                                "${result.boundingBox.left};"+
                                "${result.boundingBox.top};"+
                                "${result.boundingBox.right};"+
                                "${result.boundingBox.bottom};"+
                                "${result.categories.first().index};"+
                                "${result.categories.first().score};"+
                                "${location.latitude};"+
                                "${location.longitude};"+
                                "${location.altitude};"+
                                "${sensorService.getSensorResult()[0]};"+
                                "${sensorService.getSensorResult()[1]};"+
                                "${sensorService.getSensorResult()[2]};"+
                                "${dist};"+
                                "${angle[0]};"+
                                "${angle[1]}\n",
                        "annotations_images")
                }
                //upload annotation
                dataSave.getUri()?.let {
                    CloudConnection(applicationContext).upload(it,"annotation/","annotations_images.txt")
                }
            }

    }
    fun imageUploadProcess(bitmap: Bitmap,dataSave: DataSave, devicePath:String, cloudPath:String){

        //save bitmap to device store
        dataSave.saveBitmapAsJPEG(bitmap,devicePath)

        //get uri saved bitmap on device
        dataSave.getUri()?.let {
            //upload bitmap to cloud
            CloudConnection(applicationContext).upload(
                it, cloudPath, dataSave.getNameFile())
        }
    }
    fun imageUploadProcess(uri:Uri,dataSave: DataSave,cloudPath:String){
        CloudConnection(applicationContext).upload(uri,cloudPath,dataSave.getNameFile())
    }

    override fun onResume() {
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