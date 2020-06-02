package net.stew.stew

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.drawable.ProgressBarDrawable
import com.facebook.drawee.view.SimpleDraweeView

class PostsAdapter(private val activity: MainActivity, var collection: PostCollection) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), PostsProvider.Listener {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val postImageView = itemView.findViewById(R.id.postImageView) as SimpleDraweeView
        val repostButton = itemView.findViewById(R.id.repostButton) as Button
        val shareButton = itemView.findViewById(R.id.shareButton) as ImageButton
        val authorshipLayout: View = itemView.findViewById(R.id.authorshipLayout)
        val authorLayout: View = itemView.findViewById(R.id.authorLayout)
        val authorNameTextView = itemView.findViewById(R.id.authorNameTextView) as TextView
        val authorImageView = itemView.findViewById(R.id.authorImageView) as SimpleDraweeView
        val groupLayout: View = itemView.findViewById(R.id.groupLayout)
        val groupNameTextView = itemView.findViewById(R.id.groupNameTextView) as TextView
        val groupImageView = itemView.findViewById(R.id.groupImageView) as SimpleDraweeView
        val description = itemView.findViewById(R.id.descriptionTextView) as TextView

    }

    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val messageTextView = itemView.findViewById(R.id.messageTextView) as TextView
        val retryButton = itemView.findViewById(R.id.retryButton) as Button

    }

    enum class LoadMode {
        REPLACE,
        APPEND
    }

    private val application = activity.application as Application
    private var posts = application.postsStore.restore(collection)
    private val postsProvider = PostsProvider(application, collection, this)
    private var loadMode = LoadMode.REPLACE
    private var retriesLeft: Int? = null
    private var lastPostsLoadConnectionError: ConnectionError? = null
    private var isActive = false


    fun activate(load: Boolean) {
        isActive = true

        if (load) {
            loadMode = LoadMode.REPLACE
            load()
        }
    }

    fun deactivate() {
        isActive = false
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 1) {
            val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.loading, viewGroup, false)

            return LoadingViewHolder(view).apply {
                retryButton.setOnClickListener { load() }
            }
        }

        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.post, viewGroup, false)

        return PostViewHolder(view).apply {
            val drawable = ProgressBarDrawable().apply {
                color = ContextCompat.getColor(activity, R.color.accent)
                barWidth = activity.resources.getDimensionPixelSize(R.dimen.image_progress_bar_height)
            }

            postImageView.hierarchy.setProgressBarImage(drawable)
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        if (position == posts.size && shouldShowMessageCard() ) {
            val loadingViewHolder = viewHolder as LoadingViewHolder

            loadingViewHolder.messageTextView.apply {
                text = lastPostsLoadConnectionError?.details
                        ?: retriesLeft?.let { activity.getString(R.string.loading_with_retry, it) }
                                ?: activity.getString(R.string.loading)
            }

            loadingViewHolder.retryButton.visibility = if (lastPostsLoadConnectionError == null) View.GONE else View.VISIBLE

            return
        }

        val post = posts[position]
        val postViewHolder = viewHolder as PostViewHolder

        postViewHolder.postImageView.apply {
            if (tag != post.uri) {
                tag = post.uri
                controller = Fresco.newDraweeControllerBuilder().
                        setUri(post.uri).
                        setAutoPlayAnimations(true).
                        setOldController(postViewHolder.postImageView.controller).
                        setTapToRetryEnabled(true).
                        build()
                setOnClickListener {
                    FullscreenImageActivity.start(activity, post.uri, it)
                }
            }
        }

        postViewHolder.description.apply {
            visibility = if (post.description.isBlank()) View.GONE else View.VISIBLE
            text = post.description
        }

        postViewHolder.repostButton.apply {
            visibility = if (collection != PostCollection.Me) View.VISIBLE else View.GONE
            isEnabled = post.repostState == Post.RepostState.NOT_REPOSTED

            setOnClickListener {
                val errorListener: (ConnectionError) -> Unit = { err ->
                    if (err.isForbidden()) {
                        handleForbidden()
                    } else {
                        notifyDataSetChanged()
                        showErrorToast(err)
                    }
                }
                application.api.repost(post, errorListener, this@PostsAdapter::notifyDataSetChanged)
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
            setOnClickListener {
                val sendIntent = Intent(Intent.ACTION_SEND)
                sendIntent.type = "text/plain"
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
            postViewHolder.authorImageView.setImageURI(post.author.imageUri, null)
            postViewHolder.authorLayout.setOnClickListener {
                val collection = SubdomainPostCollection(post.author.name)
                activity.setActivePostsAdapter(collection, true)
            }

            if (group != null) {
                postViewHolder.groupLayout.visibility = View.VISIBLE
                postViewHolder.groupNameTextView.text = group.name
                postViewHolder.groupImageView.setImageURI(group.imageUri, null)
                postViewHolder.groupLayout.setOnClickListener {
                    val collection = SubdomainPostCollection(group.name)
                    activity.setActivePostsAdapter(collection, true)
                }
            } else {
                postViewHolder.groupLayout.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = posts.size + if (shouldShowMessageCard()) 1 else 0

    override fun getItemViewType(position: Int) = if (shouldShowMessageCard() && position == posts.size) 1 else 0

    override fun onPostsLoaded(posts: Collection<Post>) {
        this.posts = if (loadMode == LoadMode.REPLACE) posts.toList() else this.posts + posts

        application.postsStore.store(collection, this.posts)
        notifyDataSetChanged()
    }

    override fun onPostsLoadError(connectionError: ConnectionError) {
        lastPostsLoadConnectionError = connectionError

        if (connectionError.isForbidden()) {
            handleForbidden()
        }

        notifyDataSetChanged()
    }

    override fun onPostsLoadRetrying(retriesLeft: Int) {
        this.retriesLeft = retriesLeft
        notifyDataSetChanged()
    }

    fun maybeLoadMore(visibleItemPosition: Int) {
        if (posts.size - visibleItemPosition in 1..9) {
            loadMode = LoadMode.APPEND
            load()
        }
    }

    private fun load() {
        retriesLeft = null
        lastPostsLoadConnectionError = null
        postsProvider.loadPosts(if (loadMode == LoadMode.REPLACE) null else posts.lastOrNull())
        notifyDataSetChanged()
    }

    private fun shouldShowMessageCard() = postsProvider.isLoading() || lastPostsLoadConnectionError != null

    private fun showErrorToast(error: ConnectionError) {
        if (isActive) {
            val msg = activity.getString(R.string.network_error, error.details)
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleForbidden() {
        Toast.makeText(activity, R.string.forbidden, Toast.LENGTH_SHORT).show()
        application.logOut()
    }

}
