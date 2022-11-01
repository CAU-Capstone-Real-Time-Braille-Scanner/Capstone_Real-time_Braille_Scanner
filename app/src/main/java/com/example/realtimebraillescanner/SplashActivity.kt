package com.example.realtimebraillescanner

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.example.realtimebraillescanner.databinding.SplashActivityBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding : SplashActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Handler().postDelayed({
          startActivity(Intent(this, MainActivity::class.java))
        }, 2000)
    }

}