package net.keitto.keitto

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView

class PostsActivity : Activity() {

    companion object {
        val loginRequest = Activity.RESULT_FIRST_USER
    }

    private var mPostsView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (requireCurrentSession()) {
            initialize()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == loginRequest && resultCode == Activity.RESULT_OK) {
            initialize()
        }
    }

    private fun initialize() {
        setContentView(R.layout.activity_posts)

        mPostsView = findViewById(R.id.postsView) as RecyclerView

        mPostsView!!.setHasFixedSize(true)
        mPostsView!!.setLayoutManager(LinearLayoutManager(this))
        mPostsView!!.setAdapter(PostsAdapter(getApplication() as Application))
    }

    private fun requireCurrentSession(): Boolean {
        if ((getApplication() as Application).currentSession == null) {
            val intent = Intent(this, javaClass<LoginActivity>())
            startActivityForResult(intent, loginRequest)
            return false
        }

        return true
    }

}