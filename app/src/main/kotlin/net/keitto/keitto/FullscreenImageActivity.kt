package net.keitto.keitto

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.facebook.drawee.backends.pipeline.Fresco
import kotlinx.android.synthetic.activity_fullscreen_image.imageView

class FullscreenImageActivity() : Activity() {

    companion object {
        fun start(activity: Activity, uri: Uri, view: View) {
            val intent = Intent(activity, javaClass<FullscreenImageActivity>())
            intent.putExtra("uri", uri)
            val width = 0.5 * view.getWidth()
            val height = 0.5 * view.getHeight()
            val bundle = ActivityOptions.makeScaleUpAnimation(view, width.toInt(), height.toInt(), 0, 0).toBundle()
            activity.startActivity(intent, bundle)
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