package net.keitto.keitto;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> implements PostsProvider.Listener {

    public static class PostViewHolder extends RecyclerView.ViewHolder {

        private final TextView mPostUrlView;

        public PostViewHolder(View itemView) {
            super(itemView);
            mPostUrlView = (TextView) itemView.findViewById(R.id.postUrlView);
        }

    }

    private List<Post> mPosts;

    public PostsAdapter() {
        new PostsProvider(this).loadPosts();
    }

    @Override
    public PostViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.post, viewGroup, false);

        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PostViewHolder postViewHolder, int i) {
        Post post = mPosts.get(i);

        postViewHolder.mPostUrlView.setText(post.getUrl());
    }

    @Override
    public int getItemCount() {
        return mPosts.size();
    }

    @Override
    public void onPostsLoaded(List<Post> posts) {
        mPosts = posts;

        notifyDataSetChanged();
    }

}
