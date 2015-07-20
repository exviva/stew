package net.keitto.keitto

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView

class PostsActivity : Activity() {

    private var mPostsView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_posts)

        mPostsView = findViewById(R.id.postsView) as RecyclerView

        mPostsView!!.setHasFixedSize(true)
        mPostsView!!.setLayoutManager(LinearLayoutManager(this))
        mPostsView!!.setAdapter(PostsAdapter())
    }

}
