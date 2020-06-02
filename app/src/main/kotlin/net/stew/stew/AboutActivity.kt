package net.stew.stew

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_about.*

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_about)

        versionTextView.text = getString(R.string.version, BuildConfig.VERSION_NAME)

        logOutButton.setOnClickListener { (application as Application).logOut() }
    }

}