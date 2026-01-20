package eu.anifantakis.camerax_demo.ui.screens.simplistic

import android.app.Activity
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Foldables & Adaptive UIs - Simplistic Example
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
fun AdaptivePreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { req -> surfaceRequests.value = req }
        }
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview
        )
    }

    // Modern approach: WindowSizeClass detects width, not just orientation
    // Works better for tablets and foldables
    val windowSizeClass = calculateWindowSizeClass(context as Activity)
    val isExpanded = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    // AnimatedContent handles layout transitions smoothly
    AnimatedContent(targetState = isExpanded, label = "adaptive") { expanded ->
        if (expanded) {
            // Medium/Expanded: Row with preview + controls side by side
            Row(Modifier.fillMaxSize()) {
                surfaceRequest?.let {
                    CameraXViewfinder(
                        surfaceRequest = it,
                        modifier = Modifier.weight(1f).aspectRatio(9f / 16f)
                    )
                }
                Controls(modifier = Modifier.weight(1f))
            }
        } else {
            // Compact: Full-screen preview with controls overlaid
            Box(Modifier.fillMaxSize()) {
                surfaceRequest?.let {
                    CameraXViewfinder(
                        surfaceRequest = it,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Controls(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun Controls(modifier: Modifier = Modifier) {
    Row(modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { /* example */ }) { Text("Grid") }
        OutlinedButton(onClick = { /* example */ }) { Text("Level") }
    }
}
