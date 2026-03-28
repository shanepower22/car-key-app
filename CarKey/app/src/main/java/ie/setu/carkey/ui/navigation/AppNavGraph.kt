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
                    navController.navigate(Home.route) {
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
    }
}
