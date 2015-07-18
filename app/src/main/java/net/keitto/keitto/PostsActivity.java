package net.keitto.keitto;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public class PostsActivity extends Activity {

    private RecyclerView mPostsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_posts);

        mPostsView = (RecyclerView) findViewById(R.id.postsView);

        mPostsView.setHasFixedSize(true);
        mPostsView.setLayoutManager(new LinearLayoutManager(this));
        mPostsView.setAdapter(new PostsAdapter(this));
    }

}
