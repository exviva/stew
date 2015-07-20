package net.keitto.keitto

import android.net.Uri
import android.os.AsyncTask
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.*

object Api {
    fun fetchPosts(listener: (Collection<Post>) -> Unit) {
        get("/friends") {
            val posts = it.select(".post_image").
                map {
                    val id = it.attr("id").replaceAll("[^0-9]", "").toInt()
                    val src = it.select(".imagecontainer img").attr("src")
                    Post(id, Uri.parse(src))
                }
            listener(posts)
        }
    }

    fun repost(post: Post) {
        val data = HashMap<String, String>()
        data.put("parent_id", post.id.toString())
        post("/remote/repost", data)
    }

    private fun get(path: String, listener: (Document) -> Unit) {
        dispatch(Method.GET, path, listener = listener)
    }

    private fun post(path: String, data: Map<String, String>) {
        dispatch(Method.POST, path, data)
    }

    private enum class Method { GET, POST }

    private fun dispatch(method: Method, path: String, data: Map<String, String>? = null, listener: ((Document) -> Unit)? = null) {
        val task = object: AsyncTask<Void, Void, Document>() {
            override fun doInBackground(vararg params: Void): Document {
                val connection = Jsoup.connect("http://www.soup.io${path}").
                    cookie("soup_user_id", "XXX").
                    cookie("soup_session_id", "XXX").
                    header("X-CSRF-Token", "XXX")

                if (data != null) {
                    connection.data(data)
                }

                return when (method) {
                    Method.GET -> connection.get()
                    Method.POST -> connection.post()
                }
            }

            override fun onPostExecute(document: Document) {
                if (listener != null) {
                    listener(document)
                }
            }
        }
        task.execute()
    }

}
