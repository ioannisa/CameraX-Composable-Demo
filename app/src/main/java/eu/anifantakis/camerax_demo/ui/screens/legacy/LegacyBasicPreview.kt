package eu.anifantakis.camerax_demo.ui.screens.legacy

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * LEGACY: Basic CameraX Preview using AndroidView + PreviewView
 *
 * THE OLD WAY (Imperative):
 *  - Create a PreviewView (Android View)
 *  - Wrap it in AndroidView composable
 *  - Hand off the surface provider to CameraX
 *
 * PROBLEMS WITH THIS APPROACH:
 *  1. View island inside Compose - different lifecycle, different paradigm
 *  2. PreviewView is a black box - can't clip, blur, or animate like Compose
 *  3. Manual coordination between View and Compose systems
 *  4. Imperative hand-off: "Here's a View, CameraX draw on it"
 *
 * Compare with: simplistic/BasicCameraPreview.kt for the new declarative way
 *
 *
 *
 *
 *
 *
 *   How lifecycle is handled:
 *
 *   It isn't handled by you at all. It was delegated entirely to CameraX. When you call:
 *   cameraProvider.bindToLifecycle(lifecycleOwner, ...)
 *
 *   CameraX registers an observer on the lifecycleOwner. That observer:
 *   - starts the camera on ON_START/ON_RESUME
 *   - stops it on ON_STOP/ON_PAUSE
 *   - unbinds all use cases on ON_DESTROY
 *
 *   So the camera does get cleaned up — but only when the Activity/Fragment is destroyed, not when the composable is removed
 *   from the tree.
 *
 *   Compare with: `LegacyBasicPreviewLifecycle.kt` for an approach that uses DisposableEffect to handle cleanup when the composable leaves the tree.
 */

@Composable
fun LegacyBasicPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Allocate a PreviewView once and keep the same instance across recompositions.
    // PreviewView is an Android View that holds a SurfaceView/TextureView internally;
    // CameraX will render camera frames directly into that native surface.
    val previewView = remember { PreviewView(context) }

    // LaunchedEffect runs once when `previewView` enters composition (key never changes
    // because `previewView` is a stable `remember`-ed instance).  It launches a
    // coroutine on the Main dispatcher, which is where the CameraX listener callback
    // must also run.
    LaunchedEffect(previewView) {
        // ProcessCameraProvider is a singleton that manages camera hardware access.
        // getInstance() returns a ListenableFuture; the result is delivered via a listener
        // on the main executor once the provider is ready.
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Build a Preview use case with default settings (resolution, aspect ratio, etc.).
            val preview = Preview.Builder().build()

            // IMPERATIVE HAND-OFF:
            // Tell CameraX which surface to draw frames onto.
            // previewView.surfaceProvider is the bridge between the camera pipeline
            // and the native surface that PreviewView manages internally.
            preview.surfaceProvider = previewView.surfaceProvider

            // Unbind any previously bound use cases before rebinding, to avoid
            // "use case already bound" exceptions on recomposition or config changes.
            cameraProvider.unbindAll()

            // Bind the Preview use case to the lifecycle owner.
            // CameraX will automatically:
            //   - Start the camera when the owner reaches ON_START / ON_RESUME
            //   - Stop  the camera when the owner reaches ON_STOP  / ON_PAUSE
            //   - Unbind all use cases when the owner reaches ON_DESTROY
            // This is the only lifecycle management in this version — no explicit cleanup.
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview
            )
        }, ContextCompat.getMainExecutor(context))
    }

    // THE VIEW ISLAND: embed the legacy PreviewView inside the Compose hierarchy.
    // `factory` is called once to create the View; `update` (omitted here) would be
    // called on recomposition.  The View fills all available space via the modifier.
    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}
