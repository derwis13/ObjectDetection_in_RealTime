package com.example.camerax

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.camerax.databinding.FragmentTitleBinding

class TitleFragment : Fragment() {
    private lateinit var binding: FragmentTitleBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        FragmentTitleBinding.inflate(layoutInflater).also { binding=it}
        binding.startCameraButton.setOnClickListener {
            val intent = Intent(this.requireContext(), CameraActivity::class.java)
            intent.putExtra("name", floatArrayOf(0.2f,30f,10f,50f))
            startActivity(intent)
        }
        binding.settingsButton.setOnClickListener{
                view:View ->view.findNavController()
            .navigate(R.id.action_titleFragment_to_settingsFragment)}
        binding.startMapButton.setOnClickListener{
                view:View ->view.findNavController()
            .navigate(R.id.action_titleFragment_to_mapsActivity)}

        return binding.root
    }
}