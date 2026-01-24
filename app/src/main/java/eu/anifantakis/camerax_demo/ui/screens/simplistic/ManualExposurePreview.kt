package eu.anifantakis.camerax_demo.ui.screens.simplistic

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.util.Range
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * NEW WAY: Manual Exposure with Camera2Interop
 *
 * KEY FINDING: CameraX doesn't expose direct ISO and shutter speed control.
 * You can only use exposure compensation (EV values), but not full manual control.
 *
 * To get FULL MANUAL CONTROL, you need Camera2Interop to access:
 *  - CaptureRequest.CONTROL_AE_MODE = OFF (disable auto-exposure)
 *  - CaptureRequest.SENSOR_SENSITIVITY (ISO: 100, 200, 400, 800...)
 *  - CaptureRequest.SENSOR_EXPOSURE_TIME (shutter: 1/1000s, 1/500s, 1/60s...)
 *
 * This demonstrates that while CameraX handles most features, some "pro camera"
 * features still require dropping down to Camera2.
 *
 * IMPORTANT: Camera2Interop is the SAME for both:
 *  - Legacy CameraX (PreviewView)
 *  - New CameraX Compose (CameraXViewfinder)
 *
 * The interop happens at the CameraX core level, not the UI level.
 */

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
@Composable
fun ManualExposurePreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Exposure controls
    var isoValue by remember { mutableFloatStateOf(0.5f) } // Normalized 0-1
    var shutterValue by remember { mutableFloatStateOf(0.5f) } // Normalized 0-1

    // Camera ranges (will be populated from camera characteristics)
    var isoRange by remember { mutableStateOf<Range<Int>?>(null) }
    var exposureTimeRange by remember { mutableStateOf<Range<Long>?>(null) }

    // Camera reference for reading characteristics
    var camera by remember { mutableStateOf<Camera?>(null) }

    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    // Calculate actual values from normalized sliders
    val actualIso = isoRange?.let { range ->
        val logMin = kotlin.math.ln(range.lower.toFloat())
        val logMax = kotlin.math.ln(range.upper.toFloat())
        kotlin.math.exp(logMin + (logMax - logMin) * isoValue).toInt().coerceIn(range.lower, range.upper)
    }

    val actualExposureTime = exposureTimeRange?.let { range ->
        // Exposure time in nanoseconds - use log scale for better UX
        val logMin = kotlin.math.ln(range.lower.toFloat())
        val logMax = kotlin.math.ln(range.upper.toFloat())
        kotlin.math.exp(logMin + (logMax - logMin) * shutterValue).toLong().coerceIn(range.lower, range.upper)
    }

    // Format shutter speed for display (convert nanoseconds to fraction)
    val shutterDisplay = actualExposureTime?.let { ns ->
        val seconds = ns / 1_000_000_000.0
        if (seconds >= 1.0) {
            "${seconds.toInt()}s"
        } else {
            "1/${(1.0 / seconds).toInt()}"
        }
    } ?: "..."

    LaunchedEffect(isoValue, shutterValue) {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)

        // Build Preview with Camera2Interop for manual exposure
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

        val preview = previewBuilder.build().apply {
            setSurfaceProvider { req -> surfaceRequests.value = req }
        }

        cameraProvider.unbindAll()
        val boundCamera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview
        )

        camera = boundCamera

        // Get camera characteristics to determine valid ranges
        val camera2Info = Camera2CameraInfo.from(boundCamera.cameraInfo)
        val characteristics = camera2Info.getCameraCharacteristic(
            CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE
        )
        val exposureRange = camera2Info.getCameraCharacteristic(
            CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE
        )

        if (isoRange == null) {
            isoRange = characteristics
        }
        if (exposureTimeRange == null) {
            exposureTimeRange = exposureRange
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Info banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color(0xFF1565C0), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = "Camera2Interop: Manual Exposure",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "CameraX doesn't expose ISO/shutter directly.\n" +
                            "We use Camera2Interop to access CaptureRequest parameters.",
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

        // Camera preview
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            surfaceRequest?.let {
                CameraXViewfinder(
                    surfaceRequest = it,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
