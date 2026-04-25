package eu.anifantakis.camerax_demo.ui.screens.simplistic.usecase

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
 * Use Case 1 of 4: Preview.
 *
 * The Preview use case streams camera frames to a Surface so the user can see
 * what the camera is pointed at. By itself, it does nothing else — no capture,
 * no analysis. Just a live viewfinder.
 *
 * The pipeline:
 *   Preview --(SurfaceRequest)--> CameraXViewfinder
 *
 * Bind it with `bindToLifecycle(owner, selector, preview)`.
 */
@Composable
fun UseCasePreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner) {
        var cameraProvider: ProcessCameraProvider? = null

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { req -> surfaceRequests.value = req }
        }

        val job = scope.launch {
            cameraProvider = ProcessCameraProvider.awaitInstance(context)
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview
            )
        }

        onDispose {
            job.cancel()
            cameraProvider?.unbind(preview)
            preview.surfaceProvider = null
        }
    }

    surfaceRequest?.let { req ->
        CameraXViewfinder(
            surfaceRequest = req,
            modifier = Modifier.fillMaxSize()
        )
    }
}
