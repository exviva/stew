package net.stew.stew

import android.net.Uri
import android.os.AsyncTask
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import kotlin.text.Regex

class Api(val application: Application) {

    private val runningTasks = arrayListOf<AsyncTask<Void, Void, Document?>>()

    fun logIn(userName: String, password: String, errorListener: () -> Unit, listener: (String?, String?, String?) -> Unit) {
        val loginPageConnection = connect("/login", useSsl = true)
        executeRequest(loginPageConnection, errorListener) {
            val sessionId = loginPageConnection.response().cookie("soup_session_id")
            val authenticityToken = it.select("input[name=authenticity_token]").attr("value")

            val connection = connect("/login", useSsl = true).
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

    fun fetchPosts(collection: PostCollection, lastPost: Post?, errorListener: () -> Unit, listener: (Collection<Post>) -> Unit):
        AsyncTask<Void, Void, Document?> {
        val path = when (collection) {
            PostCollection.FRIENDS -> "/friends"
            PostCollection.FOF -> "/fof"
            PostCollection.ME -> throw IllegalArgumentException("Call fetchMyPosts instead")
        }

        val connection = connect(path)

        if (lastPost != null) {
            connection.data("since", lastPost.id.toString())
        }

        return executeRequest(connection, errorListener) {
            listener(parsePosts(it))
        }
    }

    fun fetchMyPosts(lastPost: Post?, cookies: Map<String, String>?, errorListener: () -> Unit,
        listener: (Collection<Post>, Map<String, String>) -> Unit): AsyncTask<Void, Void, Document?> {
        val path = "/" + if (lastPost != null) "since/${lastPost.id}" else ""
        val subdomain = application.currentSession!!.userName

        val connection = connect(path, subdomain)

        if (lastPost != null) {
            connection.data("mode", "own")
        }

        if (cookies != null) {
            connection.cookies(cookies)
        }

        return executeRequest(connection, errorListener) {
            listener(parsePosts(it), connection.response().cookies())
        }
    }

    fun repost(post: Post, errorListener: () -> Unit, listener: () -> Unit) {
        val connection = connect("/remote/repost").
            method(Connection.Method.POST).
            data("parent_id", post.id.toString())
        val wrappingErrorListener = {
            post.repostState = Post.RepostState.NOT_REPOSTED
            errorListener()
        }

        post.repostState = Post.RepostState.REPOSTING
        executeRequest(connection, wrappingErrorListener) {
            post.repostState = Post.RepostState.REPOSTED
            listener()
        }
    }

    fun cancelRunningRequests() {
        runningTasks.forEach { it.cancel(true) }
        runningTasks.clear()
    }

    private fun connect(path: String, subdomain: String? = null, useSsl: Boolean = false): Connection {
        val connection = Jsoup.connect("http${if (useSsl) "s" else ""}://${subdomain ?: "www"}.soup.io${path}").
            timeout(10000)
        val currentSession = application.currentSession

        if (currentSession != null) {
            connection.cookie("soup_user_id", currentSession.userIdCookie).
            cookie("soup_session_id", currentSession.sessionIdCookie).
            header("X-CSRF-Token", currentSession.csrfToken)
        }

        return connection
    }

    private fun executeRequest(connection: Connection, errorListener: () -> Unit, listener: ((Document) -> Unit)):
        AsyncTask<Void, Void, Document?> {
        val task = object: AsyncTask<Void, Void, Document?>() {
            override fun doInBackground(vararg params: Void?): Document? {
                try {
                    return connection.execute().parse()
                } catch (e: IOException) {
                    return null
                }
            }

            override fun onPostExecute(document: Document?) {
                runningTasks.remove(this)
                if (document != null) {
                    listener(document)
                } else {
                    errorListener()
                }
            }
        }
        task.execute()
        runningTasks.add(task)
        return task
    }

    private fun parsePosts(document: Document): Collection<Post> {
        return document.select(".post_image").map {
            val id = it.attr("id").replace(Regex("[^0-9]"), "").toInt()
            val src = it.select(".imagecontainer img").attr("src")
            val isReposted = it.select(".reposted_by .user${application.currentSession!!.userId}").isNotEmpty()
            Post(id, Uri.parse(src), if (isReposted) Post.RepostState.REPOSTED else Post.RepostState.NOT_REPOSTED)
        }
    }

}
