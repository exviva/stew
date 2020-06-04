package net.stew.stew.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_about.*
import net.stew.stew.Application
import net.stew.stew.BuildConfig
import net.stew.stew.R

class AboutFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        versionTextView.text = getString(R.string.version, BuildConfig.VERSION_NAME)
        logOutButton.setOnClickListener { (activity!!.application as Application).logOut() }
    }
}