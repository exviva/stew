package net.keitto.keitto

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.activity_main.drawerLayout
import kotlinx.android.synthetic.activity_main.drawerListView
import kotlinx.android.synthetic.activity_main.postsView

class MainActivity : Activity() {

    var drawerToggle: ActionBarDrawerToggle? = null
    var postsAdapter: PostsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (requireCurrentSession()) {
            setContentView(R.layout.activity_main)

            postsAdapter = PostsAdapter(this)
            postsView.setHasFixedSize(true)
            postsView.setLayoutManager(LinearLayoutManager(this))
            postsView.setAdapter(postsAdapter)

            val listItems = getResources().getStringArray(R.array.post_collections) + "Log out"
            drawerListView.setAdapter(ArrayAdapter<String>(this, android.R.layout.simple_selectable_list_item, listItems))
            drawerListView.setOnItemClickListener { parent, view, position, id ->
                if (position < PostCollection.values().size()) {
                    postsAdapter!!.setCollection(PostCollection.values()[position])
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