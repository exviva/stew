package net.keitto.keitto;

import java.util.ArrayList;
import java.util.List;

public class PostsProvider {

    public interface Listener {
        void onPostsLoaded(List<Post> posts);
    }

    private static final List<Post> POSTS = new ArrayList<Post>() {{
        add(new Post("http://asset-7.soup.io/asset/12310/2641_7b26.jpeg"));
        add(new Post("http://asset-b.soup.io/asset/11263/4660_bdf2_520.jpeg"));
        add(new Post("http://asset-d.soup.io/asset/8685/3796_d822.jpeg"));
    }};

    private Listener mListener;

    public PostsProvider(Listener listener) {
        mListener = listener;
    }

    public void loadPosts() {
        mListener.onPostsLoaded(POSTS);
    }

}
