package net.stew.stew

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val ACTIVE_PREDEFINED_POST_COLLECTION_TAG = "ACTIVE_PREDEFINED_POST_COLLECTION_TAG"
        private const val ACTIVE_SUBDOMAIN_POST_COLLECTION_TAG = "ACTIVE_SUBDOMAIN_POST_COLLECTION_TAG"
        private const val LAST_VIEWED_PREDEFINED_POST_COLLECTION_TAG = "LAST_VIEWED_PREDEFINED_POST_COLLECTION_TAG"
    }

    private lateinit var postsAdapters: HashMap<PostCollection, PostsAdapter>
    private lateinit var activePostsAdapter: PostsAdapter
    private var lastViewedPredefinedCollection: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if ((application as Application).requireCurrentSession()) {
            setContentView(R.layout.activity_main)

            val layoutManager = LinearLayoutManager(this)
            postsAdapters = HashMap(PostCollection.Predefined.map { it to PostsAdapter(this, it) }.toMap())

            postsView.layoutManager = layoutManager
            postsView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                    postsView.post { activePostsAdapter.maybeLoadMore(lastVisibleItemPosition) }
                }
            })

            if (savedInstanceState != null && savedInstanceState.containsKey(LAST_VIEWED_PREDEFINED_POST_COLLECTION_TAG)) {
                lastViewedPredefinedCollection = savedInstanceState.getInt(LAST_VIEWED_PREDEFINED_POST_COLLECTION_TAG)
            }

            val activePostCollection = when {
                savedInstanceState == null -> PostCollection.Friends
                savedInstanceState.containsKey(ACTIVE_PREDEFINED_POST_COLLECTION_TAG) -> PostCollection.Predefined[savedInstanceState.getInt(ACTIVE_PREDEFINED_POST_COLLECTION_TAG)]
                else -> SubdomainPostCollection(savedInstanceState.getString(ACTIVE_SUBDOMAIN_POST_COLLECTION_TAG)!!)
            }

            setActivePostsAdapter(activePostCollection, savedInstanceState == null)

            navigationView.setOnNavigationItemSelectedListener { item ->
                when(item.itemId) {
                    R.id.friends -> setActivePostsAdapter(PostCollection.Friends, true)
                    R.id.fof -> setActivePostsAdapter(PostCollection.FOF, true)
                    R.id.everyone -> setActivePostsAdapter(PostCollection.Everyone, true)
                    R.id.me -> setActivePostsAdapter(PostCollection.Me, true)
                    R.id.about -> {
                        startActivity(Intent(this, AboutActivity::class.java))
                        return@setOnNavigationItemSelectedListener false
                    }
                }
                true
            }
        }
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
    }

}