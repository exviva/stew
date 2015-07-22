package net.stew.stew

import kotlin.text.Regex

data class CurrentSession(val userName: String, val userIdCookie: String, val sessionIdCookie: String, val csrfToken: String) {
    val userId: Int
        get() = userIdCookie.split(Regex("-"), 2)[0].toInt()
}