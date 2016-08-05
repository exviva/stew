package net.stew.stew

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import com.facebook.drawee.backends.pipeline.Fresco
import kotlinx.android.synthetic.main.activity_fullscreen_image.imageView

class FullscreenImageActivity() : Activity() {

    companion object {
        fun start(activity: Activity, uri: Uri, view: View) {
            val intent = Intent(activity, FullscreenImageActivity::class.java)
            intent.putExtra("uri", uri)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                val width = 0.5 * view.width
                val height = 0.5 * view.height
                val bundle = ActivityOptions.makeScaleUpAnimation(view, width.toInt(), height.toInt(), 0, 0).toBundle()
                activity.startActivity(intent, bundle)
            } else {
                activity.startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fullscreen_image)

        val uri: Uri = intent.getParcelableExtra("uri")
        val controller = Fresco.newDraweeControllerBuilder().
            setUri(uri).
            setAutoPlayAnimations(true).
            build()
        imageView.controller = controller
        imageView.setOnClickListener { finish() }
    }

}