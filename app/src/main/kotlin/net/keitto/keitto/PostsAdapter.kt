package net.keitto.keitto

import android.graphics.drawable.Animatable
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.image.ImageInfo

class PostsAdapter(val application: Application) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>(), PostsProvider.Listener {

    public class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val postImageView: SimpleDraweeView
        val repostButton: Button

        init {
            postImageView = itemView.findViewById(R.id.postImageView) as SimpleDraweeView
            repostButton = itemView.findViewById(R.id.repostButton) as Button
        }

    }

    private var posts: List<Post>? = null
    private var collection: PostCollection = PostCollection.FRIENDS

    init {
        load()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): PostViewHolder {
        val view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.post, viewGroup, false)

        return PostViewHolder(view)
    }

    override fun onBindViewHolder(postViewHolder: PostViewHolder, i: Int) {
        val post = posts!!.get(i)

        val controller = Fresco.newDraweeControllerBuilder().
            setUri(post.uri).
            setAutoPlayAnimations(true).
            setControllerListener(object: BaseControllerListener<ImageInfo>() {
                override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
                    postViewHolder.repostButton.setEnabled(true)
                }
            }).
            build()
        postViewHolder.postImageView.setController(controller)
        postViewHolder.repostButton.setVisibility(if (collection != PostCollection.ME) View.VISIBLE else View.GONE)
        postViewHolder.repostButton.setEnabled(false)
        postViewHolder.repostButton.setOnClickListener { application.api.repost(post) }

    }

    override fun getItemCount(): Int {
        return posts?.size() ?: 0
    }

    override fun onPostsLoaded(collection: PostCollection, posts: Collection<Post>) {
        if (collection == this.collection) {
            this.posts = posts.toList()

            notifyDataSetChanged()
        }
    }

    fun setCollection(collection: PostCollection) {
        this.collection = collection

        load()
    }

    private fun load() {
        posts = null
        notifyDataSetChanged()
        val provider = PostsProvider(this, application)
        provider.loadPosts(collection)
    }

}