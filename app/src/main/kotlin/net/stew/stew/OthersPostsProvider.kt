package net.stew.stew

class OthersPostsProvider(listener: PostsProvider.Listener, private val collection: PostCollection,
    application: Application) : PostsProvider(listener, application) {

    override protected fun fetchPosts(lastPost: Post?) {
        application.api.fetchPosts(collection, lastPost, errorListener, successListener)
    }

}