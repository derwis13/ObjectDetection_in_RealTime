package com.example.camerax

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.example.camerax.databinding.FragmentSettingsBinding
import java.util.*

class SettingsFragment : Fragment(), AdapterView.OnItemSelectedListener {
    private lateinit var binding: FragmentSettingsBinding
    private val sensorList= arrayListOf<String>("en","pl","de")
    private lateinit var selectSensor:String


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {

        FragmentSettingsBinding.inflate(layoutInflater,container,false).also { binding=it}

        setDefaultItemOnSpinner()

       val spinnerLanguages: Spinner = binding.languageSpinner
       val adapter = ArrayAdapter(
           this.requireContext(),
           android.R.layout.simple_spinner_item,
           sensorList
           )
           .also {
               it.setDropDownViewResource(android.R.layout.simple_spinner_item)
               spinnerLanguages.adapter=it
           }
        spinnerLanguages.onItemSelectedListener=this

        binding.saveSettingButton.setOnClickListener {
            val intent = Intent(activity, CameraActivity::class.java)
            intent.putExtra("name", floatArrayOf(
                binding.scoreThresholdValue.text.toString().toFloat(),
                binding.maxDetectionValue.text.toString().toFloat(),
                binding.strokeWidthValue.text.toString().toFloat(),
                binding.resultTextSizeValue.text.toString().toFloat()))
            startActivity(intent)
            it.findNavController()
                .navigate(R.id.action_settingFragment_to_titleFragment)
            }

        return binding.root
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        Log.d("choose_item","${parent.getItemAtPosition(pos)}")
        var selected:String=parent.getItemAtPosition(pos).toString()
        setLocal(selected)
    }
    fun setDefaultItemOnSpinner() {
        val language=resources.configuration.locales[0].language
        selectSensor=language
        sensorList.remove(language)
        sensorList.add(0,language)
    }
    override fun onNothingSelected(p0: AdapterView<*>?) {
    }
    fun setLocal(langCode:String){
        val locale:Locale= Locale(langCode)
        val config:Configuration=resources.configuration.also { it.setLocale(locale) }
        resources.updateConfiguration(config,resources.displayMetrics)
        onConfigurationChanged(config)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.scoreThresholdTextView.setText(R.string.scoreThreshold)
        binding.strokeWidthTextView.setText(R.string.bndboxStrokeWidth)
        binding.maxDetectionTextView.setText(R.string.maxDetecionsResult)
        binding.resultTextSizeTextView.setText(R.string.resultsTextSize)
        binding.languageTextView.setText(R.string.language)
        binding.saveSettingButton.setText(R.string.saveSettingButtonValue)
    }


}


