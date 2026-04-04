package eu.anifantakis.camerax_demo

import androidx.annotation.OptIn
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
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
import eu.anifantakis.camerax_demo.ui.screens.legacy.LegacyControllerPreview
import eu.anifantakis.camerax_demo.ui.screens.legacy.LegacyCameraSwitchingPreview
import eu.anifantakis.camerax_demo.ui.screens.legacy.LegacyContentScalePreview
import eu.anifantakis.camerax_demo.ui.screens.legacy.LegacyEffectsPreview
import eu.anifantakis.camerax_demo.ui.screens.legacy.LegacyManualExposurePreview
import eu.anifantakis.camerax_demo.ui.screens.legacy.LegacyMenuScreen
import eu.anifantakis.camerax_demo.ui.screens.legacy.LegacyMlKitPreview
import eu.anifantakis.camerax_demo.ui.screens.legacy.LegacyPhotoVideoCapturePreview
import eu.anifantakis.camerax_demo.ui.screens.legacy.LegacyTapToFocusPreview
import eu.anifantakis.camerax_demo.ui.screens.realistic.AdaptiveScreen
import eu.anifantakis.camerax_demo.ui.screens.realistic.CaptureScreen
import eu.anifantakis.camerax_demo.ui.screens.realistic.InteractivePreviewScreen
import eu.anifantakis.camerax_demo.ui.screens.realistic.PreviewOnlyScreen
import eu.anifantakis.camerax_demo.ui.screens.realistic.RealisticMenuScreen
import eu.anifantakis.camerax_demo.ui.screens.simplistic.CameraSwitchingPreview
import eu.anifantakis.camerax_demo.ui.screens.simplistic.ContentScalePreview
import eu.anifantakis.camerax_demo.ui.screens.simplistic.SimplisticMenuScreen
import eu.anifantakis.camerax_demo.ui.screens.simplistic.TapToFocusPreview
import eu.anifantakis.camerax_demo.ui.screens.simplistic.PhotoVideoCapturePreview
import eu.anifantakis.camerax_demo.ui.screens.simplistic.AdaptivePreview
import eu.anifantakis.camerax_demo.ui.screens.simplistic.EffectsPreview
import eu.anifantakis.camerax_demo.ui.screens.simplistic.ExtensionsPreview
import eu.anifantakis.camerax_demo.ui.screens.simplistic.FullCameraPreview
import eu.anifantakis.camerax_demo.ui.screens.simplistic.ManualExposurePreview
import eu.anifantakis.camerax_demo.ui.screens.realistic.mlkit.MlKitScreen
import eu.anifantakis.camerax_demo.ui.screens.simplistic.MlKitPreview
import eu.anifantakis.camerax_demo.ui.screens.simplistic.LensSelectionPreview
import eu.anifantakis.camerax_demo.ui.screens.legacy.LegacyExtensionsPreview
import eu.anifantakis.camerax_demo.ui.screens.legacy.LegacyLensSelectionPreview
import eu.anifantakis.camerax_demo.ui.screens.legacy.LegacyMedia3Preview
import eu.anifantakis.camerax_demo.ui.screens.legacy.LegacySessionConfigPreview
import eu.anifantakis.camerax_demo.ui.screens.simplistic.Media3Preview
import eu.anifantakis.camerax_demo.ui.screens.simplistic.SessionConfigPreview
import eu.anifantakis.camerax_demo.ui.screens.realistic.media3.Media3Screen
import eu.anifantakis.camerax_demo.ui.screens.simplistic.BasicCameraPreview

// Routes for Simplistic examples
sealed class SimplisticRoute(val path: String) {
    data object Menu : SimplisticRoute("simplistic_menu")
    data object BasicPreview : SimplisticRoute("simplistic_basic_preview")
    data object CameraSwitching : SimplisticRoute("simplistic_camera_switching")
    data object TapToFocus : SimplisticRoute("simplistic_tap_to_focus")
    data object PhotoVideoCapture : SimplisticRoute("simplistic_photo_video_capture")

