package net.keitto.keitto

import android.content.Context
import com.facebook.drawee.backends.pipeline.Fresco

class Application : android.app.Application() {

    var currentSession: CurrentSession? = null
    val api = Api(this)

    override fun onCreate() {
        super.onCreate()

        Fresco.initialize(this)
        restoreCurrentSession()
    }

    private fun restoreCurrentSession() {
        val preferences = getPreferences()
        val userIdCookie = preferences.getString("userIdCookie", null)
        val sessionIdCookie = preferences.getString("sessionIdCookie", null)
        val csrfToken = preferences.getString("csrfToken", null)

        if (userIdCookie != null && sessionIdCookie != null && csrfToken != null) {
            setCurrentSession(userIdCookie, sessionIdCookie, csrfToken)
        }
    }

    fun setCurrentSession(userIdCookie: String, sessionIdCookie: String, csrfToken: String) {
        currentSession = CurrentSession(userIdCookie, sessionIdCookie, csrfToken)
        val preferences = getPreferences()
        preferences.edit().
            putString("userIdCookie", userIdCookie).
            putString("sessionIdCookie", sessionIdCookie).
            putString("csrfToken", csrfToken).
            commit()
    }

    private fun getPreferences() = getSharedPreferences("Application", Context.MODE_PRIVATE)

}