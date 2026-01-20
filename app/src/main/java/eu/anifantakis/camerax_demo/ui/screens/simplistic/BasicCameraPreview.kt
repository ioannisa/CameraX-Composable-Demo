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
 * Basic CameraX Preview - Minimal Example
 *
 * This is a simplified, self-contained composable that demonstrates
 * the core CameraX + Compose pattern without ViewModel abstraction.
 *
 * Key concepts:
 *  1. STATE SETUP: MutableStateFlow REQUIRES an initial value. We start null.
 *  2. CAMERAX BINDING: awaitInstance() is the coroutine-friendly API (1.4+)
 *  3. SURFACE PROVIDER: Publishes requests to StateFlow instead of giving CameraX a View
 *  4. RENDER: CameraXViewfinder consumes the request and renders frames
 *
 * No AndroidView. No PreviewView. Just standard Compose patterns.
 */
@Composable
fun BasicCameraPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { req -> surfaceRequests.value = req }
        }
        cameraProvider.bindToLifecycle(
            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview
        )
    }

    surfaceRequest?.let {
        CameraXViewfinder(surfaceRequest = it, modifier = Modifier.fillMaxSize())
    }
}
