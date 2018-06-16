package net.stew.stew

class PostsProvider(
        private val application: Application,
        private val collection: PostCollection,
        private val listener: PostsProvider.Listener) {

    interface Listener {
        fun onPostsLoaded(posts: Collection<Post>)
        fun onPostsLoadError(connectionError: ConnectionError)
    }

    private var loadingTask: Task? = null
    private var loadingTaskIsForMorePosts: Boolean = false

    private val errorListener: (ConnectionError) -> Unit = {
        loadingTask = null
        listener.onPostsLoadError(it)
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
