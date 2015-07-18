package net.keitto.keitto;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> implements PostsProvider.Listener {

    public static class PostViewHolder extends RecyclerView.ViewHolder {

        private final ImageView mPostImageView;

        public PostViewHolder(View itemView) {
            super(itemView);
            mPostImageView = (ImageView) itemView.findViewById(R.id.postImageView);
        }

    }

    private final Context mContext;
    private List<Post> mPosts;

    public PostsAdapter(Context context) {
        mContext = context;
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

        Picasso.with(mContext).load(post.getUrl()).into(postViewHolder.mPostImageView);
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
