package net.stew.stew

import android.content.Context
import android.content.Intent
import com.facebook.drawee.backends.pipeline.Fresco
import net.stew.stew.model.PostsStore
import net.stew.stew.ui.LoginActivity

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
        getPreferences().edit().
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
        getPreferences().edit().
            remove("userIdCookie").
            remove("sessionIdCookie").
            remove("csrfToken").
            apply()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    fun requireCurrentSession(): Boolean {
        if (currentSession == null) {
            logOut()
            return false
        }

        return true
    }

    private fun restoreCurrentSession() {
        getPreferences().run {
            val userName = getString("userName", null)
            val userIdCookie = getString("userIdCookie", null)
            val sessionIdCookie = getString("sessionIdCookie", null)
            val csrfToken = getString("csrfToken", null)

            if (userName != null && userIdCookie != null && sessionIdCookie != null && csrfToken != null) {
                setCurrentSession(userName, userIdCookie, sessionIdCookie, csrfToken)
            }

            previousUserName = userName
        }
    }

    private fun getPreferences() = getSharedPreferences("Application", Context.MODE_PRIVATE)

}