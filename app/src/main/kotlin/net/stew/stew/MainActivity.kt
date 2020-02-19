package net.stew.stew

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.drawerLayout
import kotlinx.android.synthetic.main.activity_main.drawerListView
import kotlinx.android.synthetic.main.activity_main.postsView

class MainActivity : AppCompatActivity() {

    companion object {
        private const val ACTIVE_PREDEFINED_POST_COLLECTION_TAG = "ACTIVE_PREDEFINED_POST_COLLECTION_TAG"
        private const val ACTIVE_SUBDOMAIN_POST_COLLECTION_TAG = "ACTIVE_SUBDOMAIN_POST_COLLECTION_TAG"
        private const val LAST_VIEWED_PREDEFINED_POST_COLLECTION_TAG = "LAST_VIEWED_PREDEFINED_POST_COLLECTION_TAG"
    }

    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var postsAdapters: HashMap<PostCollection, PostsAdapter>
    private lateinit var activePostsAdapter: PostsAdapter
    private lateinit var drawerAdapter: DrawerAdapter
    private var lastViewedPredefinedCollection: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (requireCurrentSession()) {
            setContentView(R.layout.activity_main)

            val layoutManager = LinearLayoutManager(this)
            postsAdapters = HashMap(PostCollection.Predefined.map { it to PostsAdapter(this, it) }.toMap())

            postsView.layoutManager = layoutManager
            postsView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    activePostsAdapter.maybeLoadMore(layoutManager.findLastVisibleItemPosition())
                }
            })

            drawerAdapter = DrawerAdapter(this)

            if (savedInstanceState != null && savedInstanceState.containsKey(LAST_VIEWED_PREDEFINED_POST_COLLECTION_TAG)) {
                lastViewedPredefinedCollection = savedInstanceState.getInt(LAST_VIEWED_PREDEFINED_POST_COLLECTION_TAG)
            }

            val activePostCollection = when {
                savedInstanceState == null -> PostCollection.Friends
                savedInstanceState.containsKey(ACTIVE_PREDEFINED_POST_COLLECTION_TAG) -> PostCollection.Predefined[savedInstanceState.getInt(ACTIVE_PREDEFINED_POST_COLLECTION_TAG)]
                else -> SubdomainPostCollection(savedInstanceState.getString(ACTIVE_SUBDOMAIN_POST_COLLECTION_TAG)!!)
            }

            setActivePostsAdapter(activePostCollection, savedInstanceState == null)

            drawerListView.adapter = drawerAdapter
            drawerListView.setOnItemClickListener { _, _, position, _ ->
                val postCollectionValuesSize = PostCollection.Predefined.size
                when {
                    position < postCollectionValuesSize -> setActivePostsAdapter(PostCollection.Predefined[position], true)
                    position == postCollectionValuesSize -> logOut()
                    else -> startActivity(Intent(this, AboutActivity::class.java))
                }
                drawerLayout.closeDrawers()
            }

            drawerToggle = ActionBarDrawerToggle(this, drawerLayout, android.R.string.ok, android.R.string.ok)
            drawerLayout.addDrawerListener(drawerToggle)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(menuItem)) {
            return true
        }

        return super.onOptionsItemSelected(menuItem)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (activePostsAdapter.collection.isPredefined()) {
            outState.putInt(ACTIVE_PREDEFINED_POST_COLLECTION_TAG, activePostsAdapter.collection.ordinal())
        } else {
            outState.putString(ACTIVE_SUBDOMAIN_POST_COLLECTION_TAG, activePostsAdapter.collection.subdomain)
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

    fun logOut() {
        (application as Application).logOut()
        requireCurrentSession()
    }

    fun setActivePostsAdapter(postCollection: PostCollection, load: Boolean) {
        lastViewedPredefinedCollection = null

        if (::activePostsAdapter.isInitialized) {
            activePostsAdapter.deactivate()

            if (activePostsAdapter.collection.isPredefined() && !postCollection.isPredefined()) {
                lastViewedPredefinedCollection = activePostsAdapter.collection.ordinal()
            }
        }

        activePostsAdapter =
            if (postCollection.isPredefined()) postsAdapters[postCollection]!!
            else PostsAdapter(this, postCollection)

        activePostsAdapter.activate(load)
        postsView.adapter = activePostsAdapter
        drawerAdapter.setActiveItemPosition(postCollection.ordinal())
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