package net.stew.stew

import android.content.Intent
import android.graphics.drawable.Animatable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.drawable.ProgressBarDrawable
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.image.ImageInfo

class PostsAdapter(val activity: MainActivity, var collection: PostCollection) :
    RecyclerView.Adapter<PostsAdapter.PostViewHolder>(), PostsProvider.Listener {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val postImageView = itemView.findViewById(R.id.postImageView) as SimpleDraweeView
        val repostButton = itemView.findViewById(R.id.repostButton) as Button
        val shareButton = itemView.findViewById(R.id.shareButton) as ImageButton
        val authorshipLayout = itemView.findViewById(R.id.authorshipLayout)
        val authorLayout = itemView.findViewById(R.id.authorLayout)
        val authorNameTextView = itemView.findViewById(R.id.authorNameTextView) as TextView
        val authorImageView = itemView.findViewById(R.id.authorImageView) as SimpleDraweeView
        val groupLayout = itemView.findViewById(R.id.groupLayout)
        val groupNameTextView = itemView.findViewById(R.id.groupNameTextView) as TextView
        val groupImageView = itemView.findViewById(R.id.groupImageView) as SimpleDraweeView
        val description = itemView.findViewById(R.id.descriptionTextView) as TextView

    }

    enum class LoadMode {
        REPLACE,
        APPEND
    }

    private val application = activity.application as Application
    private var posts = application.postsStore.restore(collection)
    private val postsProvider = PostsProvider(application, collection, this)
    private var loadMode = LoadMode.REPLACE
    private var isActive = false


    fun activate(load: Boolean) {
        isActive = true

        if (load) {
            loadMode = LoadMode.REPLACE
            load()
        } else if (postsProvider.isLoading()) {
            showLoadingIndicator()
        }
    }

    fun deactivate() {
        hideLoadingIndicator()
        isActive = false
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): PostViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.post, viewGroup, false)

        return PostViewHolder(view).apply {
            val drawable = ProgressBarDrawable().apply {
                color = ContextCompat.getColor(activity, R.color.accent)
                barWidth = activity.resources.getDimensionPixelSize(R.dimen.image_progress_bar_height)
            }

            postImageView.hierarchy.setProgressBarImage(drawable)
        }
    }

    override fun onBindViewHolder(postViewHolder: PostViewHolder, i: Int) {
        val post = posts[i]

        postViewHolder.postImageView.apply {
            if (tag != post.uri) {
                tag = post.uri
                post.imageLoaded = false
                setOnClickListener(null)
                controller = Fresco.newDraweeControllerBuilder().
                        setUri(post.uri).
                        setAutoPlayAnimations(true).
                        setOldController(postViewHolder.postImageView.controller).
                        setTapToRetryEnabled(true).
                        setControllerListener(object : BaseControllerListener<ImageInfo>() {
                            override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
                                post.imageLoaded = true
                                notifyDataSetChanged()
                                setOnClickListener {
                                    FullscreenImageActivity.start(activity, post.uri, it)
                                }
                            }
                        }).
                        build()
            }
        }

        postViewHolder.description.apply {
            visibility = if (post.description.isBlank()) View.GONE else View.VISIBLE
            text = post.description
        }

        postViewHolder.repostButton.apply {
            visibility = if (collection != PostCollection.Me) View.VISIBLE else View.GONE
            isEnabled = post.imageLoaded && post.repostState == Post.RepostState.NOT_REPOSTED

            setOnClickListener {
                val errorListener: (ResponseStatus) -> Unit = {
                    if (it == ResponseStatus.FORBIDDEN) {
                        handleForbidden()
                    } else {
                        notifyDataSetChanged()
                        showErrorToast()
                    }
                }
                application.api.repost(post, errorListener) { notifyDataSetChanged() }
                notifyDataSetChanged()
            }

            val stringId = when (post.repostState) {
                Post.RepostState.NOT_REPOSTED, Post.RepostState.BLOCKED -> R.string.repost
                Post.RepostState.REPOSTING -> R.string.reposting
                Post.RepostState.REPOSTED -> R.string.reposted
            }
            setText(stringId)
        }
        postViewHolder.shareButton.apply {
            visibility = if (post.imageLoaded) View.VISIBLE else View.GONE

            setOnClickListener {
                val sendIntent = Intent(Intent.ACTION_SEND)
                sendIntent.setType("text/plain")
                sendIntent.putExtra(Intent.EXTRA_TEXT, post.uri.toString())
                activity.startActivity(Intent.createChooser(sendIntent, ""))
            }
        }

        if (collection.subdomain != null) {
            postViewHolder.authorshipLayout.visibility = View.GONE
        } else {
            val group = post.group

            postViewHolder.authorshipLayout.visibility = View.VISIBLE

            postViewHolder.authorNameTextView.text = post.author.name
            postViewHolder.authorImageView.setImageURI(post.author.imageUri)
            postViewHolder.authorLayout.setOnClickListener {
                val collection = SubdomainPostCollection(post.author.name)
                activity.setActivePostsAdapter(collection, true)
            }

            if (group != null) {
                postViewHolder.groupLayout.visibility = View.VISIBLE
                postViewHolder.groupNameTextView.text = group.name
                postViewHolder.groupImageView.setImageURI(group.imageUri)
                postViewHolder.groupLayout.setOnClickListener {
                    val collection = SubdomainPostCollection(group.name)
                    activity.setActivePostsAdapter(collection, true)
                }
            } else {
                postViewHolder.groupLayout.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = posts.size

    override fun onPostsLoaded(posts: Collection<Post>) {
        this.posts = if (loadMode == LoadMode.REPLACE) posts.toList() else this.posts + posts

        application.postsStore.store(collection, this.posts)
        notifyDataSetChanged()
        stopLoading()
    }

    override fun onPostsLoadError(responseStatus: ResponseStatus) {
        if (responseStatus == ResponseStatus.FORBIDDEN) {
            handleForbidden()
        } else {
            stopLoading()
            showErrorToast()
        }
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

    private fun handleForbidden() {
        Toast.makeText(activity, R.string.forbidden, Toast.LENGTH_SHORT).show()
        activity.logOut()
    }

}
