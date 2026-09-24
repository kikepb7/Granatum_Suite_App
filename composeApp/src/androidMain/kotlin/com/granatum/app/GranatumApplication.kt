package com.granatum.app

import android.app.Application
import com.granatum.app.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class GranatumApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@GranatumApplication)
            androidLogger()
        }
    }
}