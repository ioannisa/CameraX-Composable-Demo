package eu.anifantakis.camerax_demo.ui.screens.simplistic

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * MODERN APPROACH: Declarative + Explicit Lifecycle Management
 *
 * This builds on [BasicCameraPreviewLaunchedEffect] by adding proper Compose lifecycle cleanup
 * via [DisposableEffect]. Without this, camera use cases remain bound in a stopped state
 * when the composable leaves the tree, relying on implicit NavBackStackEntry lifecycle
 * behavior instead of explicit cleanup.
 *
 * THE FIX:
 * By using [DisposableEffect] instead of [LaunchedEffect], we get an [onDispose]
 * callback that fires when the Composable is removed from the tree. Inside it,
 * we explicitly unbind the camera use cases and cancel any in-flight coroutine,
 * ensuring a clean state regardless of which lifecycle owner is in use.
 *
 * Compare with:
 * - `BasicCameraPreviewLaunchedEffect.kt` to see the anti-pattern without explicit cleanup.
 * - `legacy/LegacyBasicPreviewDisposable.kt` for the same fix applied to the legacy AndroidView approach.
 */
@Composable
fun BasicCameraPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // We need a scope to launch coroutines from inside the synchronous DisposableEffect
    val scope = rememberCoroutineScope()

    // 1. Setup reactive state to hold the incoming surface requests
    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        var cameraProvider: ProcessCameraProvider? = null

        val preview = Preview.Builder().build().apply {
            // Instead of giving a View, we publish the request to our StateFlow
            setSurfaceProvider { req -> surfaceRequests.value = req }
        }

        // 2. Launch the background work safely
        val job = scope.launch {
            cameraProvider = ProcessCameraProvider.awaitInstance(context)

            cameraProvider.bindToLifecycle(
                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview
            )
        }

        // 3. Explicitly clean up resources when the Composable leaves the tree
        onDispose {
            job.cancel()
            cameraProvider?.unbind(preview)
            preview.surfaceProvider = null
        }
    }

    // 4. Reactively render the Viewfinder only when we have a valid request
    surfaceRequest?.let {
        CameraXViewfinder(
            surfaceRequest = it,
            modifier = Modifier.fillMaxSize()
        )
    }
}


