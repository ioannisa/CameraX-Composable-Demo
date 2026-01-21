package eu.anifantakis.camerax_demo.ui.screens.legacy

import android.content.ContentValues
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * LEGACY: Photo & Video Capture using AndroidView + PreviewView
 *
 * THE OLD WAY (Imperative):
 *  - PreviewView wrapped in AndroidView
 *  - Use cases (Preview, ImageCapture, VideoCapture) bound together
 *  - Blocking ProcessCameraProvider.getInstance().get()
 *
 * PROBLEMS WITH THIS APPROACH:
 *  1. View island in Compose tree
 *  2. Capture controls are Compose, but preview is View - two worlds
 *  3. Must coordinate Compose state with View lifecycle
 *  4. Can't apply Compose effects (blur, clip) to preview
 *
 * Compare with: simplistic/PhotoVideoCapturePreview.kt for the new way
 */
@Composable
fun LegacyPhotoVideoCapturePreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider

            // Build all use cases
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val imgCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            imageCapture = imgCapture

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.FHD))
                .build()
            val vidCapture = VideoCapture.withOutput(recorder)
            videoCapture = vidCapture

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imgCapture,
                vidCapture
            )
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            recording?.stop()
            cameraProvider?.unbindAll()
        }
    }

    Box(Modifier.fillMaxSize()) {
        // THE VIEW ISLAND
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Compose controls - but preview is a separate View world
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = {
                val capture = imageCapture ?: return@Button

                val name = "IMG_${System.currentTimeMillis()}.jpg"
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                }

                val outputOptions = ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ).build()

                capture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            // Photo saved
                        }
                        override fun onError(exception: ImageCaptureException) {
                            // Handle error
                        }
                    }
                )
            }) {
                Text("Take Photo")
            }

            Button(onClick = {
                val vidCap = videoCapture ?: return@Button

                if (recording != null) {
                    recording?.stop()
                    recording = null
                } else {
                    val name = "VID_${System.currentTimeMillis()}.mp4"
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, name)
                    }

                    val outputOptions = MediaStoreOutputOptions.Builder(
                        context.contentResolver,
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    ).setContentValues(contentValues).build()

                    recording = vidCap.output
                        .prepareRecording(context, outputOptions)
                        .withAudioEnabled()
                        .start(ContextCompat.getMainExecutor(context)) { event ->
                            // Handle recording events
                        }
                }
            }) {
                Text(if (recording == null) "Record" else "Stop")
            }
        }
    }
}
