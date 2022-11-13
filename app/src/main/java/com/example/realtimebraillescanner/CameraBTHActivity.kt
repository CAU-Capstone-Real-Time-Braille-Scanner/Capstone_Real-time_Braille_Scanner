package com.example.realtimebraillescanner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class CameraBTHActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_bth_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container_camera_activity2, CameraBTHFragment.newInstance())
                .commitNow()
        }
    }
}