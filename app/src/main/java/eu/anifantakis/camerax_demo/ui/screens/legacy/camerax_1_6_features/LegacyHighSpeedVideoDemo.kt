package eu.anifantakis.camerax_demo.ui.screens.legacy.camerax_1_6_features

import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.HighSpeedVideoSessionConfig
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.anifantakis.camerax_demo.ui.components.Permission
import eu.anifantakis.camerax_demo.ui.components.PermissionGate
import java.io.File

/**
 * LEGACY: CameraX 1.6 High-Speed Video Demo
 *
 * Same high-speed capability query and recording as the Simplistic version
 * but uses ProcessCameraProvider.getInstance() + addListener + PreviewView.
 *
 * Compare with: simplistic/camerax_1_6_features/HighSpeedVideoDemo.kt
 */
@SuppressLint("MissingPermission")
@Composable
fun LegacyHighSpeedVideoDemo() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = ContextCompat.getMainExecutor(context)

    val previewView = remember { PreviewView(context) }
    var statusText by remember { mutableStateOf("Querying high-speed capabilities...") }
    var isSupported by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<Recorder?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val cameraInfo = provider.getCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA)

            val capabilities = Recorder.getHighSpeedVideoCapabilities(cameraInfo)

            if (capabilities == null) {
                statusText = "High-speed NOT supported.\n\n" +
                    "Samsung: proprietary HAL.\nPixel 6+: best support."
                isSupported = false

                val preview = Preview.Builder().build()
                preview.surfaceProvider = previewView.surfaceProvider
                provider.bindToLifecycle(
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview
                )
                return@addListener
            }

            val qualities = capabilities.getSupportedQualities(DynamicRange.SDR)
            val rec = Recorder.Builder()
                .setQualitySelector(QualitySelector.fromOrderedList(qualities))
                .build()
            val videoCapture = VideoCapture.withOutput(rec)
            val preview = Preview.Builder().build()
            preview.surfaceProvider = previewView.surfaceProvider

            val config = HighSpeedVideoSessionConfig.Builder(videoCapture)
                .setPreview(preview)
                .setSlowMotionEnabled(true)
                .build()

            val frameRates = cameraInfo.getSupportedFrameRateRanges(config)
            val bestRate = frameRates.maxByOrNull { it.upper } ?: frameRates.first()

            val finalConfig = HighSpeedVideoSessionConfig.Builder(videoCapture)
                .setPreview(preview)
                .setSlowMotionEnabled(true)
                .setFrameRateRange(bestRate)
                .build()

            provider.bindToLifecycle(
                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, finalConfig
            )

            recorder = rec
            isSupported = true
            statusText = "High-speed supported!\n" +
                "Frame rates: ${frameRates.joinToString { "${it.lower}-${it.upper}fps" }}\n" +
                "Using: ${bestRate.upper}fps slow-motion"
        }, mainExecutor)

        onDispose {
            recording?.stop()
            ProcessCameraProvider.getInstance(context).get().unbindAll()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .background(
                    if (isSupported) Color(0xFF1B5E20).copy(alpha = 0.85f)
                    else Color(0xFFB71C1C).copy(alpha = 0.85f),
                    RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("HighSpeedVideoSessionConfig", style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text(statusText, style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
        }

        Box(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            if (isSupported) {
                PermissionGate(permission = Permission.RECORD_AUDIO) {
                    Button(
                        onClick = {
                            if (!Permission.RECORD_AUDIO.isGranted(context)) return@Button
                            val rec = recorder ?: return@Button

                            recording?.let { it.stop(); recording = null; return@Button }

                            val file = File(context.cacheDir, "slowmo_${System.currentTimeMillis()}.mp4")
                            recording = rec
                                .prepareRecording(context, FileOutputOptions.Builder(file).build())
                                .withAudioEnabled()
                                .start(mainExecutor) { event ->
                                    if (event is VideoRecordEvent.Finalize) { recording = null }
                                }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    ) {
                        Text(if (recording == null) "Record Slow-Mo" else "Stop")
                    }
                }
            }
        }
    }
}
