package ie.setu.carkey.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ie.setu.carkey.data.login.UserRole
import ie.setu.carkey.ui.screens.home.HomeScreen
import ie.setu.carkey.ui.screens.login.LoginScreen
import ie.setu.carkey.ui.screens.manager.EventLogScreen
import ie.setu.carkey.ui.screens.manager.KeysScreen
import ie.setu.carkey.ui.screens.manager.ManagerDashboardScreen
import ie.setu.carkey.ui.screens.manager.UsersScreen
import ie.setu.carkey.ui.screens.manager.VehiclesScreen

@Composable
fun NavHostProvider(
    navController: NavHostController,
    paddingValues: PaddingValues,
    onLoginSuccess: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Login.route,
        modifier = modifier.padding(paddingValues)
    ) {
        composable(Login.route) {
            LoginScreen(
                onLoginSuccess = { role ->
                    onLoginSuccess(role)
                    val destination = if (role == UserRole.MANAGER)
                        ManagerDashboard.route
                    else
                        Home.route
                    navController.navigate(destination) {
                        popUpTo(Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Home.route) {
            HomeScreen(
                onSignOut = {
                    navController.navigate(Login.route) {
                        popUpTo(Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(ManagerDashboard.route) {
            ManagerDashboardScreen(
                onNavigateToKeys = { navController.navigate(ManagerKeys.route) },
                onNavigateToEvents = { navController.navigate(ManagerEvents.route) },
                onNavigateToVehicles = { navController.navigate(ManagerVehicles.route) },
                onNavigateToUsers = { navController.navigate(ManagerUsers.route) },
                onSignOut = {
                    navController.navigate(Login.route) {
                        popUpTo(ManagerDashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(ManagerKeys.route) {
            KeysScreen()
        }

        composable(ManagerEvents.route) {
            EventLogScreen()
        }

        composable(ManagerVehicles.route) {
            VehiclesScreen()
        }

        composable(ManagerUsers.route) {
            UsersScreen()
        }
    }
}
