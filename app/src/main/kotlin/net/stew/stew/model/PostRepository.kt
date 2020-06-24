package net.stew.stew.model

class PostRepository {

    private val postsByCollection = mutableMapOf<PostCollection, ArrayList<Post>>().withDefault { arrayListOf() }

    fun store(collection: PostCollection, posts: Collection<Post>) {
        postsByCollection.getValue(collection).apply {
            clear()
            addAll(posts)
        }
    }

    fun restore(collection: PostCollection) = postsByCollection.getValue(collection).toList()

    fun clear() = postsByCollection.values.forEach(ArrayList<Post>::clear)

}