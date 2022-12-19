package com.example.camerax

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DataSave(private val context: Context) {
    private lateinit var file:File
    private  var uri: Uri?=null
    val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    private var name:String
    private var dir:File

    init {
        val filepath=context.getExternalFilesDir(null)
        dir= File(filepath!!.absolutePath+"/annotations/")
        dir.mkdir()
        //Create a new file that points to the root directory, with the given name:
        name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        //file= File(dir,name+".txt")
    }

    fun getNameFile():String{
        return name
    }

    fun writeFileExternalStorage(textToWrite:String, filename:String){
        file=File(dir,filename+".txt")
        uri=Uri.fromFile(file)
        //Log.d("Uri_d", "${uri}")
        try {
            val outputStream: FileOutputStream = FileOutputStream(file,true)
            outputStream.write(textToWrite.toByteArray())
            outputStream.flush()
            outputStream.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun saveBitmapAsJPEG(bitmap: Bitmap,filepath:String){
        val filePath: File? = context.getExternalFilesDir(null)
        val dir:File=File(filePath!!.absolutePath+filepath)
        dir.mkdir();
        val file:File=File(dir,name+".jpg")
        uri=Uri.fromFile(file)

        try{
            val outputStream: FileOutputStream =FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream)
            //Toast.makeText(context.applicationContext,"Image is Saved on ${file.absoluteFile}",Toast.LENGTH_LONG).show()
            outputStream.flush()
            outputStream.close()
        }catch (e: FileNotFoundException){
            e.printStackTrace()
        }
    }

    fun getUri():Uri?{
        return this.uri
    }

    fun readFile(filename: String) {
        file=File(dir, filename + ".txt")
        try {
            val inputStream = FileInputStream(file)
            val byteArray = ByteArray(file.length().toInt())
            var text = inputStream.read(byteArray)
            inputStream.close()
        }catch (e:FileNotFoundException){
            e.printStackTrace()
        }
    }
}