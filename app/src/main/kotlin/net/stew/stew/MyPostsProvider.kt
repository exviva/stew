package net.stew.stew

class MyPostsProvider(listener: PostsProvider.Listener, application: Application) :
    PostsProvider(listener, application) {

    private var meCookies: Map<String, String>? = null

    override protected fun fetchPosts(lastPost: Post?) {
        application.api.fetchMyPosts(lastPost, meCookies, errorListener) { posts, cookies ->
            meCookies = cookies
            successListener(posts)
        }
    }
}