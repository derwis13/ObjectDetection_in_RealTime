//package com.example.camerax
//
//import android.graphics.Color
//import android.util.Log
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//
//class SettingsViewModel:ViewModel(){
//
//    //private var zmienna=MutableLiveData<Float>()
//    private val boundboxStrokeWidth = MutableLiveData<Float>()
//    val strokeWidth_:LiveData<Float> =boundboxStrokeWidth
//    private val textSize = MutableLiveData<Float>()
//    val textSize_:LiveData<Float> = textSize
//    private var maxResults = MutableLiveData<Float>()
//    val maxResults_:LiveData<Float> =maxResults
//    private var scoreThreshold = MutableLiveData<Float>()
//    val scoreThreshold_=scoreThreshold
//
//    fun getSettingsInfo(){
//        Log.d("ViewModelParametres","$boundboxStrokeWidth, $textSize, $maxResults, $scoreThreshold")
//    }
//    public fun setPreferences(bndboxWidth:Float,textSize:Float,maxResult:Float,Threshold:Float){
//        this.boundboxStrokeWidth.value=bndboxWidth
//        this.textSize.value=textSize
//        this.maxResults.value=maxResult
//        this.scoreThreshold.value=Threshold
//    }
//    fun getBoundboxStrokeWidth(): MutableLiveData<Float> {
//        return boundboxStrokeWidth
//    }
//}