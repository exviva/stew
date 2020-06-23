package net.stew.stew.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.stew.stew.databinding.FragmentPostsBinding
import net.stew.stew.model.PostCollection
import net.stew.stew.model.SubdomainPostCollection

class PostsFragment : Fragment() {

    companion object {
        fun newInstance(collection: PostCollection) = PostsFragment().apply {
            arguments = Bundle().apply {
                putInt("index", collection.ordinal())
                putString("subdomain", collection.subdomain)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            FragmentPostsBinding.inflate(inflater, container, false).apply {
                postsView.apply {
                    val collection = requireArguments().run {
                        val index = getInt("index")

                        if (index >= 0) PostCollection.Predefined[index]
                        else SubdomainPostCollection(getString("subdomain")!!)
                    }
                    val adapter = PostsAdapter(activity as MainActivity, collection).apply { load() }
                    val layoutManager = LinearLayoutManager(context)

                    this.adapter = adapter
                    this.layoutManager = layoutManager
                    addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                            post { adapter.maybeLoadMore(lastVisibleItemPosition) }
                        }
                    })
                }
            }.root

}