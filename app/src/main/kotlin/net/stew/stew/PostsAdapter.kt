package net.stew.stew

import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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
    }

    enum class LoadMode {
        REPLACE,
        APPEND
    }

    private val application = activity.application as Application
    private var posts = application.postsStore.restore(collection)
    private var lastLoadedPageSize: Int? = null
    private val postsProvider = PostsProvider(application, collection, this)
    private var loadMode = LoadMode.REPLACE
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

            return LoadingViewHolder(view)
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
        if (position == posts.size && isLoading()) return

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

    override fun getItemCount(): Int = posts.size + if (isLoading()) 1 else 0

    override fun getItemViewType(position: Int) = if (isLoading() && position == posts.size) 1 else 0

    override fun onPostsLoaded(posts: Collection<Post>) {
        this.posts = if (loadMode == LoadMode.REPLACE) posts.toList() else this.posts + posts
        lastLoadedPageSize = posts.size

        application.postsStore.store(collection, this.posts)
        stopLoading()
    }

    override fun onPostsLoadError(connectionError: ConnectionError) {
        if (connectionError.isForbidden()) {
            handleForbidden()
        } else {
            showErrorToast(connectionError)
        }

        stopLoading()
    }

    override fun onPostsLoadRetrying(retriesLeft: Int) {
        showRetryToast(retriesLeft)
    }

    fun maybeLoadMore(visibleItemPosition: Int) {
        if (lastLoadedPageSize?.let { visibleItemPosition == itemCount - it + 3 } == true) {
            loadMode = LoadMode.APPEND
            load()
        }
    }

    private fun load() {
        postsProvider.loadPosts(if (loadMode == LoadMode.REPLACE) null else posts.lastOrNull())
        notifyDataSetChanged()
    }

    private fun stopLoading() {
        notifyDataSetChanged()
    }

    private fun isLoading() = postsProvider.isLoading()

    private fun showErrorToast(error: ConnectionError) {
        if (isActive) {
            val msg = activity.getString(R.string.network_error, error.details)
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRetryToast(retriesLeft: Int) {
        if (isActive) {
            val msg = activity.getString(R.string.retriable_network_error, retriesLeft)
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleForbidden() {
        Toast.makeText(activity, R.string.forbidden, Toast.LENGTH_SHORT).show()
        activity.logOut()
    }

}
