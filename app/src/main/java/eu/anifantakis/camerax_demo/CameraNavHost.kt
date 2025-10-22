package eu.anifantakis.camerax_demo

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import eu.anifantakis.camerax_demo.ui.screens.AdaptiveScreen
import eu.anifantakis.camerax_demo.ui.screens.CaptureScreen
import eu.anifantakis.camerax_demo.ui.screens.InteractivePreviewScreen
import eu.anifantakis.camerax_demo.ui.screens.MainMenuScreen
import eu.anifantakis.camerax_demo.ui.screens.PreviewOnlyScreen

sealed class Route(val path: String) {
    data object Main : Route("main")
    data object Preview : Route("preview")
    data object Interactive : Route("interactive")
    data object Capture : Route("capture")
    data object Adaptive : Route("adaptive")
}

@Composable
fun CameraNavHost(
    paddingValues: PaddingValues
) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Route.Main.path) {
        composable(Route.Main.path) { MainMenuScreen(nav) }
        composable(Route.Preview.path) { PreviewOnlyScreen() }
        composable(Route.Interactive.path) { InteractivePreviewScreen() }
        composable(Route.Capture.path) { CaptureScreen() }
        composable(Route.Adaptive.path) { AdaptiveScreen() }
    }
}