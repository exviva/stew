package net.stew.stew

abstract class PostCollection {

    companion object {
        val Friends = CurrentUserPostCollection("/friends")
        val FOF = CurrentUserPostCollection("/fof")
        val Me = SubdomainPostCollection(":current_user")

        val Predefined = listOf(Friends, FOF, Me)
    }

    abstract val subdomain : String?
    abstract val path : String?

    fun ordinal() = Predefined.indexOf(this)

}

class CurrentUserPostCollection(override val path: String) : PostCollection() {
    override val subdomain : String?
        get() = null
}

class SubdomainPostCollection(override val subdomain: String) : PostCollection() {
    override val path: String?
        get() = null
}