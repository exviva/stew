package net.stew.stew

class PostsProvider(private val listener: PostsProvider.Listener, private val application: Application) {

    public interface Listener {
        public fun onPostsLoaded(collection: PostCollection, posts: Collection<Post>)
        fun onPostsLoadError(): Unit
    }

    public fun loadPosts(collection: PostCollection) {
        val errorListener = { listener.onPostsLoadError() }
        application.api.fetchPosts(collection, errorListener) { listener.onPostsLoaded(collection, it) }
    }

}
