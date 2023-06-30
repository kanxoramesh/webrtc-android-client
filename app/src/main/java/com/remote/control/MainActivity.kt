package com.remote.control


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.remote.control.databinding.ActivityMainBinding
import com.remote.remote.screenshare.RemoteScreenActivity

/**
 * MainScreen for remote trackify service .
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.remote.setOnClickListener {
            startRemoteActivity()
        }
    }

    fun startService() {
        val serviceIntent = Intent(this, ControlService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }


    fun startRemoteActivity() {
        startService()
        var intent = Intent(this, RemoteScreenActivity::class.java)
        startActivity(intent)
    }

}

