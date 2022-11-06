package com.example.camerax

import android.content.Intent
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

class SettingsFragment : Fragment(), AdapterView.OnItemSelectedListener {
    private lateinit var binding: FragmentSettingsBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {

        FragmentSettingsBinding.inflate(layoutInflater,container,false).also { binding=it}

       val spinnerLanguages: Spinner = binding.languageSpinner
       val adapter = ArrayAdapter.createFromResource(
           this.requireContext(),
           com.example.camerax.R.array.language_spinner,
           android.R.layout.simple_spinner_item)
           .also {
               it.setDropDownViewResource(android.R.layout.simple_spinner_item)
               spinnerLanguages.adapter=it
           }
        spinnerLanguages.onItemSelectedListener=this

        binding.saveSettingButton.setOnClickListener {
            Log.d("sss","${binding.resultTextSizeValue.text.toString().toFloat()}")
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
    }
    override fun onNothingSelected(p0: AdapterView<*>?) {
    }
}


