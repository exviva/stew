package net.stew.stew.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.stew.stew.Application
import net.stew.stew.BuildConfig
import net.stew.stew.R
import net.stew.stew.databinding.FragmentAboutBinding

class AboutFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            FragmentAboutBinding.inflate(inflater, container, false).apply {
                versionTextView.text = getString(R.string.version, BuildConfig.VERSION_NAME)
                logOutButton.setOnClickListener { (requireActivity().application as Application).logOut() }
            }.root

}