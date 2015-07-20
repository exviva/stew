package net.keitto.keitto

data class CurrentSession(val userIdCookie: String, val sessionIdCookie: String, val csrfToken: String)