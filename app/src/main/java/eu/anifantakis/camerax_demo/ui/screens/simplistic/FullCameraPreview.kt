package eu.anifantakis.camerax_demo.ui.screens.simplistic

import android.annotation.SuppressLint
import android.content.ContentValues
import eu.anifantakis.camerax_demo.ui.components.Permission
import eu.anifantakis.camerax_demo.ui.components.PermissionGate
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Full Camera Preview - Combined Camera Switching + Photo/Video Capture
 *
 * This example combines multiple features into a more complete camera experience:
 *  - Front/Back camera switching
 *  - Photo capture
 *  - Video recording
 *
 * KEY CONCEPTS:
 *  1. When camera selector changes, ALL use cases must be rebound together
 *  2. DisposableEffect(selector) triggers rebinding when switching cameras and cleans up on dispose
 *  3. Use cases (Preview, ImageCapture, VideoCapture) share the same camera session
 *  4. rememberSaveable preserves camera choice across configuration changes
 *
 * IMPORTANT: When switching cameras while recording, the recording stops.
 * This is expected behavior - you can't seamlessly switch cameras mid-recording.
 */
@SuppressLint("MissingPermission") // guarded by PermissionGate + Permission.RECORD_AUDIO.isGranted()
@Composable
fun FullCameraPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = ContextCompat.getMainExecutor(context)

    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    // Camera selection - persists across config changes
    var useFrontCamera by rememberSaveable { mutableStateOf(false) }
    val cameraSelector = if (useFrontCamera)
        CameraSelector.DEFAULT_FRONT_CAMERA
    else
        CameraSelector.DEFAULT_BACK_CAMERA

    val scope = rememberCoroutineScope()

    // Capture references
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }

    // Rebind all use cases when camera selector changes
    DisposableEffect(lifecycleOwner, cameraSelector) {
        var provider: ProcessCameraProvider? = null

        // Stop any ongoing recording when switching cameras
        recording?.stop()
        recording = null

        // Build all use cases
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

        val job = scope.launch {
            provider = ProcessCameraProvider.awaitInstance(context)

            // Unbind previous and rebind with new selector
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imgCapture,
                vidCapture
            )

            imageCapture = imgCapture
            videoCapture = vidCapture
        }

        onDispose {
            job.cancel()
            provider?.unbindAll()
            preview.surfaceProvider = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Camera preview
        surfaceRequest?.let { req ->
            CameraXViewfinder(
                surfaceRequest = req,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Controls row at bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Switch Camera button
            IconButton(
                onClick = { useFrontCamera = !useFrontCamera },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch camera",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Take Photo button (larger, center)
            IconButton(
                onClick = {
                    val capture = imageCapture ?: return@IconButton

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
                },
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = "Take photo",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Record Video button — gated on RECORD_AUDIO permission
            PermissionGate(permission = Permission.RECORD_AUDIO) {
                IconButton(
                    onClick = {
                        if (!Permission.RECORD_AUDIO.isGranted(context)) return@IconButton
                        val vc = videoCapture ?: return@IconButton

                        // Stop if already recording
                        recording?.let {
                            it.stop()
                            recording = null
                            return@IconButton
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
                            .withAudioEnabled()
                            .start(mainExecutor) { event ->
                                if (event is VideoRecordEvent.Finalize) {
                                    recording = null
                                }
                            }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (recording != null) Color.Red
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        )
                ) {
                    Icon(
                        imageVector = if (recording != null) Icons.Default.Stop else Icons.Default.Videocam,
                        contentDescription = if (recording != null) "Stop recording" else "Record video",
                        tint = if (recording != null) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
