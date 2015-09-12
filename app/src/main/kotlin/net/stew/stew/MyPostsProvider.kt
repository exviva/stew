package net.stew.stew

import android.os.AsyncTask
import org.jsoup.nodes.Document

class MyPostsProvider(listener: PostsProvider.Listener, application: Application) :
    PostsProvider(listener, application) {

    override protected fun fetchPosts(lastPost: Post?): AsyncTask<Void, Void, Pair<ResponseStatus, Document?>> {
        return application.api.fetchMyPosts(lastPost, errorListener, successListener)
    }

}