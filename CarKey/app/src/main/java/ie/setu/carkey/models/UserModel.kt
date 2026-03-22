package ie.setu.carkey.models

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.DRIVER
)

enum class UserRole {
    DRIVER,    // Can only lock/unlock their assigned vehicle
    MANAGER    // Can provision, assign and revoke keys
}