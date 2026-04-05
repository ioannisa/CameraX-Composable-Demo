package eu.anifantakis.camerax_demo.ui.screens.simplistic

import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.anifantakis.camerax_demo.ui.screens.CameraLensInfo
import eu.anifantakis.camerax_demo.ui.screens.buildCameraSelectorForId
import eu.anifantakis.camerax_demo.ui.screens.enumerateCameraLenses
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Physical Lens Selection via Independent Camera Binding
 *
 * Demonstrates how to enumerate all independently bindable cameras using
 * Camera2CameraInfo and switch between them by building per-camera
 * CameraSelectors.
 *
 * IMPORTANT: On virtually all modern Android flagships (Pixel, Samsung Galaxy,
 * OnePlus, Xiaomi, etc.), this approach typically shows only one back camera
 * and one front camera. The ultrawide and telephoto sensors are physical
 * sub-cameras within the logical camera and are NOT independently bindable.
 *
 * For multi-lens access on modern flagships, see [ZoomLensSelectionPreview]
 * which uses zoom-ratio-based switching on the logical camera instead.
 *
 * This screen:
 *  1. Uses enumerateCameraLenses() to discover all cameras + focal lengths
 *  2. Shows a FilterChip per lens with descriptive labels
 *  3. Builds a per-camera CameraSelector via buildCameraSelectorForId()
 *  4. Rebinds the preview whenever the user selects a different lens
 *
 * Requires @ExperimentalCamera2Interop because Camera2CameraInfo is used
 * to read camera IDs and focal lengths from Camera2 characteristics.
 */

@ExperimentalCamera2Interop
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LensSelectionPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lenses by remember { mutableStateOf<List<CameraLensInfo>>(emptyList()) }
    var selectedLens by remember { mutableStateOf<CameraLensInfo?>(null) }

    val scope = rememberCoroutineScope()

    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    // Enumerate all cameras on launch
    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)
        val discovered = enumerateCameraLenses(cameraProvider)
        lenses = discovered
        // Default to first back camera
        if (selectedLens == null && discovered.isNotEmpty()) {
            selectedLens = discovered.first()
        }
    }

    // Rebind camera when selected lens changes
    DisposableEffect(selectedLens) {
        var cameraProvider: ProcessCameraProvider? = null
        val lens = selectedLens

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { req -> surfaceRequests.value = req }
        }

        val job = if (lens != null) {
            scope.launch {
                cameraProvider = ProcessCameraProvider.awaitInstance(context)
                val cameraSelector = buildCameraSelectorForId(lens.cameraId)
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            }
        } else null

        onDispose {
            job?.cancel()
            cameraProvider?.unbind(preview)
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
                .background(Color(0xFF1565C0).copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = selectedLens?.let { lens ->
                    "Camera ID: ${lens.cameraId}\n" +
                    "Focal Length: ${String.format("%.1f", lens.focalLength)}mm\n" +
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
            surfaceRequest?.let {
                CameraXViewfinder(
                    surfaceRequest = it,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
