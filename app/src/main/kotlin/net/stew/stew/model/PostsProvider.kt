package net.stew.stew.model

import net.stew.stew.Application
import net.stew.stew.ConnectionError
import net.stew.stew.Task

class PostsProvider(
        private val application: Application,
        private val collection: PostCollection,
        private val listener: Listener) {

    interface Listener {
        fun onPostsLoaded(posts: Collection<Post>)
        fun onPostsLoadError(connectionError: ConnectionError)
        fun onPostsLoadRetrying(retriesLeft: Int)
    }

    private var loadingTask: Task? = null
    private var loadingTaskIsForMorePosts: Boolean = false

    private val errorListener: (ConnectionError) -> Unit = {
        if (it.retriesLeft != null) {
            listener.onPostsLoadRetrying(it.retriesLeft)
        } else {
            loadingTask = null
            listener.onPostsLoadError(it)
        }
    }
    private val successListener: (Collection<Post>) -> Unit = {
        loadingTask = null
        listener.onPostsLoaded(it)
    }

    fun loadPosts(lastPost: Post?) {
        if (loadingTask != null) {
            if (loadingTaskIsForMorePosts == (lastPost != null)) {
                return
            } else {
                loadingTask!!.cancel(true)
            }
        }

        loadingTask = application.api.fetchPosts(collection, lastPost, errorListener, successListener)
        loadingTaskIsForMorePosts = lastPost != null
    }

    fun isLoading(): Boolean = loadingTask != null

}
