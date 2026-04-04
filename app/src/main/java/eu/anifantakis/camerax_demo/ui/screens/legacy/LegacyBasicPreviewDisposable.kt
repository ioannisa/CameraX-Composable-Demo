package eu.anifantakis.camerax_demo.ui.screens.legacy

import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

/**
 * LEGACY APPROACH: The "Two Lifecycles" Problem (Coroutine Version)
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

    // We need a scope to launch coroutines from inside the synchronous DisposableEffect
    val scope = rememberCoroutineScope()

    // 1. Imperatively instantiate the legacy View
    val previewView = remember { PreviewView(context) }

    DisposableEffect(previewView, lifecycleOwner) {
        var cameraProvider: ProcessCameraProvider? = null
        val preview = Preview.Builder().build()

        // Launch the background work safely
        val job = scope.launch {
            cameraProvider = ProcessCameraProvider.awaitInstance(context)

            // 2. Imperatively wire the View's surface to the CameraX use case
            preview.surfaceProvider = previewView.surfaceProvider

            // Best practice: unbind before rebinding (optional but safe here too)
            cameraProvider.unbindAll()

            // 3. Bind the camera to the lifecycle manually
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview
            )
        }

        // 4. Explicitly clean up resources when the Composable leaves the tree
        onDispose {
            job.cancel() // Cancel the coroutine if the user leaves before the camera even loads!
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
 * CONCISE LEGACY APPROACH: Native Lifecycle Bridging (Coroutine Version)
 * * This version also tackles the "Two Lifecycles" problem, but instead of using Compose's
 * [DisposableEffect], it listens to the native Android View's attach/detach state.
 * * This keeps all the manual lifecycle synchronization neatly contained within the
 * View's creation block.
 */
@Composable
fun LegacyBasicPreviewLifecycleConcise() {
    val lifecycleOwner = LocalLifecycleOwner.current
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
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    previewUseCase
                )

                // 5. Explicit cleanup: Native View equivalent of DisposableEffect's onDispose
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
            }

            // 6. Return the View to be embedded
            previewView
        }
    )
}