    data object Adaptive : SimplisticRoute("simplistic_adaptive")
    data object Effects : SimplisticRoute("simplistic_effects")
    data object MlKit : SimplisticRoute("simplistic_mlkit")
    data object ManualExposure : SimplisticRoute("simplistic_manual_exposure")
    data object FullCamera : SimplisticRoute("simplistic_full_camera")
    data object ContentScale : SimplisticRoute("simplistic_content_scale")
    data object Extensions : SimplisticRoute("simplistic_extensions")
    data object LensSelection : SimplisticRoute("simplistic_lens_selection")
    data object Media3 : SimplisticRoute("simplistic_media3")
    data object SessionConfig : SimplisticRoute("simplistic_session_config")
}

// Routes for Realistic examples
sealed class RealisticRoute(val path: String) {
    data object Menu : RealisticRoute("realistic_menu")
    data object Preview : RealisticRoute("realistic_preview")
    data object Interactive : RealisticRoute("realistic_interactive")
    data object Capture : RealisticRoute("realistic_capture")
    data object Adaptive : RealisticRoute("realistic_adaptive")
    data object MlKit : RealisticRoute("realistic_mlkit")
    data object Media3 : RealisticRoute("realistic_media3")
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
    data object MlKit : LegacyRoute("legacy_mlkit")
    data object ManualExposure : LegacyRoute("legacy_manual_exposure")
    data object ContentScale : LegacyRoute("legacy_content_scale")
    data object Extensions : LegacyRoute("legacy_extensions")
    data object LensSelection : LegacyRoute("legacy_lens_selection")
    data object Media3 : LegacyRoute("legacy_media3")
    data object SessionConfig : LegacyRoute("legacy_session_config")
    data object Controller : LegacyRoute("legacy_controller")
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

// Camera2Interop is experimental — opt-in required because ManualExposure demo
// uses Camera2 capture-request parameters (ISO, shutter speed) via Camera2Interop.
// Annotation needed for the ManualExposure route, but we can just annotate the whole NavHost for simplicity.
@OptIn(ExperimentalCamera2Interop::class)
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
        composable(LegacyRoute.MlKit.path) { LegacyMlKitPreview() }
        composable(LegacyRoute.ManualExposure.path) { LegacyManualExposurePreview() }
        composable(LegacyRoute.ContentScale.path) { LegacyContentScalePreview() }
        composable(LegacyRoute.Extensions.path) { LegacyExtensionsPreview() }
        composable(LegacyRoute.LensSelection.path) { LegacyLensSelectionPreview() }
        composable(LegacyRoute.Media3.path) { LegacyMedia3Preview() }
        composable(LegacyRoute.SessionConfig.path) { LegacySessionConfigPreview() }
        composable(LegacyRoute.Controller.path) { LegacyControllerPreview() }
    }
}

// Camera2Interop is experimental — opt-in required because ManualExposure demo
// uses Camera2 capture-request parameters (ISO, shutter speed) via Camera2Interop.
// Annotation needed for the ManualExposure route, but we can just annotate the whole NavHost for simplicity.
@OptIn(ExperimentalCamera2Interop::class)
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
        composable(SimplisticRoute.MlKit.path) { MlKitPreview() }
        composable(SimplisticRoute.ManualExposure.path) { ManualExposurePreview() }
        composable(SimplisticRoute.FullCamera.path) { FullCameraPreview() }
        composable(SimplisticRoute.ContentScale.path) { ContentScalePreview() }
        composable(SimplisticRoute.Extensions.path) { ExtensionsPreview() }
        composable(SimplisticRoute.LensSelection.path) { LensSelectionPreview() }
        composable(SimplisticRoute.Media3.path) { Media3Preview() }
        composable(SimplisticRoute.SessionConfig.path) { SessionConfigPreview() }
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
        composable(RealisticRoute.MlKit.path) { MlKitScreen() }
        composable(RealisticRoute.Media3.path) { Media3Screen() }
    }
}
