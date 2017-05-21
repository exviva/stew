package net.stew.stew

import android.content.Context
import com.facebook.drawee.backends.pipeline.Fresco

class Application : android.app.Application() {

    var currentSession: CurrentSession? = null
    var previousUserName: String? = null
    val api = Api(this)
    val postsStore = PostsStore()

    override fun onCreate() {
        super.onCreate()

        Fresco.initialize(this)
        restoreCurrentSession()
    }

    fun setCurrentSession(userName: String, userIdCookie: String, sessionIdCookie: String, csrfToken: String) {
        previousUserName = userName
        currentSession = CurrentSession(userName, userIdCookie, sessionIdCookie, csrfToken)
        val preferences = getPreferences()
        preferences.edit().
            putString("userName", userName).
            putString("userIdCookie", userIdCookie).
            putString("sessionIdCookie", sessionIdCookie).
            putString("csrfToken", csrfToken).
            apply()
    }

    fun logOut() {
        api.clear()
        postsStore.clear()
        currentSession = null
        val preferences = getPreferences()
        preferences.edit().
            remove("userIdCookie").
            remove("sessionIdCookie").
            remove("csrfToken").
            apply()
    }

    private fun restoreCurrentSession() {
        val preferences = getPreferences()
        val userName = preferences.getString("userName", null)
        val userIdCookie = preferences.getString("userIdCookie", null)
        val sessionIdCookie = preferences.getString("sessionIdCookie", null)
        val csrfToken = preferences.getString("csrfToken", null)

        if (userName != null && userIdCookie != null && sessionIdCookie != null && csrfToken != null) {
            setCurrentSession(userName, userIdCookie, sessionIdCookie, csrfToken)
        }

        previousUserName = userName
    }

    private fun getPreferences() = getSharedPreferences("Application", Context.MODE_PRIVATE)

}