package com.rcremote.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rcremote.ui.screens.ControlScreen
import com.rcremote.ui.screens.DeviceScreen
import com.rcremote.viewmodel.RCViewModel

object Routes {
    const val DEVICES = "devices"
    const val CONTROL = "control"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val vm: RCViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.DEVICES) {
        composable(Routes.DEVICES) {
            DeviceScreen(
                vm = vm,
                onConnected = { navController.navigate(Routes.CONTROL) {
                    popUpTo(Routes.DEVICES) { inclusive = false }
                }}
            )
        }
        composable(Routes.CONTROL) {
            ControlScreen(
                vm = vm,
                onDisconnect = { navController.popBackStack() }
            )
        }
    }
}
