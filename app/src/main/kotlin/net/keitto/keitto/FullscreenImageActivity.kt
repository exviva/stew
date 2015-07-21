package net.keitto.keitto

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.facebook.drawee.backends.pipeline.Fresco
import kotlinx.android.synthetic.activity_fullscreen_image.imageView

class FullscreenImageActivity() : Activity() {

    companion object {
        fun start(activity: Activity, uri: Uri) {
            val intent = Intent(activity, javaClass<FullscreenImageActivity>())
            intent.putExtra("uri", uri)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fullscreen_image)

        val uri: Uri = getIntent().getParcelableExtra("uri")
        val controller = Fresco.newDraweeControllerBuilder().
            setUri(uri).
            setAutoPlayAnimations(true).
            build()
        imageView.setController(controller)
    }

}