package net.keitto.keitto

class PostsProvider(private val listener: PostsProvider.Listener, private val application: Application) {

    public interface Listener {
        public fun onPostsLoaded(posts: Collection<Post>)
    }

    public fun loadPosts() {
        application.api.fetchPosts { listener.onPostsLoaded(it) }
    }

}
