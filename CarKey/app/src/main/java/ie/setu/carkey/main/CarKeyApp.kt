package ie.setu.carkey.main

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ie.setu.carkey.service.BleManager
import timber.log.Timber

@HiltAndroidApp
class CarKeyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        BleManager.initialize(this)
        Timber.i("CarKey Application started")
    }
}
