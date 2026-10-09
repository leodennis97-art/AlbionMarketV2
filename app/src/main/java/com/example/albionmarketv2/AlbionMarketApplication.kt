package com.example.albionmarketv2

import android.app.Application
import android.util.Log
import org.opencv.android.OpenCVLoader

class AlbionMarketApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (OpenCVLoader.initDebug()) {
            Log.d("AlbionMarketApp", "OpenCV loaded successfully")
        } else {
            Log.e("AlbionMarketApp", "OpenCV initialization failed")
        }
    }
}
