package com.example.realtimebraillescanner

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.realtimebraillescanner.databinding.SplashActivityBinding
import java.util.UUID

class SplashActivity : AppCompatActivity() {
    private lateinit var binding : SplashActivityBinding
    private val pref: SharedPreferences by lazy {
        getSharedPreferences("UUID", Activity.MODE_PRIVATE)
    }
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SplashActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editor = pref.edit()

        val uniqueID = pref.getString("id", null)
        if (uniqueID == null) {
            val newID = UUID.randomUUID().toString()
            editor.putString("id", newID).apply()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
        }, 2000)
    }
}