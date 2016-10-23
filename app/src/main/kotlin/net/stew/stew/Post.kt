package net.stew.stew

import android.net.Uri

class Post(val id: Int,
           val uri: Uri,
           val description: String,
           val author: Post.Author,
           val group: Post.Group?,
           var repostState: Post.RepostState) {

    enum class RepostState {
        NOT_REPOSTED,
        REPOSTING,
        REPOSTED,
        BLOCKED
    }

    class Author(val name: String, val imageUri: Uri)
    class Group(val name: String, val imageUri: Uri)

}