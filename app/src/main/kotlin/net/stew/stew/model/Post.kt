package net.stew.stew.model

import android.net.Uri

class Post(val id: Int,
           val uri: Uri,
           val description: String,
           val author: Author,
           val group: Group?,
           var repostState: RepostState) {

    enum class RepostState {
        NOT_REPOSTED,
        REPOSTING,
        REPOSTED,
        BLOCKED
    }

    class Author(val name: String, val imageUri: Uri)
    class Group(val name: String, val imageUri: Uri)

}