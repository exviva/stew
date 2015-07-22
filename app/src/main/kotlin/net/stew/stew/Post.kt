package net.stew.stew

import android.net.Uri

data class Post(val id: Int, val uri: Uri, var repostState: Post.RepostState) {

    enum class RepostState {
        NOT_REPOSTED,
        REPOSTING,
        REPOSTED
    }

}