package eu.anifantakis.camerax_demo.ui.screens.legacy

import android.hardware.camera2.CameraCharacteristics
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
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
import eu.anifantakis.camerax_demo.ui.screens.ZoomLensInfo
import eu.anifantakis.camerax_demo.ui.screens.enumerateZoomLenses

/**
 * LEGACY: Zoom-Based Lens Selection with AndroidView + PreviewView
 *
 * Same zoom-ratio approach as the Simplistic version but uses:
 *  - DisposableEffect instead of LaunchedEffect for enumeration
 *  - ProcessCameraProvider.getInstance() + addListener (callback-based)
 *  - AndroidView wrapping PreviewView
 *
 * Compare with: simplistic/ZoomLensSelectionPreview.kt for the modern Compose approach.
 */

@ExperimentalCamera2Interop
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LegacyZoomLensSelectionPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = ContextCompat.getMainExecutor(context)

    var zoomLenses by remember { mutableStateOf<List<ZoomLensInfo>>(emptyList()) }
    var selectedLens by remember { mutableStateOf<ZoomLensInfo?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var isFrontCamera by remember { mutableStateOf(false) }

    val previewView = remember { PreviewView(context) }

    // Enumerate physical sub-cameras and bind on facing change
    DisposableEffect(isFrontCamera) {
        val preview = Preview.Builder().build()
        preview.surfaceProvider = previewView.surfaceProvider

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Enumerate zoom lenses
            val facing = if (isFrontCamera) CameraCharacteristics.LENS_FACING_FRONT
                         else CameraCharacteristics.LENS_FACING_BACK
            val discovered = enumerateZoomLenses(context, cameraProvider, facing)
            zoomLenses = discovered
            val defaultLens = discovered.firstOrNull { it.zoomRatio == 1.0f }
                ?: discovered.firstOrNull()
            selectedLens = defaultLens

            // Bind to logical camera
            val selector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
                           else CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
            cameraControl = camera.cameraControl

            // Apply initial zoom
            defaultLens?.let { camera.cameraControl.setZoomRatio(it.zoomRatio) }
        }, mainExecutor)

        onDispose {
            ProcessCameraProvider.getInstance(context).get().unbindAll()
            preview.surfaceProvider = null
            cameraControl = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Front / Back toggle
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = !isFrontCamera,
                onClick = { isFrontCamera = false },
                label = { Text("Back", style = MaterialTheme.typography.labelSmall) }
            )
            FilterChip(
                selected = isFrontCamera,
                onClick = { isFrontCamera = true },
                label = { Text("Front", style = MaterialTheme.typography.labelSmall) }
            )
        }

        // Zoom lens selector chips
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            zoomLenses.forEach { lens ->
                FilterChip(
                    selected = selectedLens == lens,
                    onClick = {
                        selectedLens = lens
                        cameraControl?.setZoomRatio(lens.zoomRatio)
                    },
                    label = { Text(lens.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Status info box
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth()
                .background(Color(0xFF6A1B9A).copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = selectedLens?.let { lens ->
                    "Physical Camera ID: ${lens.physicalCameraId}\n" +
                    "Focal Length: ${String.format("%.1f", lens.focalLength)}mm\n" +
                    "Zoom Ratio: ${String.format("%.1f", lens.zoomRatio)}x\n" +
                    "Total lenses found: ${zoomLenses.size}"
                } ?: "Discovering physical sub-cameras...",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
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
        }
    }
}
