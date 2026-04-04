package eu.anifantakis.camerax_demo.ui.screens.simplistic

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * ==================================================================
 * ******* ANTI-PATTERN: No Cleanup on Composition Exit *************
 * ==================================================================
 *
 * This file demonstrates the WRONG way to bind CameraX in Compose.
 * It uses [LaunchedEffect] which cancels its coroutine when the composable
 * leaves the tree — but that does NOT unbind the camera use cases.
 *
 * WHY IT APPEARS HARMLESS:
 * In Compose Navigation, [LocalLifecycleOwner] is the NavBackStackEntry,
 * not the Activity. When you navigate away, that lifecycle goes to STOPPED
 * and CameraX automatically closes the camera hardware — so the green
 * indicator light turns off and battery is not wasted.
 *
 * WHY IT IS STILL WRONG:
 * - The use cases remain **bound but stopped** inside ProcessCameraProvider,
 *   holding memory and occupying binding slots on the back stack.
 * - If the NavBackStackEntry resumes (user presses back), the camera reopens
 *   with stale bindings instead of a clean state.
 * - If [LocalLifecycleOwner] ever happens to be the Activity (not a
 *   NavBackStackEntry), the camera truly would keep running in the background.
 * - Relying on implicit lifecycle behavior rather than explicit cleanup is fragile.
 *
 * See [BasicCameraPreview] in `BasicCameraPreviewDisposable.kt` for the correct approach using [DisposableEffect].
 */
@Composable
fun BasicCameraPreviewLaunchedEffect() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. Setup reactive state to hold the incoming surface requests
    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    // 2. Coroutine-friendly initialization and wiring of the camera
    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)

        val preview = Preview.Builder().build().apply {
            // Instead of giving a View, we publish the request to our StateFlow
            setSurfaceProvider { req -> surfaceRequests.value = req }
        }

        cameraProvider.bindToLifecycle(
            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview
        )
    }

    // 3. Reactively render the Viewfinder only when we have a valid request
    surfaceRequest?.let {
        CameraXViewfinder(
            surfaceRequest = it,
            modifier = Modifier.fillMaxSize()
        )
    }
}
