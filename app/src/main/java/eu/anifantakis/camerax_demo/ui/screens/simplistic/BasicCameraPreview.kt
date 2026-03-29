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
 * MODERN APPROACH: The Declarative Paradigm Shift
 *
 * This demonstrates the modern CameraX + Compose integration.
 * Notice the complete absence of `AndroidView` or `PreviewView`.
 *
 * THE BIG DIFFERENCE:
 * Instead of imperatively saying, "Here is an Android View, please draw on it,"
 * we use a reactive state flow:
 * 1. We ask CameraX for a stream of video.
 * 2. CameraX provides a [SurfaceRequest] (a state object).
 * 3. Compose reacts to that state and feeds it into [CameraXViewfinder].
 *
 * Note: While this removes the AndroidView boilerplate, this specific all-in-one
 * component still binds the camera to the Activity's lifecycle. For true
 * separation of concerns, see `NativeCameraPreview` below.
 */
@Composable
fun BasicCameraPreview() {
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
