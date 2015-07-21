package net.keitto.keitto

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.activity_main.*

class MainActivity : Activity() {

    var drawerToggle: ActionBarDrawerToggle? = null
    var postsAdapter: PostsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (requireCurrentSession()) {
            setContentView(R.layout.activity_main)

            postsAdapter = PostsAdapter(getApplication() as Application)
            postsView.setHasFixedSize(true)
            postsView.setLayoutManager(LinearLayoutManager(this))
            postsView.setAdapter(postsAdapter)

            val listItems = arrayOf("Friends", "Friends of friends", "My soup", "Log out")
            drawerListView.setAdapter(ArrayAdapter<String>(this, android.R.layout.simple_selectable_list_item, listItems))
            drawerListView.setOnItemClickListener { parent, view, position, id ->
                when (listItems[position]) {
                    "Friends" -> postsAdapter!!.setCollection(PostCollection.FRIENDS)
                    "Friends of friends" -> postsAdapter!!.setCollection(PostCollection.FOF)
                    "My soup" -> postsAdapter!!.setCollection(PostCollection.ME)
                    "Log out" -> logOut()
                }
                drawerLayout.closeDrawers()
            }

            drawerToggle = ActionBarDrawerToggle(this, drawerLayout, android.R.string.ok, android.R.string.ok)
            drawerLayout.setDrawerListener(drawerToggle)
            getActionBar().setDisplayHomeAsUpEnabled(true)
            getActionBar().setHomeButtonEnabled(true)
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