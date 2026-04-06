package eu.anifantakis.camerax_demo.ui.screens.legacy.camerax_1_6_features

import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.GroupableFeatures as VideoFeatures
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * LEGACY: CameraX 1.6 Feature Groups Demo
 *
 * Same feature probing as the Simplistic version but uses:
 *  - ProcessCameraProvider.getInstance() + addListener (callback-based)
 *  - AndroidView wrapping PreviewView
 *
 * Compare with: simplistic/camerax_1_6_features/FeatureGroupsDemo.kt
 */
@Composable
fun LegacyFeatureGroupsDemo() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = ContextCompat.getMainExecutor(context)

    val previewView = remember { PreviewView(context) }
    var cameraInfo by remember { mutableStateOf<CameraInfo?>(null) }
    var featureSupport by remember { mutableStateOf(mapOf<String, Boolean>()) }

    DisposableEffect(lifecycleOwner) {
        val preview = Preview.Builder().build()
        preview.surfaceProvider = previewView.surfaceProvider

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val camera = provider.bindToLifecycle(
                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview
            )
            cameraInfo = camera.cameraInfo

            // Probe features
            val probeUseCases = listOf(Preview.Builder().build(), ImageCapture.Builder().build())
            val features = mapOf(
                "HDR (HLG10)" to GroupableFeature.HDR_HLG10,
                "60 FPS" to GroupableFeature.FPS_60,
                "Preview Stabilization" to GroupableFeature.PREVIEW_STABILIZATION,
                "Ultra HDR Images" to GroupableFeature.IMAGE_ULTRA_HDR,
                "Video Stabilization" to VideoFeatures.VIDEO_STABILIZATION,
                "UHD Recording" to VideoFeatures.UHD_RECORDING,
                "FHD Recording" to VideoFeatures.FHD_RECORDING,
            )
            featureSupport = features.mapValues { (_, feature) ->
                try {
                    camera.cameraInfo.isSessionConfigSupported(
                        SessionConfig(useCases = probeUseCases, requiredFeatureGroup = setOf(feature))
                    )
                } catch (_: Exception) { false }
            }
        }, mainExecutor)

        onDispose {
            ProcessCameraProvider.getInstance(context).get().unbindAll()
            preview.surfaceProvider = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .background(Color(0xFF1A1A2E).copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("isSessionConfigSupported() Results", style = MaterialTheme.typography.titleSmall, color = Color.White)
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

        Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp)) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        }
    }
}
