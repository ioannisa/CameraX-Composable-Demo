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
 * LEGACY APPROACH: The Missing Composable Teardown
 *
 * This demonstrates the older, imperative way of wiring CameraX using [AndroidView].
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
 * - `LegacyBasicPreviewLifecycle.kt` to see how to fix this by explicitly unbinding.
 * - `simplistic/BasicCameraPreview.kt` for the modern, declarative Compose way.
 */
@Composable
fun LegacyBasicPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. Imperatively instantiate the legacy View
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // 2. Imperatively wire the View's surface to the CameraX use case
            val preview = Preview.Builder().build()
            preview.surfaceProvider = previewView.surfaceProvider

            // Best practice: unbind before rebinding
            cameraProvider.unbindAll()

            // 3. Bind the camera to the lifecycle manually
            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
        }, ContextCompat.getMainExecutor(context))
    }

    // 4. Embed the legacy PreviewView inside the Compose hierarchy
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

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            // 1. Imperatively instantiate the legacy View
            val previewView = PreviewView(context)

            // 2. Imperatively wire the View's surface to the CameraX use case
            val previewUseCase = Preview.Builder().build()
            previewUseCase.surfaceProvider = previewView.surfaceProvider

            // 3. Bind the camera to the lifecycle manually
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // Best practice: unbind before rebinding
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, previewUseCase)
            }, ContextCompat.getMainExecutor(context))

            // 4. Return the View to be embedded
            previewView
        }
    )
}
