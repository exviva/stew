package net.stew.stew

import android.graphics.drawable.Animatable
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.drawable.ProgressBarDrawable
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.image.ImageInfo

class PostsAdapter(val activity: MainActivity, var collection: PostCollection) :
    RecyclerView.Adapter<PostsAdapter.PostViewHolder>(), PostsProvider.Listener {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val postImageView: SimpleDraweeView
        val repostButton: Button

        init {
            postImageView = itemView.findViewById(R.id.postImageView) as SimpleDraweeView
            repostButton = itemView.findViewById(R.id.repostButton) as Button
        }

    }

    enum class LoadMode {
        REPLACE,
        APPEND
    }

    private val application: Application
    private var posts: List<Post>
    private val postsProvider: PostsProvider
    private var loadMode = LoadMode.REPLACE
    private var isActive = false


    init {
        application = activity.getApplication() as Application
        postsProvider = when (collection) {
            PostCollection.ME -> MyPostsProvider(this, application)
            else -> OthersPostsProvider(this, collection, application)
        }
        posts = application.postsStore.restore(collection)
    }

    fun activate(load: Boolean) {
        isActive = true

        if (load) {
            loadMode = LoadMode.REPLACE
            load()
        }

        if (postsProvider.isLoading()) {
            showLoadingIndicator()
        }
    }

    fun deactivate() {
        hideLoadingIndicator()
        isActive = false
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): PostViewHolder {
        val view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.post, viewGroup, false)
        val viewHolder = PostViewHolder(view)
        val progressDrawable = ProgressBarDrawable()

        progressDrawable.setColor(activity.getResources().getColor(R.color.accent))
        progressDrawable.setBarWidth(activity.getResources().getDimensionPixelSize(R.dimen.image_progress_bar_height))

        val draweeHierarchy = GenericDraweeHierarchyBuilder(activity.getResources()).
            setProgressBarImage(progressDrawable).
            build()

        viewHolder.postImageView.setHierarchy(draweeHierarchy)

        return viewHolder
    }

    override fun onBindViewHolder(postViewHolder: PostViewHolder, i: Int) {
        val post = posts.get(i)

        val controller = Fresco.newDraweeControllerBuilder().
            setUri(post.uri).
            setAutoPlayAnimations(true).
            setOldController(postViewHolder.postImageView.getController()).
            setControllerListener(object : BaseControllerListener<ImageInfo>() {
                override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
                    postViewHolder.repostButton.setEnabled(post.repostState == Post.RepostState.NOT_REPOSTED)
                    postViewHolder.postImageView.setOnClickListener {
                        FullscreenImageActivity.start(activity, post.uri, it)
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
            it.setOnClickListener {
                val errorListener = {
                    notifyDataSetChanged()
                    showErrorToast()
                }
                application.api.repost(post, errorListener) { notifyDataSetChanged() }
                notifyDataSetChanged()
            }

            val stringId = when (post.repostState) {
                Post.RepostState.NOT_REPOSTED -> R.string.repost
                Post.RepostState.REPOSTING -> R.string.reposting
                Post.RepostState.REPOSTED -> R.string.reposted
            }
            it.setText(stringId)
        }
    }

    override fun getItemCount(): Int {
        return posts.size()
    }

    override fun onPostsLoaded(posts: Collection<Post>) {
        this.posts = if (loadMode == LoadMode.REPLACE) posts.toList() else this.posts + posts

        application.postsStore.store(collection, this.posts)
        notifyDataSetChanged()
        stopLoading()
    }

    override fun onPostsLoadError() {
        stopLoading()
        showErrorToast()
    }

    fun loadMore() {
        loadMode = LoadMode.APPEND
        load()
    }

    private fun load() {
        showLoadingIndicator()
        postsProvider.loadPosts(if (loadMode == LoadMode.REPLACE) null else posts.lastOrNull())
    }

    private fun stopLoading() {
        hideLoadingIndicator()
    }

    private fun showLoadingIndicator() {
        activity.showLoadingIndicator()
    }

    private fun hideLoadingIndicator() {
        if (isActive) {
            activity.hideLoadingIndicator()
        }
    }

    private fun showErrorToast() {
        if (isActive) {
            Toast.makeText(activity, R.string.network_error, Toast.LENGTH_SHORT).show()
        }
    }

}