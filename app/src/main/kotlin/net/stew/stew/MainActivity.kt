package net.stew.stew

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.activity_main.drawerLayout
import kotlinx.android.synthetic.activity_main.drawerListView
import kotlinx.android.synthetic.activity_main.loadingIndicator
import kotlinx.android.synthetic.activity_main.postsView
import java.util.HashMap

class MainActivity : AppCompatActivity() {

    val ACTIVE_PREDEFINED_POST_COLLECTION_TAG = "ACTIVE_PREDEFINED_POST_COLLECTION_TAG"
    val ACTIVE_SUBDOMAIN_POST_COLLECTION_TAG = "ACTIVE_SUBDOMAIN_POST_COLLECTION_TAG"
    val LAST_VIEWED_PREDEFINED_POST_COLLECTION_TAG = "LAST_VIEWED_PREDEFINED_POST_COLLECTION_TAG"

    var drawerToggle: ActionBarDrawerToggle? = null
    var postsAdapters: HashMap<PostCollection, PostsAdapter>? = null
    var activePostsAdapter: PostsAdapter? = null
    var drawerAdapter: DrawerAdapter? = null
    var lastViewedPredefinedCollection: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (requireCurrentSession()) {
            setContentView(R.layout.activity_main)

            val layoutManager = LinearLayoutManager(this)
            postsAdapters = HashMap(PostCollection.Predefined.map { it to PostsAdapter(this, it) }.toMap())

            (postsView as RecyclerView).layoutManager = layoutManager
            (postsView as RecyclerView).addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                    if (layoutManager.findLastVisibleItemPosition() == activePostsAdapter!!.itemCount - 3) {
                        activePostsAdapter!!.loadMore()
                    }
                }
            })

            drawerAdapter = DrawerAdapter(this)

            if (savedInstanceState != null && savedInstanceState.containsKey(LAST_VIEWED_PREDEFINED_POST_COLLECTION_TAG)) {
                lastViewedPredefinedCollection = savedInstanceState.getInt(LAST_VIEWED_PREDEFINED_POST_COLLECTION_TAG)
            }

            val activePostCollection = if (savedInstanceState == null)
                PostCollection.Friends else
                if (savedInstanceState.containsKey(ACTIVE_PREDEFINED_POST_COLLECTION_TAG))
                    PostCollection.Predefined[savedInstanceState.getInt(ACTIVE_PREDEFINED_POST_COLLECTION_TAG)] else
                    SubdomainPostCollection(savedInstanceState.getString(ACTIVE_SUBDOMAIN_POST_COLLECTION_TAG))

            setActivePostsAdapter(activePostCollection, savedInstanceState == null)

            drawerListView.adapter = drawerAdapter
            drawerListView.setOnItemClickListener { parent, view, position, id ->
                val postCollectionValuesSize = PostCollection.Predefined.size
                if (position < postCollectionValuesSize) {
                    setActivePostsAdapter(PostCollection.Predefined[position], true)
                } else if (position == postCollectionValuesSize) {
                    logOut()
                } else {
                    startActivity(Intent(this, AboutActivity::class.java))
                }
                (drawerLayout as DrawerLayout).closeDrawers()
            }

            drawerToggle = ActionBarDrawerToggle(this, drawerLayout as DrawerLayout, android.R.string.ok, android.R.string.ok)
            (drawerLayout as DrawerLayout).setDrawerListener(drawerToggle)
            supportActionBar.setDisplayHomeAsUpEnabled(true)
            supportActionBar.setHomeButtonEnabled(true)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        drawerToggle!!.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (drawerToggle!!.onOptionsItemSelected(menuItem)) {
            return true
        }

        return super.onOptionsItemSelected(menuItem)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle!!.syncState()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (activePostsAdapter!!.collection.isPredefined()) {
            outState.putInt(ACTIVE_PREDEFINED_POST_COLLECTION_TAG, activePostsAdapter!!.collection.ordinal())
        } else {
            outState.putString(ACTIVE_SUBDOMAIN_POST_COLLECTION_TAG, activePostsAdapter!!.collection.subdomain)
        }

        if (lastViewedPredefinedCollection != null) {
            outState.putInt(LAST_VIEWED_PREDEFINED_POST_COLLECTION_TAG, lastViewedPredefinedCollection!!)
        }
    }

    override fun onBackPressed() {
        if (lastViewedPredefinedCollection != null) {
            setActivePostsAdapter(PostCollection.Predefined[lastViewedPredefinedCollection!!], false)
            lastViewedPredefinedCollection = null
        } else {
            super.onBackPressed()
        }
    }

    fun showLoadingIndicator() {
        loadingIndicator.visibility = View.VISIBLE
        loadingIndicator.animate().alpha(1.0f);
    }

    fun hideLoadingIndicator() {
        loadingIndicator.animate().alpha(0.0f);
    }

    fun logOut() {
        (application as Application).logOut()
        requireCurrentSession()
    }

    fun setActivePostsAdapter(postCollection: PostCollection, load: Boolean) {
        lastViewedPredefinedCollection = null

        if (activePostsAdapter != null) {
            activePostsAdapter!!.deactivate()

            if (activePostsAdapter!!.collection.isPredefined() && !postCollection.isPredefined()) {
                lastViewedPredefinedCollection = activePostsAdapter!!.collection.ordinal()
            }
        }

        activePostsAdapter = if (postCollection.isPredefined())
            postsAdapters!![postCollection] else
            PostsAdapter(this, postCollection)

        activePostsAdapter!!.activate(load)
        (postsView as RecyclerView).adapter = activePostsAdapter
        drawerAdapter!!.setActiveItemPosition(postCollection.ordinal())
        title = if (postCollection.isPredefined())
            resources.getStringArray(R.array.post_collections)[postCollection.ordinal()] else
            getString(R.string.soup_of_user, postCollection.subdomain)
    }

    private fun requireCurrentSession(): Boolean {
        if ((application as Application).currentSession == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return false
        }

        return true
    }

}