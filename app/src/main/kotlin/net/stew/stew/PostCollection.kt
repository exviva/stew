package net.stew.stew

abstract class PostCollection {

    companion object {
        val Friends = CurrentUserPostCollection("/friends")
        private val FOF = CurrentUserPostCollection("/fof")
        private val Everyone = CurrentUserPostCollection("/everyone")
        val Me = SubdomainPostCollection(":current_user")

        val Predefined = listOf(Friends, FOF, Everyone, Me)
    }

    abstract val subdomain : String?
    abstract val path : String?

    fun ordinal() = Predefined.indexOf(this)
    fun isPredefined() = ordinal() != -1

    override fun equals(other: Any?): Boolean {
        return other is PostCollection && subdomain == other.subdomain && path == other.path
    }

    override fun hashCode() = (subdomain?.hashCode() ?: 0) * 13 + (path?.hashCode() ?: 0) * 7

}

class CurrentUserPostCollection(override val path: String) : PostCollection() {
    override val subdomain : String?
        get() = null
}

class SubdomainPostCollection(override val subdomain: String) : PostCollection() {
    override val path: String?
        get() = null
}