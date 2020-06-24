package net.stew.stew.ui

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.drawable.ProgressBarDrawable
import kotlinx.coroutines.launch
import net.stew.stew.Api
import net.stew.stew.Application
import net.stew.stew.R
import net.stew.stew.databinding.LoadingBinding
import net.stew.stew.databinding.PostBinding
import net.stew.stew.model.Post
import net.stew.stew.model.PostCollection
import net.stew.stew.model.SubdomainPostCollection

class PostsAdapter(private val activity: MainActivity, private val collection: PostCollection) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        val ViewTypeLoading = Post.Content.Type.values().size
    }

    class PostViewHolder(val binding: PostBinding) : RecyclerView.ViewHolder(binding.root)
    class LoadingViewHolder(val binding: LoadingBinding) : RecyclerView.ViewHolder(binding.root)

    private val application = activity.application as Application
    private var posts = application.postRepository.restore(collection)
    private var retriesLeft: Int? = null
    private var lastPostsLoadError: Api.Response.Error? = null
    private var isLoading = false

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int) = when (viewType) {
        ViewTypeLoading -> LoadingBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false).run {
            retryButton.setOnClickListener { load() }
            LoadingViewHolder(this)
        }
        else -> PostBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false).run {
            when (viewType) {
                Post.Content.Type.Image.ordinal -> postImageView.apply {
                    visibility = View.VISIBLE
                    hierarchy.setProgressBarImage(ProgressBarDrawable().apply {
                        color = ContextCompat.getColor(activity, R.color.accent)
                        barWidth = activity.resources.getDimensionPixelSize(R.dimen.image_progress_bar_height)
                    })
                }
                Post.Content.Type.Video.ordinal -> postVideoView.visibility = View.VISIBLE
                Post.Content.Type.Other.ordinal -> otherTextView.visibility = View.VISIBLE
            }
            PostViewHolder(this)
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        when (viewHolder) {
            is LoadingViewHolder -> onBindLoadingViewHolder(viewHolder)
            is PostViewHolder -> onBindPostViewHolder(posts[position], viewHolder)
        }
    }

    private fun onBindLoadingViewHolder(viewHolder: LoadingViewHolder) = viewHolder.binding.apply {
        messageTextView.text = lastPostsLoadError?.details
                ?: retriesLeft?.let { activity.getString(R.string.loading_with_retry, it) }
                        ?: activity.getString(R.string.loading)

        retryButton.visibility = if (lastPostsLoadError == null) View.GONE else View.VISIBLE
    }

    private fun onBindPostViewHolder(post: Post, viewHolder: PostViewHolder) = viewHolder.binding.apply {
        when (post.contentType) {
            Post.Content.Type.Image -> postImageView.apply {
                if (tag != post.contentUri) {
                    tag = post.contentUri
                    controller = Fresco.newDraweeControllerBuilder()
                            .setUri(post.contentUri)
                            .setAutoPlayAnimations(true)
                            .setOldController(controller)
                            .setTapToRetryEnabled(true)
                            .build()
                    setOnClickListener {
                        FullscreenImageActivity.start(activity, post.contentUri, it)
                    }
                }
            }
            Post.Content.Type.Video -> postVideoView.apply {
                if (tag != post.contentUri) {
                    tag = post.contentUri
                    setVideoURI(post.contentUri)
                    setMediaController(MediaController(activity))
                }
            }
            Post.Content.Type.Other -> otherTextView.apply {
                text = post.content.text
                setOnClickListener {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, post.contentUri))
                }
            }
        }

        descriptionTextView.apply {
            visibility = if (post.description.isBlank()) View.GONE else View.VISIBLE
            text = post.description
        }

        repostButton.apply {
            visibility = if (collection != PostCollection.Me) View.VISIBLE else View.GONE
            isEnabled = post.repostState == Post.RepostState.NOT_REPOSTED

            setOnClickListener {
                activity.lifecycleScope.launch {
                    post.repostState = Post.RepostState.REPOSTING
                    notifyDataSetChanged()

                    when (val res = application.api.repost(post)) {
                        is Api.Response.Success -> {
                            post.repostState = Post.RepostState.REPOSTED
                        }
                        is Api.Response.Error -> {
                            post.repostState = Post.RepostState.NOT_REPOSTED

                            if (res.isForbidden()) {
                                handleForbidden()
                            } else {
                                showErrorToast(res)
                            }
                        }
                    }
                    notifyDataSetChanged()
                }
            }

            val stringId = when (post.repostState) {
                Post.RepostState.NOT_REPOSTED, Post.RepostState.BLOCKED -> R.string.repost
                Post.RepostState.REPOSTING -> R.string.reposting
                Post.RepostState.REPOSTED -> R.string.reposted
            }
            setText(stringId)
        }
        shareButton.apply {
            setOnClickListener {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, post.contentUri.toString())
                }

                activity.startActivity(Intent.createChooser(sendIntent, ""))
            }

            setOnLongClickListener {
                activity.startActivity(Intent(Intent.ACTION_VIEW, post.permalink))
                true
            }
        }

        if (collection.subdomain != null) {
            authorshipLayout.visibility = View.GONE
        } else {
            val group = post.group

            authorshipLayout.visibility = View.VISIBLE

            authorNameTextView.text = post.author.name
            authorImageView.setImageURI(post.author.imageUri)
            authorLayout.setOnClickListener {
                activity.setActivePostsFragment(SubdomainPostCollection(post.author.name))
            }

            if (group != null) {
                groupLayout.visibility = View.VISIBLE
                groupNameTextView.text = group.name
                groupImageView.setImageURI(group.imageUri)
                groupLayout.setOnClickListener {
                    activity.setActivePostsFragment(SubdomainPostCollection(group.name))
                }
            } else {
                groupLayout.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = posts.size + if (shouldShowLoadingCard()) 1 else 0

    override fun getItemViewType(position: Int) = when {
        shouldShowLoadingCard() && position == posts.size -> ViewTypeLoading
        else -> posts[position].contentType.ordinal
    }

    fun maybeLoadMore(visibleItemPosition: Int) {
        if (!isLoading && posts.size - visibleItemPosition in 1..9) {
            load()
        }
    }

    fun load() {
        retriesLeft = null
        lastPostsLoadError = null

        activity.lifecycleScope.launch {
            val retry = Api.Retry(2) {
                retriesLeft = it
                notifyDataSetChanged()
            }

            isLoading = true
            notifyDataSetChanged()

            val res = application.api.fetchPosts(collection, posts.lastOrNull(), retry)
            isLoading = false

            when (res) {
                is Api.Response.Success -> {
                    posts = posts + res.data

                    application.postRepository.store(collection, posts)
                }
                is Api.Response.Error -> {
                    lastPostsLoadError = res

                    if (res.isForbidden()) {
                        handleForbidden()
                    }
                }
            }

            notifyDataSetChanged()
        }
    }

    private fun shouldShowLoadingCard() = isLoading || lastPostsLoadError != null

    private fun showErrorToast(error: Api.Response.Error) {
        val msg = activity.getString(R.string.network_error, error.details)
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }

    private fun handleForbidden() {
        Toast.makeText(activity, R.string.forbidden, Toast.LENGTH_SHORT).show()
        application.logOut()
    }

}
