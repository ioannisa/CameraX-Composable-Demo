package eu.anifantakis.camerax_demo.ui.screens.legacy

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * LEGACY (with explicit Compose lifecycle management): Basic CameraX Preview
 * using AndroidView + PreviewView + DisposableEffect.
 *
 * Compare with: `simplistic/BasicCameraPreview.kt` for the modern declarative way.
 *
 *
 *
 *
 *   How lifecycle is handled in LegacyBasicPreviewLifecycle:
 *
 *   It is handled by you explicitly, via DisposableEffect. When the composable enters the composition, the effect body runs
 *   and binds the camera. When the composable leaves the composition, Compose calls the onDispose block you provide:
 *
 *   onDispose {
 *       cameraProvider?.unbind(preview)
 *       preview.surfaceProvider = null
 *   }
 *
 *   This fires in two situations:
 *   - the composable is removed from the tree (e.g. navigating away)
 *   - a key passed to DisposableEffect changes, triggering a re-run
 *
 *  So the camera gets cleaned up as soon as the composable disappears — even if the Activity/Fragment is still alive and its
 *  lifecycle has not reached ON_DESTROY.
 *
 *  Compare with: `LegacyBasicPreview.kt` for an approach that relies on CameraX's internal lifecycle handling, which only cleans up when the Activity/Fragment is destroyed, not when the composable leaves the tree.
 */

@Composable
fun LegacyBasicPreviewLifecycle() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Allocate a PreviewView once and keep the same instance across recompositions.
    // CameraX renders camera frames directly into this View's native surface.
    val previewView = remember { PreviewView(context) }

    // DisposableEffect keys: `previewView` and `lifecycleOwner`.
    // The effect re-runs (teardown + setup) if either key changes.  In practice
    // both are stable for the lifetime of this composable, so the body runs exactly
    // once on entry and onDispose runs exactly once on exit.
    DisposableEffect(previewView, lifecycleOwner) {

        // ProcessCameraProvider is a singleton that manages camera hardware access.
        // We hold a nullable reference so the onDispose block can reach the same
        // provider instance even though it is delivered asynchronously.
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var cameraProvider: ProcessCameraProvider? = null

        // Build the Preview use case up-front so the same instance can be
        // referenced in both the listener (to bind it) and onDispose (to unbind it).
        val preview = Preview.Builder().build()

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // IMPERATIVE HAND-OFF:
            // Tell CameraX which surface to draw frames onto.
            // previewView.surfaceProvider bridges the camera pipeline and the
            // native surface that PreviewView manages internally.
            preview.surfaceProvider = previewView.surfaceProvider

            // Unbind any previously bound use cases to avoid "already bound" errors.
            cameraProvider?.unbindAll()

            // Bind the Preview use case to the lifecycle owner so CameraX respects
            // Activity/Fragment start/stop events (e.g., going to background).
            cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview
            )
        }, ContextCompat.getMainExecutor(context))

        // onDispose is the key addition over LegacyBasicPreview.
        // It runs when this composable leaves the composition (e.g., the user
        // navigates away) regardless of whether the lifecycle owner is still alive.
        // Without this, the camera would keep running until the Activity is destroyed.
        onDispose {
            // Unbind only the Preview use case we own, not all use cases, to avoid
            // accidentally stopping cameras bound by other composables.
            cameraProvider?.unbind(preview)

            // Null out the surface provider so CameraX stops sending frames to the
            // PreviewView surface, preventing rendering onto a detached View.
            preview.surfaceProvider = null
        }
    }

    // THE VIEW ISLAND: embed the legacy PreviewView inside the Compose hierarchy.
    // `factory` is called once to create the View.  The View fills all available
    // space via the fillMaxSize modifier.
    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}