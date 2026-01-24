package eu.anifantakis.camerax_demo.ui.screens.legacy

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.util.Range
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * LEGACY: Manual Exposure with Camera2Interop + PreviewView
 *
 * KEY FINDING: Camera2Interop works EXACTLY THE SAME regardless of whether
 * you use PreviewView (legacy) or CameraXViewfinder (new Compose way).
 *
 * The interop happens at the CameraX core level when building use cases,
 * NOT at the preview/UI level. This demonstrates that:
 *
 *  1. Camera2Interop is about extending CameraX capabilities
 *  2. The choice of PreviewView vs CameraXViewfinder is PURELY about UI
 *  3. Both approaches share the exact same camera feature set
 *
 * Compare with: simplistic/ManualExposurePreview.kt to see identical
 * Camera2Interop usage with CameraXViewfinder.
 */

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
@Composable
fun LegacyManualExposurePreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Exposure controls
    var isoValue by remember { mutableFloatStateOf(0.5f) } // Normalized 0-1
    var shutterValue by remember { mutableFloatStateOf(0.5f) } // Normalized 0-1

    // Camera ranges (will be populated from camera characteristics)
    var isoRange by remember { mutableStateOf<Range<Int>?>(null) }
    var exposureTimeRange by remember { mutableStateOf<Range<Long>?>(null) }

    // Camera reference
    var camera by remember { mutableStateOf<Camera?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }

    // Calculate actual values from normalized sliders
    val actualIso = isoRange?.let { range ->
        val logMin = kotlin.math.ln(range.lower.toFloat())
        val logMax = kotlin.math.ln(range.upper.toFloat())
        kotlin.math.exp(logMin + (logMax - logMin) * isoValue).toInt().coerceIn(range.lower, range.upper)
    }

    val actualExposureTime = exposureTimeRange?.let { range ->
        val logMin = kotlin.math.ln(range.lower.toFloat())
        val logMax = kotlin.math.ln(range.upper.toFloat())
        kotlin.math.exp(logMin + (logMax - logMin) * shutterValue).toLong().coerceIn(range.lower, range.upper)
    }

    // Format shutter speed for display
    val shutterDisplay = actualExposureTime?.let { ns ->
        val seconds = ns / 1_000_000_000.0
        if (seconds >= 1.0) {
            "${seconds.toInt()}s"
        } else {
            "1/${(1.0 / seconds).toInt()}"
        }
    } ?: "..."

    // Rebind camera when exposure values change
    DisposableEffect(isoValue, shutterValue) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Build Preview with Camera2Interop - EXACTLY THE SAME AS SIMPLISTIC!
            val previewBuilder = Preview.Builder()

            // THE KEY: Use Camera2Interop to set capture request options
            val camera2Interop = Camera2Interop.Extender(previewBuilder)

            // Disable auto-exposure to enable manual control
            camera2Interop.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_OFF
            )

            // Set ISO (sensor sensitivity) if we have a valid value
            actualIso?.let { iso ->
                camera2Interop.setCaptureRequestOption(
                    CaptureRequest.SENSOR_SENSITIVITY,
                    iso
                )
            }

            // Set shutter speed (exposure time) if we have a valid value
            actualExposureTime?.let { exposureTime ->
                camera2Interop.setCaptureRequestOption(
                    CaptureRequest.SENSOR_EXPOSURE_TIME,
                    exposureTime
                )
            }

            val preview = previewBuilder.build()
            // Legacy approach: set surface provider on PreviewView
            preview.surfaceProvider = previewView.surfaceProvider

            cameraProvider.unbindAll()
            val boundCamera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview
            )

            camera = boundCamera

            // Get camera characteristics to determine valid ranges
            val camera2Info = Camera2CameraInfo.from(boundCamera.cameraInfo)
            val sensitivityRange = camera2Info.getCameraCharacteristic(
                CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE
            )
            val exposureRange = camera2Info.getCameraCharacteristic(
                CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE
            )

            if (isoRange == null) {
                isoRange = sensitivityRange
            }
            if (exposureTimeRange == null) {
                exposureTimeRange = exposureRange
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            ProcessCameraProvider.getInstance(context).get().unbindAll()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Info banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color(0xFF6A1B9A), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = "Camera2Interop: Manual Exposure (Legacy)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Same Camera2Interop code as Simplistic!\n" +
                            "Only difference: PreviewView vs CameraXViewfinder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // ISO Slider
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ISO", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = actualIso?.toString() ?: "...",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = isoValue,
                onValueChange = { isoValue = it },
                modifier = Modifier.fillMaxWidth()
            )
            isoRange?.let { range ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${range.lower}", style = MaterialTheme.typography.bodySmall)
                    Text("${range.upper}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Shutter Speed Slider
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Shutter", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = shutterDisplay,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = shutterValue,
                onValueChange = { shutterValue = it },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Camera preview with PreviewView
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
