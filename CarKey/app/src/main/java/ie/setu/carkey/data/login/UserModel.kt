package ie.setu.carkey.data.login

data class UserModel(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = UserRole.DRIVER.name
) {
    fun toUserRole() = runCatching {
        UserRole.valueOf(role)
    }.getOrDefault(UserRole.DRIVER)
}
