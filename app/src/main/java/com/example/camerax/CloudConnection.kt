package com.example.camerax

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class CloudConnection(private var context:Context) {

    private var storageReference=FirebaseStorage.getInstance().reference

    fun download(cloudPath:String, filename:String, destinationDirectory:String): Task<Uri> {
        val ref=storageReference.child(cloudPath+filename)

        ref.downloadUrl.addOnSuccessListener{
            downloadFile(filename,
                destinationDirectory,it.toString())
            Toast.makeText(context,"SUCCESS DOWNLOAD",Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {

        }
        return ref.downloadUrl
        }
    private fun downloadFile(filename:String, destinationDirectory:String, url:String){
        val downloadManager:DownloadManager=context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri:Uri=Uri.parse(url)
        val request:DownloadManager.Request=DownloadManager.Request(uri)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        val file=File(context.getExternalFilesDir(null)!!.absolutePath+destinationDirectory,filename)
        if(file.exists())
            file.delete()
        request.setDestinationInExternalFilesDir(context, destinationDirectory,filename)
        downloadManager.enqueue(request)
    }
    fun upload(uri: Uri, cloudPath:String, filename:String)= CoroutineScope(Dispatchers.IO).launch{

        val mountainsRef = storageReference.child(cloudPath+filename)
        mountainsRef.putFile(uri)
            .addOnCompleteListener {
                Toast.makeText(context,"Upload Completed", Toast.LENGTH_SHORT).show()
            }
    }


}