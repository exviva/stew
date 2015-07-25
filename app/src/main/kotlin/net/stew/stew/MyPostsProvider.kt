package net.stew.stew

import android.os.AsyncTask
import org.jsoup.nodes.Document

class MyPostsProvider(listener: PostsProvider.Listener, application: Application) :
    PostsProvider(listener, application) {

    private var meCookies: Map<String, String>? = null

    override protected fun fetchPosts(lastPost: Post?): AsyncTask<Void, Void, Document?> {
        return application.api.fetchMyPosts(lastPost, meCookies, errorListener) { posts, cookies ->
            meCookies = cookies
            successListener(posts)
        }
    }
}