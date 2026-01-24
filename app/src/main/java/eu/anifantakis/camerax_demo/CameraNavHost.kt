package eu.anifantakis.camerax_demo

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import eu.anifantakis.camerax_demo.ui.screens.legacy.LegacyAdaptivePreview
import eu.anifantakis.camerax_demo.ui.screens.legacy.LegacyBasicPreview
import eu.anifantakis.camerax_demo.ui.screens.legacy.LegacyCameraSwitchingPreview
import eu.anifantakis.camerax_demo.ui.screens.legacy.LegacyEffectsPreview
import eu.anifantakis.camerax_demo.ui.screens.legacy.LegacyManualExposurePreview
import eu.anifantakis.camerax_demo.ui.screens.legacy.LegacyMenuScreen
import eu.anifantakis.camerax_demo.ui.screens.legacy.LegacyPhotoVideoCapturePreview
import eu.anifantakis.camerax_demo.ui.screens.legacy.LegacyTapToFocusPreview
import eu.anifantakis.camerax_demo.ui.screens.realistic.AdaptiveScreen
import eu.anifantakis.camerax_demo.ui.screens.realistic.CaptureScreen
import eu.anifantakis.camerax_demo.ui.screens.realistic.InteractivePreviewScreen
import eu.anifantakis.camerax_demo.ui.screens.realistic.PreviewOnlyScreen
import eu.anifantakis.camerax_demo.ui.screens.realistic.RealisticMenuScreen
import eu.anifantakis.camerax_demo.ui.screens.simplistic.BasicCameraPreview
import eu.anifantakis.camerax_demo.ui.screens.simplistic.CameraSwitchingPreview
import eu.anifantakis.camerax_demo.ui.screens.simplistic.SimplisticMenuScreen
import eu.anifantakis.camerax_demo.ui.screens.simplistic.TapToFocusPreview
import eu.anifantakis.camerax_demo.ui.screens.simplistic.PhotoVideoCapturePreview
import eu.anifantakis.camerax_demo.ui.screens.simplistic.AdaptivePreview
import eu.anifantakis.camerax_demo.ui.screens.simplistic.EffectsPreview
import eu.anifantakis.camerax_demo.ui.screens.simplistic.ManualExposurePreview

// Routes for Simplistic examples
sealed class SimplisticRoute(val path: String) {
    data object Menu : SimplisticRoute("simplistic_menu")
    data object BasicPreview : SimplisticRoute("simplistic_basic_preview")
    data object CameraSwitching : SimplisticRoute("simplistic_camera_switching")
    data object TapToFocus : SimplisticRoute("simplistic_tap_to_focus")
    data object PhotoVideoCapture : SimplisticRoute("simplistic_photo_video_capture")
    data object Adaptive : SimplisticRoute("simplistic_adaptive")
    data object Effects : SimplisticRoute("simplistic_effects")
    data object ManualExposure : SimplisticRoute("simplistic_manual_exposure")
}

// Routes for Realistic examples
sealed class RealisticRoute(val path: String) {
    data object Menu : RealisticRoute("realistic_menu")
    data object Preview : RealisticRoute("realistic_preview")
    data object Interactive : RealisticRoute("realistic_interactive")
    data object Capture : RealisticRoute("realistic_capture")
    data object Adaptive : RealisticRoute("realistic_adaptive")
}

// Routes for Legacy examples (AndroidView + PreviewView)
sealed class LegacyRoute(val path: String) {
    data object Menu : LegacyRoute("legacy_menu")
    data object BasicPreview : LegacyRoute("legacy_basic_preview")
    data object CameraSwitching : LegacyRoute("legacy_camera_switching")
    data object TapToFocus : LegacyRoute("legacy_tap_to_focus")
    data object PhotoVideoCapture : LegacyRoute("legacy_photo_video_capture")
    data object Adaptive : LegacyRoute("legacy_adaptive")
    data object Effects : LegacyRoute("legacy_effects")
    data object ManualExposure : LegacyRoute("legacy_manual_exposure")
}

// Bottom navigation tabs
enum class BottomTab(val label: String, val icon: ImageVector) {
    Legacy("Legacy", Icons.Default.History),
    Simplistic("Simplistic", Icons.Default.Code),
    Realistic("Realistic", Icons.Default.Settings)
}

@Composable
fun CameraNavHost(
    paddingValues: PaddingValues
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        bottomBar = {
            NavigationBar {
                BottomTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> LegacyNavHost(Modifier.padding(innerPadding))
            1 -> SimplisticNavHost(Modifier.padding(innerPadding))
            2 -> RealisticNavHost(Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun LegacyNavHost(modifier: Modifier = Modifier) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = LegacyRoute.Menu.path,
        modifier = modifier
    ) {
        composable(LegacyRoute.Menu.path) { LegacyMenuScreen(nav) }
        composable(LegacyRoute.BasicPreview.path) { LegacyBasicPreview() }
        composable(LegacyRoute.CameraSwitching.path) { LegacyCameraSwitchingPreview() }
        composable(LegacyRoute.TapToFocus.path) { LegacyTapToFocusPreview() }
        composable(LegacyRoute.PhotoVideoCapture.path) { LegacyPhotoVideoCapturePreview() }
        composable(LegacyRoute.Adaptive.path) { LegacyAdaptivePreview() }
        composable(LegacyRoute.Effects.path) { LegacyEffectsPreview() }
        composable(LegacyRoute.ManualExposure.path) { LegacyManualExposurePreview() }
    }
}

@Composable
private fun SimplisticNavHost(modifier: Modifier = Modifier) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = SimplisticRoute.Menu.path,
        modifier = modifier
    ) {
        composable(SimplisticRoute.Menu.path) { SimplisticMenuScreen(nav) }
        composable(SimplisticRoute.BasicPreview.path) { BasicCameraPreview() }
        composable(SimplisticRoute.CameraSwitching.path) { CameraSwitchingPreview() }
        composable(SimplisticRoute.TapToFocus.path) { TapToFocusPreview() }
        composable(SimplisticRoute.PhotoVideoCapture.path) { PhotoVideoCapturePreview() }
        composable(SimplisticRoute.Adaptive.path) { AdaptivePreview() }
        composable(SimplisticRoute.Effects.path) { EffectsPreview() }
        composable(SimplisticRoute.ManualExposure.path) { ManualExposurePreview() }
    }
}

@Composable
private fun RealisticNavHost(modifier: Modifier = Modifier) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = RealisticRoute.Menu.path,
        modifier = modifier
    ) {
        composable(RealisticRoute.Menu.path) { RealisticMenuScreen(nav) }
        composable(RealisticRoute.Preview.path) { PreviewOnlyScreen() }
        composable(RealisticRoute.Interactive.path) { InteractivePreviewScreen() }
        composable(RealisticRoute.Capture.path) { CaptureScreen() }
        composable(RealisticRoute.Adaptive.path) { AdaptiveScreen() }
    }
}
