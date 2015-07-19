package net.keitto.keitto

import android.os.AsyncTask
import org.jsoup.Jsoup
import java.util.ArrayList

class PostsProvider(private val listener: PostsProvider.Listener) {

    public interface Listener {
        public fun onPostsLoaded(posts: Collection<Post>)
    }

    public fun loadPosts() {
        FetchingTask().execute()
    }

    private inner class FetchingTask : AsyncTask<Void, Void, Collection<Post>>() {
        override fun doInBackground(vararg params: Void?): Collection<Post>? {
            return Jsoup.connect("http://www.soup.io/friends").
                cookie("soup_user_id", "XXX").
                get().
                select(".post_image .imagecontainer img").
                map { Post(it.attr("src")) }
        }

        override fun onPostExecute(posts: Collection<Post>) {
            listener.onPostsLoaded(posts)
        }

    }

}
