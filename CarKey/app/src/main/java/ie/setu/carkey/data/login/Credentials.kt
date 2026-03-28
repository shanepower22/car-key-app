package ie.setu.carkey.data.login

data class Credentials(
    var email: String = "",
    var password: String = "",
    var remember: Boolean = false
) {
    fun isNotEmpty() = email.isNotEmpty() && password.isNotEmpty()
}
