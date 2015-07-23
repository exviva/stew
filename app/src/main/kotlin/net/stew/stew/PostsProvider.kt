package net.stew.stew

abstract class PostsProvider(private val listener: PostsProvider.Listener, protected val application: Application) {

    public interface Listener {
        fun onPostsLoaded(posts: Collection<Post>)
        fun onPostsLoadError()
    }

    private var loadingInProgress = false
    protected val errorListener: () -> Unit = {
        loadingInProgress = false
        listener.onPostsLoadError()
    }
    protected val successListener: (Collection<Post>) -> Unit = {
        loadingInProgress = false
        listener.onPostsLoaded(it)
    }

    public fun loadPosts(lastPost: Post?) {
        if (!loadingInProgress) {
            loadingInProgress = true
            fetchPosts(lastPost)
        }
    }

    abstract protected fun fetchPosts(lastPost: Post?)

}
