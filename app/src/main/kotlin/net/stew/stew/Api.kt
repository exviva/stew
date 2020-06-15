package net.stew.stew

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import net.stew.stew.model.Post
import net.stew.stew.model.PostCollection
import org.jsoup.Connection
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

class Api(private val application: Application) {

    companion object {
        const val loginPath = "/login"
    }

    private var scope = CoroutineScope(Dispatchers.IO)

    data class Retry(val count: Int, val callback: (Int) -> Unit)
    sealed class Response<out T> {
        data class Success<out T>(val data: T) : Response<T>()
        data class Error(private val error: Exception?, private val statusCode: Int?) : Response<Nothing>() {
            fun isForbidden() = statusCode?.let { it == 403 } ?: false
            val details by lazy { listOfNotNull(statusCode?.toString(), error?.message).joinToString(" - ") }
        }
    }
    data class LoginResponse(val userIdCookie: String?, val sessionIdCookie: String?, val csrfToken: String?)

    suspend fun logIn(userName: String, password: String): Response<LoginResponse> {
        val loginPageConnection = connect(loginPath, requireAuthentication = false)
        val res: Response<Pair<String?, String?>> = executeRequest(loginPageConnection) {
            val sessionId = loginPageConnection.response().cookie("soup_session_id")
            val authenticityToken = it.select("input[name=authenticity_token]").attr("value")

            Pair(sessionId, authenticityToken)
        }

        return when (res) {
            is Response.Success -> {
                val (sessionId, authenticityToken) = res.data

                if (sessionId == null || authenticityToken == null) {
                    Response.Error(Exception("Missing session_id or authenticity_token"), 400)
                } else {
                    val connection = connect(loginPath)
                            .method(Connection.Method.POST)
                            .cookie("soup_session_id", sessionId)
                            .data("login", userName)
                            .data("password", password)
                            .data("authenticity_token", authenticityToken)

                    executeRequest(connection) { doc ->
                        val userIdCookie = connection.response().cookie("soup_user_id")
                        val sessionIdCookie = connection.response().cookie("soup_session_id")
                        val csrfToken = doc.select("meta[name=csrf-token]").attr("content")
                        LoginResponse(userIdCookie, sessionIdCookie, csrfToken)
                    }
                }
            }
            is Response.Error -> res
        }
    }

    suspend fun fetchPosts(collection: PostCollection, lastPost: Post?, retry: Retry): Response<Collection<Post>> {
        val subdomain = (collection.subdomain ?: "www").replace(":current_user", application.currentSession!!.userName)
        var path = collection.path ?: "/"

        if (lastPost != null && collection.subdomain != null) {
            path += "since/${lastPost.id}"
        }

        val connection = connect(path, subdomain, requireAuthentication = collection.requiresAuthentication())

        if (lastPost != null && collection.subdomain == null) {
            connection.data("since", lastPost.id.toString())
        }

        return executeRequest(connection, retry, this::parsePosts)
    }

    suspend fun repost(post: Post): Response<Unit> {
        val connection = connect("/remote/repost").
            method(Connection.Method.POST).
            data("parent_id", post.id.toString())

        return executeRequest(connection) {}
    }

    fun clear() {
        scope.cancel("Clear")
        scope = CoroutineScope(Dispatchers.IO)
    }

    private fun connect(path: String, subdomain: String? = null, requireAuthentication: Boolean = true): Connection {
        val connection = Jsoup.connect("https://${subdomain ?: "www"}.soup.io$path")
        val currentSession = application.currentSession

        if (requireAuthentication && currentSession != null) {
            connection.cookie("soup_user_id", currentSession.userIdCookie).
                cookie("soup_session_id", currentSession.sessionIdCookie).
                header("X-CSRF-Token", currentSession.csrfToken)
        }

        return connection
    }

    private suspend fun <T> executeRequest(connection: Connection,
                                           retry: Retry? = null,
                                           callback: (Document) -> T): Response<T> {
        for (r in (retry?.count ?: 0) downTo 0) {
            try {
                val originalUrl = connection.request().url()
                val response = withContext(scope.coroutineContext) { connection.execute() }

                return if (originalUrl.path != loginPath && response.url().path == loginPath) {
                    Response.Error(null, 403)
                } else {
                    val document = withContext(scope.coroutineContext) { response.parse() }
                    Response.Success(callback(document))
                }
            } catch (e: IOException) {
                val statusCode = (e as? HttpStatusException)?.statusCode

                if (r > 0 && statusCode?.let { it >= 500 } == true) {
                    retry?.callback?.invoke(r)
                } else {
                    return Response.Error(e, statusCode)
                }
            }
        }

        throw IllegalStateException("Unreachable code")
    }

    private fun parsePosts(document: Document): Collection<Post> {
        return document.select(":not(.gallery-images) > .post").map {
            val blockRepost = it.hasClass("hide-repost")
            val id = it.attr("id").replace(Regex("[^0-9]"), "").toInt()
            val permalink = it.select(".actionbar .permalink a").attr("href")
            val content = when {
                it.hasClass("post_image") ->
                    Post.Content(
                            it.select(".content img").attr("src"),
                            Post.Content.Type.Image)
                it.hasClass("post_video") && it.select(".content video").isNotEmpty() ->
                    Post.Content(
                            it.select(".content video").attr("src"),
                            Post.Content.Type.Video)
                else ->
                    Post.Content(
                            permalink,
                            Post.Content.Type.Other,
                            it.select(".content").text())
            }
            val isReposted = it.select(".reposted_by .user${application.currentSession!!.userId}").isNotEmpty()
            val repostState = when {
                blockRepost -> Post.RepostState.BLOCKED
                isReposted -> Post.RepostState.REPOSTED
                else -> Post.RepostState.NOT_REPOSTED
            }

            val description = it.select(".description").text()

            val authorship = it.select(".icon.author")
            val authorElement = authorship.select("a.url.user").first()
                    ?: authorship.select("a.url").first()
            val authorImageUri = Uri.parse(authorElement.select("img").attr("src"))
            val authorName = authorship.select(".bubble h4 a").text()
            val author = Post.Author(authorName, authorImageUri)

            val groupImageUrl = authorship.select("a.url.group img").attr("src")
            val groupImageUri = if (groupImageUrl.isNotBlank()) Uri.parse(groupImageUrl) else null
            val groupName = authorship.select(".bubble .group a").text()
            val group = if (groupName.isNotBlank() && groupImageUri != null) Post.Group(groupName, groupImageUri) else null

            Post(id, content, Uri.parse(permalink), description, author, group, repostState)
        }
    }

}
