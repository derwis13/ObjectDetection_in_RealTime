package com.example.camerax

import android.content.Context

import com.example.camerax.DataBase.*
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class FirebaseDataBase(private var context: Context) {
    private var database: DatabaseReference=Firebase.database.reference


    fun writeNewMediaData(arrayList: MediaData,name:String){
        database.child(name).setValue(arrayList)
    }

    fun readData(){
    database.get().addOnCompleteListener {
        if(it.result.exists()){
            var dataSnapshot=it.result
            dataSnapshot.children.forEach{
                it.child("frames").children.forEach {
                    it.child("detectedObjects").children.forEach{
                    }

                }
            }

        }
    }

    }
}