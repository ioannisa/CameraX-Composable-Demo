package eu.anifantakis.camerax_demo.ui.screens.legacy

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

/**
 * LEGACY APPROACH: The Missing Composable Teardown (Now with Coroutines!)
 *
 * This demonstrates the older, imperative way of wiring CameraX using [AndroidView].
 * Note how much cleaner the syntax is now that we use `awaitInstance()` instead of
 * `ListenableFuture` callbacks!
 *
 * THE LIFECYCLE MISMATCH:
 * Here, the camera is bound to the [LocalLifecycleOwner] (usually the Activity or Fragment).
 * CameraX automatically handles starting and stopping the camera based on *that* native lifecycle.
 *
 * However, because there is no explicit Compose cleanup, if this Composable is removed
 * from the UI tree (e.g., navigating to another screen), the camera keeps running in the
 * background until the Activity itself is destroyed.
 *
 * Compare with:
 * - `LegacyBasicPreviewDisposable.kt` to see how to fix this by explicitly unbinding.
 * - `simplistic/BasicCameraPreview.kt` for the modern, declarative Compose way.
 */
@Composable
fun LegacyBasicPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. Imperatively instantiate the legacy View
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(previewView) {
        // 2. Coroutine-friendly initialization
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)

        // 3. Imperatively wire the View's surface to the CameraX use case
        val preview = Preview.Builder().build()
        preview.surfaceProvider = previewView.surfaceProvider

        // Best practice: unbind before rebinding
        cameraProvider.unbindAll()

        // 4. Bind the camera to the lifecycle manually
        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
    }

    // 5. Embed the legacy PreviewView inside the Compose hierarchy
    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * CONCISE LEGACY APPROACH: Cleaner Syntax, Same Lifecycle Mismatch
 *
 * This version neatly packs all the imperative setup into the [AndroidView] factory,
 * avoiding the need to juggle `remember` and `LaunchedEffect`.
 *
 * Note: Just like the version above, this relies solely on the Activity/Fragment lifecycle.
 * It does NOT clean up the camera when the Composable leaves the screen.
 */
@Composable
fun LegacyBasicPreviewConcise() {
    val lifecycleOwner = LocalLifecycleOwner.current

    // We need a scope because the `factory` lambda below is not a suspend function
    val scope = rememberCoroutineScope()

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            // 1. Imperatively instantiate the legacy View
            val previewView = PreviewView(context)

            // 2. Imperatively wire the View's surface to the CameraX use case
            val previewUseCase = Preview.Builder().build()
            previewUseCase.surfaceProvider = previewView.surfaceProvider

            // 3. Launch a coroutine to fetch the camera provider asynchronously
            scope.launch {
                val cameraProvider = ProcessCameraProvider.awaitInstance(context)

                // Best practice: unbind before rebinding
                cameraProvider.unbindAll()

                // 4. Bind the camera to the lifecycle manually
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, previewUseCase)
            }

            // 5. Return the View to be embedded
            previewView
        }
    )
}