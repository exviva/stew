package net.stew.stew

import android.net.Uri
import android.os.AsyncTask
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

    private val runningTasks = arrayListOf<Task>()

    fun logIn(userName: String, password: String, errorListener: (ConnectionError) -> Unit,
              listener: (String?, String?, String?) -> Unit) {

        val loginPageConnection = connect(loginPath)
        executeRequest(loginPageConnection, errorListener) {
            val sessionId = loginPageConnection.response().cookie("soup_session_id")
            val authenticityToken = it.select("input[name=authenticity_token]").attr("value")

            val connection = connect(loginPath).
                method(Connection.Method.POST).
                cookie("soup_session_id", sessionId).
                data("login", userName).
                data("password", password).
                data("authenticity_token", authenticityToken)

            executeRequest(connection, errorListener) { doc ->
                val userIdCookie = connection.response().cookie("soup_user_id")
                val sessionIdCookie = connection.response().cookie("soup_session_id")
                val csrfToken = doc.select("meta[name=csrf-token]").attr("content")
                listener(userIdCookie, sessionIdCookie, csrfToken)
            }
        }
    }

    fun fetchPosts(collection: PostCollection, lastPost: Post?, errorListener: (ConnectionError) -> Unit,
                   listener: (Collection<Post>) -> Unit): Task {

        val subdomain = (collection.subdomain ?: "www").replace(":current_user", application.currentSession!!.userName)
        var path = collection.path ?: "/"

        if (lastPost != null && collection.subdomain != null) {
            path += "since/${lastPost.id}"
        }

        val connection = connect(path, subdomain, requireAuthentication = collection.requiresAuthentication())

        if (lastPost != null && collection.subdomain == null) {
            connection.data("since", lastPost.id.toString())
        }

        return executeRequest(connection, errorListener, retries = 2) {
            listener(parsePosts(it))
        }
    }

    fun repost(post: Post, errorListener: (ConnectionError) -> Unit, listener: () -> Unit) {
        val connection = connect("/remote/repost").
            method(Connection.Method.POST).
            data("parent_id", post.id.toString())
        val wrappingErrorListener: (ConnectionError) -> Unit = {
            post.repostState = Post.RepostState.NOT_REPOSTED
            errorListener(it)
        }

        post.repostState = Post.RepostState.REPOSTING
        executeRequest(connection, wrappingErrorListener) {
            post.repostState = Post.RepostState.REPOSTED
            listener()
        }
    }

    fun clear() {
        runningTasks.forEach { it.cancel(true) }
        runningTasks.clear()
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

    private fun executeRequest(connection: Connection, errorListener: (ConnectionError) -> Unit,
                               retries: Int = 0, listener: (Document) -> Unit): Task {
        val task = Task(connection, retries) { task, connectionError, document ->
            if (connectionError?.retriesLeft != null) {
                errorListener(connectionError)
                return@Task
            }

            runningTasks.remove(task)

            if (connectionError != null) {
                errorListener(connectionError)
            } else {
                listener(document!!)
            }
        }

        task.execute()
        runningTasks.add(task)

        return task
    }

    private fun parsePosts(document: Document): Collection<Post> {
        return document.select(":not(.gallery-images) > .post").map {
            val blockRepost = it.hasClass("hide-repost")
            val id = it.attr("id").replace(Regex("[^0-9]"), "").toInt()
            val content = when {
                it.hasClass("post_image") ->
                    Post.Content(it.select(".content img").attr("src"), Post.Content.Type.Image)
                it.hasClass("post_video") && it.select(".content video").isNotEmpty() ->
                    Post.Content(it.select(".content video").attr("src"), Post.Content.Type.Video)
                else ->
                    Post.Content(it.select(".actionbar .permalink a").attr("href"), Post.Content.Type.Other)
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

            Post(id, content, description, author, group, repostState)
        }
    }

}

class Task(private val connection: Connection, private var retries: Int, val listener: (Task, ConnectionError?, Document?) -> Unit) :
        AsyncTask<Void, Int, Pair<ConnectionError?, Document?>>() {
    override fun doInBackground(vararg params: Void?): Pair<ConnectionError?, Document?> {
        var responseStatus: ConnectionError? = null
        var document: Document? = null

        while (true) {
            try {
                val originalUrl = connection.request().url()
                val response = connection.execute()

                if (originalUrl.path != Api.loginPath && response.url().path == Api.loginPath) {
                    responseStatus = ConnectionError(null, 403)
                } else {
                    document = response.parse()
                }
            } catch (e: IOException) {
                val statusCode = (e as? HttpStatusException)?.statusCode

                if (retries > 0 && statusCode?.let { it >= 500 } == true) {
                    retries--
                    publishProgress(retries)
                    continue
                }

                responseStatus = ConnectionError(e, statusCode)
            }

            return Pair(responseStatus, document)
        }
    }

    override fun onProgressUpdate(vararg values: Int?) {
        listener(this, ConnectionError(null, null, values[0]), null)
    }

    override fun onPostExecute(pair: Pair<ConnectionError?, Document?>) {
        val responseStatus = pair.first
        val document = pair.second

        listener(this, responseStatus, document)
    }
}

open class ConnectionError(private val error: Exception?, private val statusCode: Int?, val retriesLeft: Int? = null) {

    val details by lazy { listOfNotNull(statusCode?.toString(), error?.message).joinToString(" - ") }

    fun isForbidden() = statusCode == 403

}
