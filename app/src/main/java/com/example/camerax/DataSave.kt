package com.example.camerax

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DataSave(context: Context) {
    private lateinit var file:File
    private  var uri: Uri?=null
    val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    private val context=context
    private lateinit var name:String

    init {
        val filepath=context.getExternalFilesDir(null)
        val dir: File = File(filepath!!.absolutePath+"/annotations/")
        dir.mkdir()
        //Create a new file that points to the root directory, with the given name:
        name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        file= File(dir,name+".txt")
    }

    fun getNameFile():String{
        return name
    }
    fun writeFileExternalStorage(textToWrite:String) {

        try {
            val outputStream: FileOutputStream = FileOutputStream(file,true)
            //file.createNewFile()
            //Log.d("error_save","$outputStream")
            outputStream.write(textToWrite.toByteArray())
            //Toast.makeText(context,"Image is saved", Toast.LENGTH_SHORT).show()
            //second argument of FileOutputStream constructor indicates whether
            //to append or create new file if one exists
            //outputStream = FileOutputStream(file, true)
            //outputStream!!.write(textToWrite.toByteArray())
            outputStream.flush()
            outputStream.close()
            Log.d("isFile","${outputStream}")
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
    fun saveBitmapAsJPEG(bitmap: Bitmap){
        val filePath: File? = context.getExternalFilesDir(null)
        val dir:File=File(filePath!!.absolutePath+"/savedImage/")
        dir.mkdir();
        val file:File=File(dir,name+".jpg")
        uri=Uri.fromFile(file)

        try{
            val outputStream: FileOutputStream =FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream)
            Toast.makeText(context.applicationContext,"Image is Saved on ${file.absoluteFile}",Toast.LENGTH_LONG).show()
            outputStream.flush()
            outputStream.close()
        }catch (e: FileNotFoundException){
            e.printStackTrace()
        }
    }
    fun getUri():Uri?{
        return this.uri
    }
}