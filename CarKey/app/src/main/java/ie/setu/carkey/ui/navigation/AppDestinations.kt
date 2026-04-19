package ie.setu.carkey.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
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

object ManagerDashboard : AppDestination {
    override val icon  = Icons.Filled.Home
    override val label = "Dashboard"
    override val route = "manager_dashboard"
}

object ManagerKeys : AppDestination {
    override val icon  = Icons.Filled.Lock
    override val label = "Keys"
    override val route = "manager_keys"
}

object ManagerEvents : AppDestination {
    override val icon  = Icons.Filled.List
    override val label = "Event Log"
    override val route = "manager_events"
}

object ManagerVehicles : AppDestination {
    override val icon  = Icons.Filled.Home
    override val label = "Vehicles"
    override val route = "manager_vehicles"
}

object ManagerUsers : AppDestination {
    override val icon  = Icons.Filled.Person
    override val label = "Users"
    override val route = "manager_users"
}
