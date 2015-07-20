package net.keitto.keitto

class PostsProvider(private val listener: PostsProvider.Listener) {

    public interface Listener {
        public fun onPostsLoaded(posts: Collection<Post>)
    }

    public fun loadPosts() {
        Api.fetchPosts { listener.onPostsLoaded(it) }
    }

}
