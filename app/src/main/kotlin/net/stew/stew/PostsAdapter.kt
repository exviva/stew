package net.stew.stew

import android.graphics.drawable.Animatable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
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

        val postImageView = itemView.findViewById(R.id.postImageView) as SimpleDraweeView
        val repostButton = itemView.findViewById(R.id.repostButton) as Button
        val authorshipLayout = itemView.findViewById(R.id.authorshipLayout)
        val authorNameTextView = itemView.findViewById(R.id.authorNameTextView) as TextView
        val authorImageView = itemView.findViewById(R.id.authorImageView) as SimpleDraweeView
        val groupNameTextView = itemView.findViewById(R.id.groupNameTextView) as TextView
        val groupImageView = itemView.findViewById(R.id.groupImageView) as SimpleDraweeView
        val description = itemView.findViewById(R.id.descriptionTextView) as TextView

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
            PostCollection.Me -> MyPostsProvider(this, application)
            else -> OthersPostsProvider(this, collection, application)
        }
        posts = application.postsStore.restore(collection)
    }

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
        val view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.post, viewGroup, false)
        val viewHolder = PostViewHolder(view)
        val progressDrawable = ProgressBarDrawable()

        progressDrawable.setColor(ContextCompat.getColor(activity, R.color.accent))
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

        postViewHolder.description.let {
            it.setVisibility(if (post.description.isBlank()) View.GONE else View.VISIBLE)
            it.setText(post.description)
        }

        postViewHolder.repostButton.let {
            it.setVisibility(if (collection != PostCollection.Me) View.VISIBLE else View.GONE)
            it.setEnabled(false)
            it.setOnClickListener {
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
                Post.RepostState.NOT_REPOSTED -> R.string.repost
                Post.RepostState.REPOSTING -> R.string.reposting
                Post.RepostState.REPOSTED -> R.string.reposted
            }
            it.setText(stringId)
        }

        if (collection == PostCollection.Me) {
            postViewHolder.authorshipLayout.setVisibility(View.GONE)
        } else {
            val groupVisibility = if (post.group != null) View.VISIBLE else View.GONE

            postViewHolder.authorshipLayout.setVisibility(View.VISIBLE)

            postViewHolder.authorNameTextView.setText(post.author.name)
            postViewHolder.authorImageView.setImageURI(post.author.imageUri)

            postViewHolder.groupNameTextView.setText(post.group?.name)
            postViewHolder.groupNameTextView.setVisibility(groupVisibility)
            postViewHolder.groupImageView.setImageURI(post.group?.imageUri)
            postViewHolder.groupImageView.setVisibility(groupVisibility)
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