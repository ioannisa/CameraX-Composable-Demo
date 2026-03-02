package eu.anifantakis.camerax_demo.ui.screens.legacy

import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * LEGACY: LifecycleCameraController — the simplest CameraX path
 *
 * LifecycleCameraController is a convenience wrapper around ProcessCameraProvider
 * that bundles camera binding, tap-to-focus, and pinch-to-zoom into a single class.
 * You just assign it to a PreviewView and bind it to a LifecycleOwner — done.
 *
 * WHAT YOU GET FOR FREE:
 *  - Tap-to-focus: tap anywhere on the preview to focus
 *  - Pinch-to-zoom: pinch to zoom in/out
 *  - Lifecycle-aware: automatically starts/stops with the lifecycle
 *
 * TRADEOFFS:
 *  - Tied to PreviewView — does not work with CameraXViewfinder
 *  - Less flexible than ProcessCameraProvider for advanced use cases
 *    (e.g. manual use-case binding, SessionConfig, custom surface providers)
 *  - Cannot be used as a migration stepping stone to CameraXViewfinder
 *
 * Compare with:
 *  - legacy/LegacyTapToFocusPreview.kt — same features via ProcessCameraProvider
 *    + manual touch listeners (more code, but the pattern that migrates to Compose)
 *  - simplistic/TapToFocusPreview.kt — the Compose-native equivalent using
 *    CameraXViewfinder + MutableCoordinateTransformer
 */
@Composable
fun LegacyControllerPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            // Tap-to-focus and pinch-to-zoom are enabled by default.
            // No manual touch listeners, no ScaleGestureDetector, no MeteringPointFactory.
            bindToLifecycle(lifecycleOwner)
        }
    }

    // That's it. PreviewView + LifecycleCameraController = working camera
    // with built-in tap-to-focus and pinch-to-zoom in ~10 lines.
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                controller = cameraController
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
