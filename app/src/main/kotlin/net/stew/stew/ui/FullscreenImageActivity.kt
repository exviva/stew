package net.stew.stew.ui

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.facebook.drawee.backends.pipeline.Fresco
import net.stew.stew.databinding.ActivityFullscreenImageBinding

class FullscreenImageActivity : Activity() {

    companion object {
        fun start(activity: Activity, uri: Uri, view: View) {
            val intent = Intent(activity, FullscreenImageActivity::class.java).apply {
                putExtra("uri", uri)
            }
            val width = 0.5 * view.width
            val height = 0.5 * view.height
            val bundle = ActivityOptions.makeScaleUpAnimation(view, width.toInt(), height.toInt(), 0, 0).toBundle()

            activity.startActivity(intent, bundle)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityFullscreenImageBinding.inflate(layoutInflater).apply {
            imageView.apply {
                controller = Fresco
                        .newDraweeControllerBuilder()
                        .setUri(intent.getParcelableExtra<Uri>("uri"))
                        .setAutoPlayAnimations(true)
                        .build()
                setOnClickListener { finishAfterTransition() }

            }
            setContentView(root)
        }
    }

}