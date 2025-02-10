package com.intesc.controldegarantiasyensambles.main.view

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.intesc.controldegarantiasyensambles.databinding.ActivitySplashBinding
import com.intesc.controldegarantiasyensambles.main.view.search.SearchActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
            finish()
        }, 1600)
    }
}
