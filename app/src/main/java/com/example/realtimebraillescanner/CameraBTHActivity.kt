package com.example.realtimebraillescanner

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class CameraBTHActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_bth_activity)

        val hashValue = getSharedPreferences("UUID", Activity.MODE_PRIVATE)
            .getString("id", "uniqueIDNotFound")
            .hashCode()
            .toString()

        if (savedInstanceState == null) {
            val bundle = Bundle().apply {
                putString("hashValue", hashValue)
            }
            val fragment = CameraBTHFragment.newInstance().apply {
                arguments = bundle
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.container_camera_activity2, fragment)
                .commitNow()
        }
    }
}