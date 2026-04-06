package eu.anifantakis.camerax_demo.ui.screens.simplistic.camerax_1_6_features

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.video.GroupableFeatures as VideoFeatures
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * CameraX 1.6: Feature Groups Demo
 *
 * Demonstrates [CameraInfo.isSessionConfigSupported] — renamed in 1.6 from
 * the 1.5-era `isFeatureGroupSupported()`. The rename reflects that it now
 * accepts any [SessionConfig] subtype (including [ExtensionSessionConfig] and
 * [HighSpeedVideoSessionConfig]), not just feature-group queries.
 *
 * What's new in 1.6 specifically:
 *  - The method name: isFeatureGroupSupported() → isSessionConfigSupported()
 *  - Video feature groups from camera-video: VIDEO_STABILIZATION, UHD_RECORDING,
 *    FHD_RECORDING, HD_RECORDING, SD_RECORDING
 *  - CameraEffect and ImageAnalysis now work with feature groups (1.5 limitation lifted)
 *
 * Core features (already in 1.5): HDR_HLG10, FPS_60, PREVIEW_STABILIZATION, IMAGE_ULTRA_HDR
 *
 * Most emulators report false for everything — real device needed.
 */
@Composable
fun FeatureGroupsDemo() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    var cameraInfo by remember { mutableStateOf<CameraInfo?>(null) }
    var featureSupport by remember { mutableStateOf(mapOf<String, Boolean>()) }

    // Bind a basic preview to get CameraInfo
    DisposableEffect(lifecycleOwner) {
        var provider: ProcessCameraProvider? = null
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { req -> surfaceRequests.value = req }
        }

        val job = scope.launch {
            provider = ProcessCameraProvider.awaitInstance(context)
            val camera = provider.bindToLifecycle(
                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview
            )
            cameraInfo = camera.cameraInfo
        }

        onDispose {
            job.cancel()
            provider?.unbindAll()
            preview.surfaceProvider = null
        }
    }

    // Probe each feature individually via isSessionConfigSupported()
    LaunchedEffect(cameraInfo) {
        val info = cameraInfo ?: return@LaunchedEffect

        val probeUseCases = listOf(
            Preview.Builder().build(),
            ImageCapture.Builder().build()
        )

        val features = mapOf(
            // Core features
            "HDR (HLG10)" to GroupableFeature.HDR_HLG10,
            "60 FPS" to GroupableFeature.FPS_60,
            "Preview Stabilization" to GroupableFeature.PREVIEW_STABILIZATION,
            "Ultra HDR Images" to GroupableFeature.IMAGE_ULTRA_HDR,
            // CameraX 1.6: Video-specific feature groups
            "Video Stabilization" to VideoFeatures.VIDEO_STABILIZATION,
            "UHD Recording" to VideoFeatures.UHD_RECORDING,
            "FHD Recording" to VideoFeatures.FHD_RECORDING,
        )

        featureSupport = features.mapValues { (_, feature) ->
            try {
                info.isSessionConfigSupported(
                    SessionConfig(
                        useCases = probeUseCases,
                        requiredFeatureGroup = setOf(feature)
                    )
                )
            } catch (_: Exception) {
                false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Feature support panel
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .background(Color(0xFF1A1A2E).copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "isSessionConfigSupported() Results",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
                Text(
                    "CameraX 1.6 — query hardware feature combos before binding",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )

                if (featureSupport.isEmpty()) {
                    Text("Querying...", style = MaterialTheme.typography.bodySmall, color = Color.White)
                } else {
                    featureSupport.forEach { (name, supported) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                if (supported) "\u2713" else "\u2717",
                                color = if (supported) Color(0xFF4CAF50) else Color(0xFFF44336),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(name, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }
                }
            }
        }

        // Camera preview
        Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp)) {
            surfaceRequest?.let {
                CameraXViewfinder(surfaceRequest = it, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
