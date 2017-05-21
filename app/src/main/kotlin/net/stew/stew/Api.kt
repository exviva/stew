package net.stew.stew

import android.net.Uri
import android.os.AsyncTask
import org.jsoup.Connection
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

class Api(val application: Application) {

    companion object {
        val loginPath = "/login"
    }

    private val runningTasks = arrayListOf<AsyncTask<Void, Void, Pair<ResponseStatus, Document?>>>()

    fun logIn(userName: String, password: String, errorListener: (ResponseStatus) -> Unit, listener: (String?, String?, String?) -> Unit) {
        val loginPageConnection = connect(loginPath, useSsl = true)
        executeRequest(loginPageConnection, errorListener) {
            val sessionId = loginPageConnection.response().cookie("soup_session_id")
            val authenticityToken = it.select("input[name=authenticity_token]").attr("value")

            val connection = connect(loginPath, useSsl = true).
                method(Connection.Method.POST).
                cookie("soup_session_id", sessionId).
                data("login", userName).
                data("password", password).
                data("authenticity_token", authenticityToken)

            executeRequest(connection, errorListener) {
                val userIdCookie = connection.response().cookie("soup_user_id")
                val sessionIdCookie = connection.response().cookie("soup_session_id")
                val csrfToken = it.select("meta[name=csrf-token]").attr("content")
                listener(userIdCookie, sessionIdCookie, csrfToken)
            }
        }
    }

    fun fetchPosts(collection: PostCollection, lastPost: Post?, errorListener: (ResponseStatus) -> Unit,
        listener: (Collection<Post>) -> Unit): AsyncTask<Void, Void, Pair<ResponseStatus, Document?>> {

        val subdomain = (collection.subdomain ?: "www").replace(":current_user", application.currentSession!!.userName)
        var path = collection.path ?: "/"

        if (lastPost != null && collection.subdomain != null) {
            path += "since/${lastPost.id}"
        }

        val connection = connect(path, subdomain)

        if (lastPost != null && collection.subdomain == null) {
            connection.data("since", lastPost.id.toString())
        }

        return executeRequest(connection, errorListener) {
            listener(parsePosts(it))
        }
    }

    fun repost(post: Post, errorListener: (ResponseStatus) -> Unit, listener: () -> Unit) {
        val connection = connect("/remote/repost").
            method(Connection.Method.POST).
            data("parent_id", post.id.toString())
        val wrappingErrorListener: (ResponseStatus) -> Unit = {
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

    private fun connect(path: String, subdomain: String? = null, useSsl: Boolean = false): Connection {
        val connection = Jsoup.connect("http${if (useSsl) "s" else ""}://${subdomain ?: "www"}.soup.io$path").
            timeout(10000)
        val currentSession = application.currentSession

        if (currentSession != null) {
            connection.cookie("soup_user_id", currentSession.userIdCookie).
            cookie("soup_session_id", currentSession.sessionIdCookie).
            header("X-CSRF-Token", currentSession.csrfToken)
        }

        return connection
    }

    private fun executeRequest(connection: Connection, errorListener: (ResponseStatus) -> Unit, listener: (Document) -> Unit):
            AsyncTask<Void, Void, Pair<ResponseStatus, Document?>> {
        val task = Task(connection, errorListener) { task, document ->
            runningTasks.remove(task)
            listener(document)
        }
        task.execute()
        runningTasks.add(task)
        return task
    }

    private fun parsePosts(document: Document): Collection<Post> {
        return document.select(":not(.gallery-images) > .post_image").map {
            val blockRepost = it.hasClass("hide-repost")
            val id = it.attr("id").replace(Regex("[^0-9]"), "").toInt()
            val src = it.select(".imagecontainer img").attr("src")
            val isReposted = it.select(".reposted_by .user${application.currentSession!!.userId}").isNotEmpty()
            val repostState = if (blockRepost) Post.RepostState.BLOCKED else
                if (isReposted) Post.RepostState.REPOSTED else Post.RepostState.NOT_REPOSTED

            val description = it.select(".description").text()

            val authorship = it.select(".icon.author")
            val authorElement = authorship.select("a.url.user").first() ?: authorship.select("a.url").first()
            val authorImageUri = Uri.parse(authorElement.select("img").attr("src"))
            val authorName = authorship.select(".bubble h4 a").text()
            val author = Post.Author(authorName, authorImageUri)

            val groupImageUrl = authorship.select("a.url.group img").attr("src")
            val groupImageUri = if (groupImageUrl.isNotBlank()) Uri.parse(groupImageUrl) else null
            val groupName = authorship.select(".bubble .group a").text()
            val group = if (groupName.isNotBlank() && groupImageUri != null) Post.Group(groupName, groupImageUri) else null

            Post(id, Uri.parse(src), description, author, group, repostState)
        }
    }

}

private class Task(val connection: Connection, val errorListener: (ResponseStatus) -> Unit, val listener: (Task, Document) -> Unit):
        AsyncTask<Void, Void, Pair<ResponseStatus, Document?>>() {
    override fun doInBackground(vararg params: Void?): Pair<ResponseStatus, Document?> {
        var responseStatus: ResponseStatus = ResponseStatus.OK
        var document: Document? = null

        try {
            val originalUrl = connection.request().url()
            val response = connection.execute()
            if (originalUrl.path != Api.loginPath && response.url().path == Api.loginPath) {
                responseStatus = ResponseStatus.FORBIDDEN
            } else {
                document = response.parse()
            }
        } catch (e: IOException) {
            responseStatus = if (e is HttpStatusException && e.statusCode == 403)
                ResponseStatus.FORBIDDEN else ResponseStatus.SERVER_ERROR
        }

        return Pair(responseStatus, document)
    }

    override fun onPostExecute(pair: Pair<ResponseStatus, Document?>) {
        val responseStatus = pair.first
        val document = pair.second

        if (responseStatus == ResponseStatus.OK && document != null) {
            listener(this, document)
        } else {
            errorListener(responseStatus)
        }
    }
}

enum class ResponseStatus {
    OK,
    FORBIDDEN,
    SERVER_ERROR
}