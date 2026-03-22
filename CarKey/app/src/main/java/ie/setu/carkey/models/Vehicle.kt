package ie.setu.carkey.models

data class Vehicle(
    val vehicleId: String = "",
    val make: String = "",
    val model: String = "",
    val registrationPlate: String = "",
    val currentKeyId: String? = null
)

