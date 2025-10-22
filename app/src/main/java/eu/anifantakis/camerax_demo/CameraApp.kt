package eu.anifantakis.camerax_demo

import android.app.Application
import eu.anifantakis.camerax_demo.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CameraApp: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CameraApp)
            modules(appModule)
        }
    }
}