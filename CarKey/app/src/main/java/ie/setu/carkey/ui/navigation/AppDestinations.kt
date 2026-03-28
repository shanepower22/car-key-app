package ie.setu.carkey.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

interface AppDestination {
    val icon: ImageVector
    val label: String
    val route: String
}

object Login : AppDestination {
    override val icon  = Icons.Filled.Person
    override val label = "Login"
    override val route = "login"
}

object Home : AppDestination {
    override val icon  = Icons.Filled.Home
    override val label = "Home"
    override val route = "home"
}
