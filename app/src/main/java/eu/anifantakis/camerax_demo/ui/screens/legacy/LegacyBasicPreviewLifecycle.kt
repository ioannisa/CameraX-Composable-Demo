package eu.anifantakis.camerax_demo.ui.screens.legacy

import android.view.View
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
 * LEGACY APPROACH: The "Two Lifecycles" Problem
 *
 * Using CameraX with [AndroidView] creates friction because you must manually synchronize
 * two completely different lifecycle paradigms:
 * 1. The Android/Activity Lifecycle: Which CameraX inherently binds to.
 * 2. The Compose Lifecycle: When the actual UI component enters and leaves the screen.
 *
 * If we rely purely on CameraX, the camera keeps running even if the Composable is removed
 * from the screen (until the Activity itself is destroyed). To fix this, we are forced to
 * use [DisposableEffect] to manually unbind the camera when the Composable leaves the tree.
 *
 * Compare with: `simplistic/BasicCameraPreview.kt` for the modern declarative way.
 */

@Composable
fun LegacyBasicPreviewLifecycle() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. Imperatively instantiate the legacy View
    val previewView = remember { PreviewView(context) }

    DisposableEffect(previewView, lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var cameraProvider: ProcessCameraProvider? = null

        val preview = Preview.Builder().build()

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // 2. Imperatively wire the View's surface to the CameraX use case
            preview.surfaceProvider = previewView.surfaceProvider

            // Best practice: unbind before rebinding (optional but safe here too)
            cameraProvider?.unbindAll()

            // 3. Bind the camera to the lifecycle manually
            cameraProvider?.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
        }, ContextCompat.getMainExecutor(context))

        // 4. Explicitly clean up resources when the Composable leaves the tree
        onDispose {
            cameraProvider?.unbind(preview)
            preview.surfaceProvider = null
        }
    }

    // 5. Embed the legacy PreviewView inside the Compose hierarchy
    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * CONCISE LEGACY APPROACH: Native Lifecycle Bridging
 * * This version also tackles the "Two Lifecycles" problem, but instead of using Compose's
 * [DisposableEffect], it listens to the native Android View's attach/detach state.
 * * This keeps all the manual lifecycle synchronization neatly contained within the
 * View's creation block.
 */
@Composable
fun LegacyBasicPreviewLifecycleConcise() {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            // 1. Imperatively instantiate the legacy View
            val previewView = PreviewView(context)

            // 2. Imperatively wire the View's surface to the CameraX use case
            val previewUseCase = Preview.Builder().build()
            previewUseCase.surfaceProvider = previewView.surfaceProvider

            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // Best practice: unbind before rebinding
                cameraProvider.unbindAll()

                // 3. Bind the camera to the lifecycle manually
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, previewUseCase)

                // 4. Explicit cleanup: Native View equivalent of DisposableEffect's onDispose
                previewView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        // Already handled by the listener above
                    }

                    override fun onViewDetachedFromWindow(v: View) {
                        // This fires when the Composable is removed from the UI tree
                        cameraProvider.unbind(previewUseCase)
                        previewUseCase.surfaceProvider = null
                    }
                })

            }, ContextCompat.getMainExecutor(context))

            // 5. Return the View to be embedded
            previewView
        }
    )
}
