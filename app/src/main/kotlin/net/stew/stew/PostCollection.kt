package net.stew.stew

class PostCollection(val path: String) {

    companion object {
        val Friends = PostCollection("/friends")
        val FOF = PostCollection("/fof")
        val Me = PostCollection("/")

        val Predefined = listOf(Friends, FOF, Me)
    }

    fun ordinal() = Predefined.indexOf(this)

}