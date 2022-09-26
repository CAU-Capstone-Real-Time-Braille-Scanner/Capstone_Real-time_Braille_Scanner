package com.example.realtimebraillescanner

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.realtimebraillescanner.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initClickListener()

    }


    private fun initClickListener(){
        binding.tvConverterMain.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }
    }

}