package eu.anifantakis.camerax_demo.ui.screens.legacy

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.GroupableFeatures as VideoFeatures
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * LEGACY: SessionConfig demo with AndroidView + PreviewView
 *
 * Same SessionConfig logic as the Simplistic version, but uses the OLD pattern:
 *  - AndroidView wrapping PreviewView (instead of CameraXViewfinder)
 *  - Callback-based ProcessCameraProvider.getInstance() (instead of awaitInstance)
 *  - DisposableEffect for lifecycle management (instead of LaunchedEffect)
 *
 * SessionConfig was introduced in CameraX 1.5 and is stable since 1.6. The
 * binding code is identical across both UI approaches — that's the point.
 * SessionConfig is a core CameraX API, not a Compose-specific one.
 *
 * CameraX 1.6 expands feature group constants to include video-specific
 * features (VIDEO_STABILIZATION, UHD_RECORDING) from the camera-video module.
 *
 * Compare with: simplistic/SessionConfigPreview.kt for the modern Compose approach.
 *
 * Most emulators report false for all features — real device needed for meaningful results.
 */

private enum class LegacyCaptureMode(val label: String) {
    Photo("Photo"),
    Video("Video")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LegacySessionConfigPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = ContextCompat.getMainExecutor(context)

    var selectedMode by rememberSaveable { mutableStateOf(LegacyCaptureMode.Photo) }

    val previewView = remember { PreviewView(context) }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }

    // Feature group support results
    var featureSupport by remember { mutableStateOf(mapOf<String, Boolean>()) }

    // Rebind camera whenever the selected mode changes
    // SessionConfig replaces the old unbindAll()/rebind pattern.
    DisposableEffect(lifecycleOwner, selectedMode) {
        // Stop any active recording before switching modes
        recording?.stop()
        recording = null

        val preview = Preview.Builder().build()
        preview.surfaceProvider = previewView.surfaceProvider

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()

            when (selectedMode) {
                LegacyCaptureMode.Photo -> {
                    val imgCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    // SessionConfig replaces unbindAll() + bindToLifecycle(preview, imageCapture)
                    // Rebinding implicitly unbinds the previous session. No unbindAll() needed.
                    val camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        SessionConfig(useCases = listOf(preview, imgCapture))
                    )

                    imageCapture = imgCapture
                    videoCapture = null

                    // Query feature group support after binding.
                    // isSessionConfigSupported() takes a SessionConfig with the feature set
                    // as required, so we build a probe SessionConfig for each feature.
                    featureSupport = queryFeatureSupport(camera.cameraInfo)
                }
                LegacyCaptureMode.Video -> {
                    val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.FHD))
                        .build()
                    val vidCapture = VideoCapture.withOutput(recorder)

                    // SessionConfig replaces unbindAll() + bindToLifecycle(preview, videoCapture)
                    val camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        SessionConfig(useCases = listOf(preview, vidCapture))
                    )

                    imageCapture = null
                    videoCapture = vidCapture

                    featureSupport = queryFeatureSupport(camera.cameraInfo)
                }
            }
        }, mainExecutor)

        onDispose {
            ProcessCameraProvider.getInstance(context).get().unbindAll()
            preview.surfaceProvider = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Mode selector
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LegacyCaptureMode.entries.forEach { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { selectedMode = mode },
                    label = { Text(mode.label) }
                )
            }
        }

        // Feature group support panel
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth()
                .background(
                    Color(0xFF1A1A2E).copy(alpha = 0.9f),
                    RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Device Feature Support",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                if (featureSupport.isEmpty()) {
                    Text(
                        "Querying features...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                } else {
                    featureSupport.forEach { (name, supported) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                if (supported) "\u2713" else "\u2717",
                                color = if (supported) Color(0xFF4CAF50) else Color(0xFFF44336),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                name,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Camera preview
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Capture controls
            when (selectedMode) {
                LegacyCaptureMode.Photo -> {
                    Button(
                        onClick = {
                            val capture = imageCapture ?: return@Button

                            val name = "SESS_PHOTO_${System.currentTimeMillis()}.jpg"
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
                                    override fun onError(e: ImageCaptureException) {
                                        Toast.makeText(context, "Capture failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        Toast.makeText(context, "Saved: $name", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    ) {
                        Text("Take Photo")
                    }
                }
                LegacyCaptureMode.Video -> {
                    Button(
                        onClick = {
                            val vc = videoCapture ?: return@Button

                            // Stop if already recording
                            recording?.let {
                                it.stop()
                                recording = null
                                return@Button
                            }

                            // Start new recording (no audio — avoids RECORD_AUDIO permission)
                            val name = "SESS_VID_${System.currentTimeMillis()}.mp4"
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
                                .start(mainExecutor) { event ->
                                    if (event is VideoRecordEvent.Finalize) {
                                        recording = null
                                        Toast.makeText(context, "Saved: $name", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    ) {
                        Text(if (recording == null) "Record" else "Stop")
                    }
                }
            }
        }
    }
}

private fun queryFeatureSupport(info: CameraInfo): Map<String, Boolean> {
    val probePreview = Preview.Builder().build()
    val probeCapture = ImageCapture.Builder().build()
    val probeUseCases = listOf(probePreview, probeCapture)

    val features = mapOf<String, GroupableFeature>(
        "HDR (HLG10)" to GroupableFeature.HDR_HLG10,
        "60 FPS" to GroupableFeature.FPS_60,
        "Preview Stabilization" to GroupableFeature.PREVIEW_STABILIZATION,
        "Ultra HDR Images" to GroupableFeature.IMAGE_ULTRA_HDR,
        // CameraX 1.6: New video feature group constants
        "Video Stabilization" to VideoFeatures.VIDEO_STABILIZATION,
        "UHD Recording" to VideoFeatures.UHD_RECORDING,
    )

    return features.mapValues { (_, feature) ->
        try {
            val probeConfig = SessionConfig(
                useCases = probeUseCases,
                requiredFeatureGroup = setOf(feature)
            )
            info.isSessionConfigSupported(probeConfig)
        } catch (_: Exception) {
            false
        }
    }
}
