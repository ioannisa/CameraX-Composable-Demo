package eu.anifantakis.camerax_demo.ui.screens.simplistic.usecase

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Use Case 2 of 4: ImageCapture.
 *
 * The ImageCapture use case takes a JPEG photo on demand. It runs alongside
 * Preview (you need Preview to see what you are about to shoot), but it is
 * a separate pipeline — frames are captured at full sensor resolution when
 * `takePicture()` is called.
 *
 * The pipeline:
 *   Preview       --(SurfaceRequest)--> CameraXViewfinder
 *   ImageCapture  --(takePicture)----> MediaStore (DCIM/CameraX)
 *
 * Bind both together: `bindToLifecycle(owner, selector, preview, imageCapture)`.
 */
@Composable
fun UseCaseImageCapture() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = ContextCompat.getMainExecutor(context)
    val scope = rememberCoroutineScope()

    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    DisposableEffect(lifecycleOwner) {
        var cameraProvider: ProcessCameraProvider? = null

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { req -> surfaceRequests.value = req }
        }

        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val job = scope.launch {
            cameraProvider = ProcessCameraProvider.awaitInstance(context)
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                capture
            )
            imageCapture = capture
        }

        onDispose {
            job.cancel()
            cameraProvider?.unbindAll()
            preview.surfaceProvider = null
            imageCapture = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        surfaceRequest?.let { req ->
            CameraXViewfinder(surfaceRequest = req, modifier = Modifier.fillMaxSize())
        }

        Button(
            onClick = {
                val capture = imageCapture ?: return@Button

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/CameraX")
                    }
                }

                val output = ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                ).build()

                capture.takePicture(
                    output,
                    mainExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(result: ImageCapture.OutputFileResults) { /* saved */ }
                        override fun onError(e: ImageCaptureException) { /* handle */ }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        ) {
            Text("Take Photo")
        }
    }
}
