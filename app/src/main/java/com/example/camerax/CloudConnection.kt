package com.example.camerax

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.telephony.mbms.DownloadRequest
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.*

class CloudConnection(context: Context) {
    //var firebaseStorage= Firebase.storage.reference
    private lateinit var storageReference: StorageReference
    private lateinit var firebaseStorage: FirebaseStorage
    private var context=context

    var gsReference=Firebase.storage.getReferenceFromUrl("gs://objectdetection-in-realtime.appspot.com/images/star.png")
    val ONE_MEGABYTE: Long=1024*1024
//    fun dosomethings(){
//        gsReference.getBytes(ONE_MEGABYTE).addOnSuccessListener {
//            Log.d("download_from_database","SUCCESS")
//        }
//            .addOnFailureListener { Log.d("download_from_database","FAIL") }
//    }
    fun download() {
        storageReference=FirebaseStorage.getInstance().reference
        val ref=storageReference.child("star.png")
//        ref.downloadUrl.addOnSuccessListener(OnSuccessListener {
//            fun onSuccess(uri: Uri){
//                downloadFiles()
//            }
//        }).addOnFailureListener(OnFailureListener {
//            fun onFailure(e:Exception){
//
//            }
//        })
        ref.downloadUrl.addOnSuccessListener{
            downloadfiles(context,"star",".png",DIRECTORY_DOWNLOADS,it.toString())
        }.addOnFailureListener {

        }

        }
    fun downloadfiles(context: Context, filename:String, fileExtension:String, destinationDirectory:String, url:String){
        val downloadManager:DownloadManager=context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri:Uri=Uri.parse(url)
        val request:DownloadManager.Request=DownloadManager.Request(uri)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalFilesDir(context, destinationDirectory,filename+fileExtension)
        downloadManager.enqueue(request)
    }
    fun upload(imageUri: Uri, cloudPath:String)= CoroutineScope(Dispatchers.IO).launch{

        val storageReference=FirebaseStorage.getInstance().reference
        val randomKey= UUID.randomUUID().toString()
        val mountainsRef = storageReference.child(cloudPath+randomKey)
        mountainsRef.putFile(imageUri).addOnSuccessListener {
            //pd.dismiss()
        }.addOnFailureListener {
            //pd.dismiss()
        }
            .addOnProgressListener {
                val progressProcent=(100*it.bytesTransferred/it.totalByteCount)
                //pd.setMessage("Progress: "+progressProcent.toInt() + "%")

            }
            .addOnCompleteListener {
                Toast.makeText(context,"Upload Completed", Toast.LENGTH_LONG).show()
            }

    }


    //val mountainref=storage.child("images/star-883x900.png")

    //Log.d("storage_","${mountainref.path}")
    //mountainref.getStream()
}