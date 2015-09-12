package net.stew.stew

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
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

class MainActivity : AppCompatActivity() {

    val ACTIVE_POST_COLLECTION_TAG = "active_post_collection"
    var drawerToggle: ActionBarDrawerToggle? = null
    var postsAdapters: Map<PostCollection, PostsAdapter>? = null
    var activePostsAdapter: PostsAdapter? = null
    var drawerAdapter: DrawerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (requireCurrentSession()) {
            setContentView(R.layout.activity_main)

            val layoutManager = LinearLayoutManager(this)
            postsAdapters = PostCollection.values().map { it to PostsAdapter(this, it) }.toMap()

            postsView.setLayoutManager(layoutManager)
            postsView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                    if (layoutManager.findLastVisibleItemPosition() == activePostsAdapter!!.getItemCount() - 3) {
                        activePostsAdapter!!.loadMore()
                    }
                }
            })

            drawerAdapter = DrawerAdapter(this)

            val activePostCollection = if (savedInstanceState == null)
                PostCollection.FRIENDS else
                PostCollection.values()[savedInstanceState.getInt(ACTIVE_POST_COLLECTION_TAG)]

            setActivePostsAdapter(activePostCollection, savedInstanceState == null)

            drawerListView.setAdapter(drawerAdapter)
            drawerListView.setOnItemClickListener { parent, view, position, id ->
                val postCollectionValuesSize = PostCollection.values().size()
                if (position < postCollectionValuesSize) {
                    setActivePostsAdapter(PostCollection.values()[position], true)
                } else if (position == postCollectionValuesSize) {
                    logOut()
                } else {
                    startActivity(Intent(this, javaClass<AboutActivity>()))
                }
                drawerLayout.closeDrawers()
            }

            drawerToggle = ActionBarDrawerToggle(this, drawerLayout, android.R.string.ok, android.R.string.ok)
            drawerLayout.setDrawerListener(drawerToggle)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true)
            getSupportActionBar().setHomeButtonEnabled(true)
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
        outState.putInt(ACTIVE_POST_COLLECTION_TAG, activePostsAdapter!!.collection.ordinal())
    }

    fun showLoadingIndicator() {
        loadingIndicator.setVisibility(View.VISIBLE)
        loadingIndicator.animate().alpha(1.0f);
    }

    fun hideLoadingIndicator() {
        loadingIndicator.animate().alpha(0.0f);
    }

    fun logOut() {
        (getApplication() as Application).logOut()
        requireCurrentSession()
    }

    private fun setActivePostsAdapter(postCollection: PostCollection, load: Boolean) {
        if (activePostsAdapter != null) {
            activePostsAdapter!!.deactivate()
        }

        activePostsAdapter = postsAdapters!![postCollection]
        activePostsAdapter!!.activate(load)

        setTitle(getResources().getStringArray(R.array.post_collections)[postCollection.ordinal()])
        postsView.setAdapter(activePostsAdapter)
        drawerAdapter!!.setActiveItemPosition(postCollection.ordinal())
    }

    private fun requireCurrentSession(): Boolean {
        if ((getApplication() as Application).currentSession == null) {
            val intent = Intent(this, javaClass<LoginActivity>())
            startActivity(intent)
            finish()
            return false
        }

        return true
    }

}