package net.keitto.keitto

import java.util.ArrayList

public class PostsProvider(private val mListener: PostsProvider.Listener) {

    public interface Listener {
        public fun onPostsLoaded(posts: List<Post>)
    }

    public fun loadPosts() {
        mListener.onPostsLoaded(POSTS)
    }

    companion object {

        private val POSTS = object : ArrayList<Post>() {
            init {
                add(Post("http://asset-7.soup.io/asset/12310/2641_7b26.jpeg"))
                add(Post("http://asset-b.soup.io/asset/11263/4660_bdf2_520.jpeg"))
                add(Post("http://asset-d.soup.io/asset/8685/3796_d822.jpeg"))
            }
        }
    }

}
