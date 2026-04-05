package eu.anifantakis.camerax_demo.ui.screens.legacy

import androidx.camera.camera2.interop.ExperimentalCamera2Interop
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
import eu.anifantakis.camerax_demo.ui.screens.CameraLensInfo
import eu.anifantakis.camerax_demo.ui.screens.buildCameraSelectorForId
import eu.anifantakis.camerax_demo.ui.screens.enumerateCameraLenses

/**
 * LEGACY: Physical Lens Selection via Independent Camera Binding
 *
 * Same lens enumeration as the Simplistic version but uses:
 *  - DisposableEffect instead of LaunchedEffect
 *  - ProcessCameraProvider.getInstance() + addListener (callback-based)
 *  - AndroidView wrapping PreviewView
 *
 * IMPORTANT: On most modern flagships this only shows 1 back + 1 front camera.
 * See [LegacyZoomLensSelectionPreview] for zoom-ratio-based multi-lens access.
 *
 * Compare with: simplistic/LensSelectionPreview.kt for the modern Compose approach.
 */

@ExperimentalCamera2Interop
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LegacyLensSelectionPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = ContextCompat.getMainExecutor(context)

    var lenses by remember { mutableStateOf<List<CameraLensInfo>>(emptyList()) }
    var selectedLens by remember { mutableStateOf<CameraLensInfo?>(null) }

    val previewView = remember { PreviewView(context) }

    // Enumerate all cameras on first composition
    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val discovered = enumerateCameraLenses(cameraProvider)
            lenses = discovered
            if (selectedLens == null && discovered.isNotEmpty()) {
                selectedLens = discovered.first()
            }
        }, mainExecutor)

        onDispose { }
    }

    // Rebind camera when selected lens changes
    DisposableEffect(lifecycleOwner, selectedLens) {
        val preview = Preview.Builder().build()
        preview.surfaceProvider = previewView.surfaceProvider

        val lens = selectedLens
        if (lens != null) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val cameraSelector = buildCameraSelectorForId(lens.cameraId)

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            }, mainExecutor)
        }

        onDispose {
            ProcessCameraProvider.getInstance(context).get().unbindAll()
            preview.surfaceProvider = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Lens selector chips
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            lenses.forEach { lens ->
                FilterChip(
                    selected = selectedLens == lens,
                    onClick = { selectedLens = lens },
                    label = { Text(lens.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Camera info status box
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth()
                .background(Color(0xFF6A1B9A).copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = selectedLens?.let { lens ->
                    "Camera ID: ${lens.cameraId}\n" +
                    "Focal Length: ${String.format(java.util.Locale.US, "%.1f", lens.focalLength)}mm\n" +
                    "Facing: ${if (lens.lensFacing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) "Back" else "Front"}"
                } ?: "Discovering cameras...",
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
