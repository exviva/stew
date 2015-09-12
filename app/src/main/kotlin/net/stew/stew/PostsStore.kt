package net.stew.stew

import java.util.ArrayList
import java.util.HashMap

class PostsStore {

    private val postsByCollection = HashMap<PostCollection, ArrayList<Post>>()

    fun store(collection: PostCollection, posts: Collection<Post>) {
        postsByCollection[collection].clear()
        postsByCollection[collection].addAll(posts)
    }

    fun restore(collection: PostCollection) : List<Post> {
        if (!(collection in postsByCollection)) {
            postsByCollection.put(collection, arrayListOf<Post>())
        }

        return postsByCollection[collection].toList()
    }

    fun clear() {
        postsByCollection.forEach { it.value.clear() }
    }

}