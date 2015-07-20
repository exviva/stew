package net.keitto.keitto

import com.facebook.drawee.backends.pipeline.Fresco

class Application : android.app.Application() {

    override fun onCreate() {
        super.onCreate()

        Fresco.initialize(this)
    }

}