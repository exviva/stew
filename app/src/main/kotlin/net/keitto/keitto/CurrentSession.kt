package net.keitto.keitto

data class CurrentSession(val userName: String, val userIdCookie: String, val sessionIdCookie: String, val csrfToken: String)