package net.keitto.keitto

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import com.squareup.picasso.Picasso
import net.keitto.keitto.R

public class PostsAdapter(private val mContext: Context) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>(), PostsProvider.Listener {

    public class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val mPostImageView: ImageView

        init {
            mPostImageView = itemView.findViewById(R.id.postImageView) as ImageView
        }

    }

    private var mPosts: List<Post>? = null

    init {
        PostsProvider(this).loadPosts()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): PostViewHolder {
        val view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.post, viewGroup, false)

        return PostViewHolder(view)
    }

    override fun onBindViewHolder(postViewHolder: PostViewHolder, i: Int) {
        val post = mPosts!!.get(i)

        Picasso.with(mContext).load(post.url).into(postViewHolder.mPostImageView)
    }

    override fun getItemCount(): Int {
        return mPosts!!.size()
    }

    override fun onPostsLoaded(posts: List<Post>) {
        mPosts = posts

        notifyDataSetChanged()
    }

}
