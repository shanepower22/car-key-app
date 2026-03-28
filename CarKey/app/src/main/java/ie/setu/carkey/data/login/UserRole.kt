package ie.setu.carkey.data.login

enum class UserRole {
    DRIVER,   // Can lock/unlock their assigned vehicle
    MANAGER   // Can provision, assign, and revoke keys
}
