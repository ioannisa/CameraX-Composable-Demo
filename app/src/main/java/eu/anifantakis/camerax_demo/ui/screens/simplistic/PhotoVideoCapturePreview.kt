package eu.anifantakis.camerax_demo.ui.screens.simplistic

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Photo & Video Capture - Simplistic Example
 *
 * Bind use cases, trigger from Compose buttons.
 *
 * KEY CONCEPT:
 *  - Preview, ImageCapture, and VideoCapture all bind together
 *  - They share the same camera session via bindToLifecycle()
 *  - Preview and capture controls live in the same Compose tree. One UI system.
 *
 * BINDING PATTERN:
 *  1. Build Preview with surface provider
 *  2. Build ImageCapture with capture mode
 *  3. Build VideoCapture with Recorder (quality selector)
 *  4. Bind ALL together in one bindToLifecycle() call
 */
@Composable
fun PhotoVideoCapturePreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = ContextCompat.getMainExecutor(context)

    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    // Store references for capture operations
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }

    // Binding Code - All use cases bound together
    LaunchedEffect(Unit) {
        val provider = ProcessCameraProvider.awaitInstance(context)

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { surfaceRequests.value = it }
        }

        val imgCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.FHD))
            .build()
        val vidCapture = VideoCapture.withOutput(recorder)

        provider.unbindAll()

        // All share the same camera session
        camera = provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imgCapture,
            vidCapture
        )

        imageCapture = imgCapture
        videoCapture = vidCapture
    }

    Box(Modifier.fillMaxSize()) {
        surfaceRequest?.let { req ->
            CameraXViewfinder(surfaceRequest = req, modifier = Modifier.fillMaxSize())
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Take Photo button
            Button(onClick = {
                val capture = imageCapture ?: return@Button

                val name = "IMG_${System.currentTimeMillis()}.jpg"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/CameraX")
                    }
                }

                val outputOptions = ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                ).build()

                capture.takePicture(
                    outputOptions,
                    mainExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onError(e: ImageCaptureException) { /* Handle error */ }
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) { /* Success */ }
                    }
                )
            }) {
                Text("Take Photo")
            }

            // Record Video button (simplified - no audio permission handling)
            Button(onClick = {
                val vc = videoCapture ?: return@Button

                // Stop if already recording
                recording?.let {
                    it.stop()
                    recording = null
                    return@Button
                }

                // Start new recording
                val name = "VID_${System.currentTimeMillis()}.mp4"
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/CameraX")
                    }
                }

                val outputOptions = MediaStoreOutputOptions.Builder(
                    context.contentResolver,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                ).setContentValues(values).build()

                recording = vc.output
                    .prepareRecording(context, outputOptions)
                    // .withAudioEnabled() // Requires RECORD_AUDIO permission
                    .start(mainExecutor) { event ->
                        if (event is VideoRecordEvent.Finalize) {
                            recording = null
                        }
                    }
            }) {
                Text(if (recording == null) "Record" else "Stop")
            }
        }
    }
}
