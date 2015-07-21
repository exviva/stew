package net.keitto.keitto

class PostsProvider(private val listener: PostsProvider.Listener, private val application: Application) {

    public interface Listener {
        public fun onPostsLoaded(collection: PostCollection, posts: Collection<Post>)
    }

    public fun loadPosts(collection: PostCollection) {
        application.api.fetchPosts(collection) { listener.onPostsLoaded(collection, it) }
    }

}
