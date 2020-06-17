package net.stew.stew.model

import android.net.Uri

class Post(val id: Int,
           val content: Content,
           val permalink: Uri,
           val description: String,
           val author: Author,
           val group: Group?,
           var repostState: RepostState) {

    val type = content.type
    val contentUri = content.uri

    class Content(url: String, val type: Type, val text: String = "") {
        val uri = Uri.parse(url)!!

        enum class Type {
            Image,
            Video,
            Other
        }
    }

    enum class RepostState {
        NOT_REPOSTED,
        REPOSTING,
        REPOSTED,
        BLOCKED
    }

    class Author(val name: String, val imageUri: Uri)
    class Group(val name: String, val imageUri: Uri)

}