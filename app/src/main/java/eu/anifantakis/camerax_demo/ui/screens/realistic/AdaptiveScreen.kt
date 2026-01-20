package eu.anifantakis.camerax_demo.ui.screens.realistic

import android.app.Activity
import androidx.camera.compose.CameraXViewfinder
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.koin.androidx.compose.koinViewModel

/**
 * Foldables & Adaptive UIs - Realistic Example (with ViewModel)
 *
 * Uses WindowSizeClass (Material 3 recommended approach) to detect:
 *  - Compact: phones in portrait
 *  - Medium: phones in landscape, small tablets
 *  - Expanded: tablets, foldables unfolded
 *
 * COMPACT (Portrait phones):
 *  - Full-screen preview with controls overlaid at bottom
 *
 * MEDIUM/EXPANDED (Landscape, tablets, foldables):
 *  - Row with preview on one side, controls on the other
 *
 * AnimatedContent handles transitions. The preview animates along with
 * everything else — no special camera code for foldables.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun AdaptiveScreen(
    vm: CameraViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) { vm.bindPreview(lifecycleOwner) }
    val request by vm.surfaceRequest.collectAsState(initial = null)

    // Modern approach: WindowSizeClass detects width, not just orientation
    // Works better for tablets and foldables
    val windowSizeClass = calculateWindowSizeClass(context as Activity)
    val isExpanded = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    // AnimatedContent handles layout transitions smoothly
    AnimatedContent(targetState = isExpanded, label = "adaptive") { expanded ->
        if (expanded) {
            // Medium/Expanded: Row with preview + controls side by side
            Row(Modifier.fillMaxSize()) {
                request?.let {
                    CameraXViewfinder(
                        surfaceRequest = it,
                        modifier = Modifier.weight(1f).aspectRatio(9f / 16f)
                    )
                }
                Box(Modifier.weight(1f)) { DemoControls(Modifier.align(Alignment.Center)) }
            }
        } else {
            // Compact: Full-screen preview with controls overlaid
            Box(Modifier.fillMaxSize()) {
                request?.let {
                    CameraXViewfinder(surfaceRequest = it, modifier = Modifier.fillMaxSize())
                }
                DemoControls(Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun DemoControls(modifier: Modifier = Modifier) {
    Row(modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { /* example toggle */ }) { Text("Grid") }
        OutlinedButton(onClick = { /* example toggle */ }) { Text("Level") }
    }
}
