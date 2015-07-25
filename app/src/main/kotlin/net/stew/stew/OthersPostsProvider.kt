package net.stew.stew

import android.os.AsyncTask
import org.jsoup.nodes.Document

class OthersPostsProvider(listener: PostsProvider.Listener, private val collection: PostCollection,
    application: Application) : PostsProvider(listener, application) {

    override protected fun fetchPosts(lastPost: Post?): AsyncTask<Void, Void, Document?> {
        return application.api.fetchPosts(collection, lastPost, errorListener, successListener)
    }

}