package net.stew.stew

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.activity_main.drawerLayout
import kotlinx.android.synthetic.activity_main.drawerListView
import kotlinx.android.synthetic.activity_main.loadingIndicator
import kotlinx.android.synthetic.activity_main.postsView

class MainActivity : Activity() {

    var drawerToggle: ActionBarDrawerToggle? = null
    var postsAdapters: Map<PostCollection, PostsAdapter>? = null
    var activePostsAdapter: PostsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (requireCurrentSession()) {
            setContentView(R.layout.activity_main)

            val layoutManager = LinearLayoutManager(this)
            postsAdapters = PostCollection.values().map { it to PostsAdapter(this, it) }.toMap()

            postsView.setHasFixedSize(true)
            postsView.setLayoutManager(layoutManager)
            postsView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                    if (layoutManager.findLastVisibleItemPosition() == activePostsAdapter!!.getItemCount() - 1) {
                        activePostsAdapter!!.load()
                    }
                }
            })

            setActivePostsAdapter(PostCollection.FRIENDS)

            val listItems = getResources().getStringArray(R.array.post_collections) + getString(R.string.log_out)
            drawerListView.setAdapter(ArrayAdapter<String>(this, android.R.layout.simple_selectable_list_item, listItems))
            drawerListView.setOnItemClickListener { parent, view, position, id ->
                if (position < PostCollection.values().size()) {
                    setActivePostsAdapter(PostCollection.values()[position])
                } else {
                    logOut()
                }
                drawerLayout.closeDrawers()
            }

            drawerToggle = ActionBarDrawerToggle(this, drawerLayout, android.R.string.ok, android.R.string.ok)
            drawerLayout.setDrawerListener(drawerToggle)
            getActionBar().setDisplayHomeAsUpEnabled(true)
            getActionBar().setHomeButtonEnabled(true)
        }
    }

    private fun setActivePostsAdapter(postCollection: PostCollection) {
        if (activePostsAdapter != null) {
            activePostsAdapter!!.deactivate()
        }

        activePostsAdapter = postsAdapters!![postCollection]
        activePostsAdapter!!.activate()
        postsView.setAdapter(activePostsAdapter)
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

    fun showLoadingIndicator() {
        loadingIndicator.animate().translationY(0.0f)
    }

    fun hideLoadingIndicator() {
        loadingIndicator.animate().translationY(loadingIndicator.getHeight().toFloat())
    }

    private fun logOut() {
        (getApplication() as Application).logOut()
        requireCurrentSession()
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