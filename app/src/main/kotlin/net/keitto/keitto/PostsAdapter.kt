package net.keitto.keitto

import android.app.Activity
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

class PostsAdapter(val activity: Activity) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>(), PostsProvider.Listener {

    public class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val postImageView: SimpleDraweeView
        val repostButton: Button

        init {
            postImageView = itemView.findViewById(R.id.postImageView) as SimpleDraweeView
            repostButton = itemView.findViewById(R.id.repostButton) as Button
        }

    }

    val application: Application
    private var posts: List<Post>? = null
    private var collection: PostCollection = PostCollection.FRIENDS


    init {
        application = activity.getApplication() as Application
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
                    postViewHolder.postImageView.setOnClickListener {
                        FullscreenImageActivity.start(activity, post.uri)
                    }
                }
            }).
            build()

        postViewHolder.postImageView.let {
            it.setController(controller)
            it.setOnClickListener(null)
        }
        postViewHolder.repostButton.let {
            it.setVisibility(if (collection != PostCollection.ME) View.VISIBLE else View.GONE)
            it.setEnabled(false)
            it.setOnClickListener { application.api.repost(post) }
        }
    }

    override fun getItemCount(): Int {
        return posts?.size() ?: 0
    }

    override fun onPostsLoaded(collection: PostCollection, posts: Collection<Post>) {
        if (collection == this.collection) {
            this.posts = posts.toList()
            activity.setTitle(activity.getResources().getStringArray(R.array.post_collections)[collection.ordinal()])
            notifyDataSetChanged()
        }
    }

    fun setCollection(collection: PostCollection) {
        this.collection = collection

        load()
    }

    private fun load() {
        posts = null
        activity.setTitle("Loading...")
        notifyDataSetChanged()
        val provider = PostsProvider(this, application)
        provider.loadPosts(collection)
    }

}