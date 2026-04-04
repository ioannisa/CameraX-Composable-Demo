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
 * leaves the tree — but that does NOT unbind the camera use cases from
 * the [ProcessCameraProvider].
 *
 * WHEN IT WORKS FINE (AND WHY YOU MIGHT NOT NOTICE):
 * Navigating away with the **back button** (pop) destroys the NavBackStackEntry.
 * CameraX sees ON_DESTROY and auto-unbinds everything. Nothing is held.
 * This is the scenario most developers test, so the bug stays hidden.
 *
 * WHEN IT BREAKS:
 * Navigate **forward** (push) to another screen. The NavBackStackEntry is not
 * destroyed — it goes to CREATED and sits on the back stack with use cases
 * still bound in a stopped state. When you navigate **back** to this screen,
 * the composable is recreated, a new [LaunchedEffect] runs, and it tries to
 * bind a **second** Preview to the same lifecycle owner that already has one.
 * CameraX does not allow duplicate use case types — this causes a crash or
 * silently broken state.
 *
 * The same problem occurs when a camera section is conditionally shown/hidden
 * within the same screen (e.g., a toggle). See `AntiPatternToggleDemo.kt`
 * for a self-contained reproduction.
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
