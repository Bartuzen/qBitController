package dev.bartuzen.qbitcontroller

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

expect object Telemetry {
    fun setCurrentScreen(screen: String, tab: String? = null)
}

@Composable
fun registerCurrentScreenTelemetry(navController: NavController) {
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    LaunchedEffect(route) {
        val screen = route?.substringAfter("Destination.")?.substringBefore("/")

        if (screen != null && screen != "Torrent") {
            Telemetry.setCurrentScreen(screen)
        }
    }
}
