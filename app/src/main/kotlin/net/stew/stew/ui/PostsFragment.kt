package net.stew.stew.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_posts.*
import net.stew.stew.R
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_posts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val collection = requireArguments().run {
            val index = getInt("index")

            if (index >= 0) PostCollection.Predefined[index]
            else SubdomainPostCollection(getString("subdomain")!!)
        }
        val adapter = PostsAdapter(activity as MainActivity, collection).apply { load() }
        val layoutManager = LinearLayoutManager(context)

        postsView.apply {
            this.adapter = adapter
            this.layoutManager = layoutManager
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                    post { adapter.maybeLoadMore(lastVisibleItemPosition) }
                }
            })
        }
    }

}