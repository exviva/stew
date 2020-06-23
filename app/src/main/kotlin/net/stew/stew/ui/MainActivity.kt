package net.stew.stew.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import net.stew.stew.Application
import net.stew.stew.R
import net.stew.stew.databinding.ActivityMainBinding
import net.stew.stew.model.PostCollection

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private val postsFragments = mutableMapOf<PostCollection, PostsFragment>()
    private val aboutFragment = AboutFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!(application as Application).requireCurrentSession()) {
            finish()
        }

        ActivityMainBinding.inflate(layoutInflater).apply {
            navigationView.setOnNavigationItemSelectedListener(this@MainActivity)
            setContentView(root)
        }

        setActivePostsFragment(PostCollection.Friends)
    }

    fun setActivePostsFragment(collection: PostCollection) {
        val fragment =
                if (collection.isPredefined()) postsFragments.getOrPut(collection) {
                    PostsFragment.newInstance(collection)
                }
                else PostsFragment.newInstance(collection)

        setActiveFragment(fragment)
    }

    private fun setActiveFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().run {
            replace(R.id.fragmentContainer, fragment)

            if (fragment != aboutFragment && !postsFragments.containsValue(fragment)) {
                addToBackStack(null)
            }

            commit()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        supportFragmentManager.run {
            repeat(backStackEntryCount) { popBackStack() }
        }

        when (item.itemId) {
            R.id.friends -> setActivePostsFragment(PostCollection.Friends)
            R.id.fof -> setActivePostsFragment(PostCollection.FOF)
            R.id.everyone -> setActivePostsFragment(PostCollection.Everyone)
            R.id.me -> setActivePostsFragment(PostCollection.Me)
            R.id.about -> setActiveFragment(aboutFragment)
        }

        return true
    }

}