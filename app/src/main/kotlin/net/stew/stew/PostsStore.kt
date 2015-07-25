package net.stew.stew

import java.util.HashMap

class PostsStore {

    private val postsByCollection = HashMap(PostCollection.values().map { it to arrayListOf<Post>() }.toMap())

    fun store(collection: PostCollection, posts: Collection<Post>) {
        postsByCollection[collection].clear()
        postsByCollection[collection].addAll(posts)
    }

    fun restore(collection: PostCollection) = postsByCollection[collection].toList()
}