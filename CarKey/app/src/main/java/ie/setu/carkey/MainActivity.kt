package ie.setu.carkey

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import ie.setu.carkey.data.login.AuthManager
import ie.setu.carkey.data.login.UserRole
import ie.setu.carkey.service.BleManager
import ie.setu.carkey.ui.general.AppNavDrawer
import ie.setu.carkey.ui.general.TopAppBarProvider
import ie.setu.carkey.ui.navigation.Login
import ie.setu.carkey.ui.navigation.NavHostProvider
import ie.setu.carkey.ui.theme.CarKeyTheme
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val blePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }

    private val blePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Timber.i("BLE permissions granted — starting GATT server")
            BleManager.start(this)
        } else {
            Timber.w("BLE permissions denied — VACU communication unavailable")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        blePermissionLauncher.launch(blePermissions)
        setContent {
            CarKeyTheme {
                CarKeyApp()
            }
        }
    }
}

@Composable
fun CarKeyApp() {
    val navController  = rememberNavController()
    val drawerState    = rememberDrawerState(DrawerValue.Closed)
    val scope          = rememberCoroutineScope()
    val currentRoute   = navController.currentBackStackEntryAsState().value?.destination?.route
    var userRole       by remember { mutableStateOf<UserRole?>(null) }

    val showTopBar = currentRoute != Login.route

    AppNavDrawer(
        email       = AuthManager.currentEmail,
        role        = userRole,
        drawerState = drawerState,
        onSignOut   = {
            AuthManager.signOut()
            userRole = null
            navController.navigate(Login.route) { popUpTo(0) { inclusive = true } }
        }
    ) {
        Scaffold(
            topBar = {
                if (showTopBar) {
                    TopAppBarProvider(
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }
            }
        ) { paddingValues ->
            NavHostProvider(
                navController   = navController,
                paddingValues   = paddingValues,
                onLoginSuccess  = { role -> userRole = role }
            )
        }
    }
}
