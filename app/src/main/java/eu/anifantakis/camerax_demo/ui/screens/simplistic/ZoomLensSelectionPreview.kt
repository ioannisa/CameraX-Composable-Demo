package eu.anifantakis.camerax_demo.ui.screens.simplistic

import android.hardware.camera2.CameraCharacteristics
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraControl
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
import androidx.camera.core.CameraSelector
import eu.anifantakis.camerax_demo.ui.screens.ZoomLensInfo
import eu.anifantakis.camerax_demo.ui.screens.enumerateZoomLenses
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Zoom-Based Lens Selection with CameraXViewfinder
 *
 * On virtually all modern Android flagships (Pixel, Samsung Galaxy, OnePlus,
 * Xiaomi, etc.), multiple back cameras (ultrawide, wide, telephoto) are
 * physical sub-cameras within a single logical camera. They are NOT
 * independently bindable — CameraX's availableCameraInfos returns only
 * the logical camera.
 *
 * This screen demonstrates the correct way to access individual lenses:
 *  1. Bind to the logical back camera (DEFAULT_BACK_CAMERA)
 *  2. Query Camera2's CameraManager for physical sub-camera IDs and focal lengths
 *  3. Calculate the zoom ratio that activates each physical sensor
 *  4. Use CameraControl.setZoomRatio() to switch — the HAL seamlessly
 *     switches between physical sensors as the zoom ratio crosses boundaries
 *
 * This is how stock camera apps (Google Camera, Samsung Camera) work.
 *
 * Compare with: LensSelectionPreview.kt which uses independent camera binding
 * and only works on the rare devices that expose sub-cameras as bindable.
 */

@ExperimentalCamera2Interop
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZoomLensSelectionPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var zoomLenses by remember { mutableStateOf<List<ZoomLensInfo>>(emptyList()) }
    var selectedLens by remember { mutableStateOf<ZoomLensInfo?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var isFrontCamera by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    // Enumerate physical sub-cameras when facing changes
    LaunchedEffect(isFrontCamera) {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)
        val facing = if (isFrontCamera) CameraCharacteristics.LENS_FACING_FRONT
                     else CameraCharacteristics.LENS_FACING_BACK
        val discovered = enumerateZoomLenses(context, cameraProvider, facing)
        zoomLenses = discovered
        selectedLens = discovered.firstOrNull { it.zoomRatio == 1.0f }
            ?: discovered.firstOrNull()
    }

    // Bind to logical camera and apply zoom when lens or facing changes
    DisposableEffect(lifecycleOwner, isFrontCamera) {
        var cameraProvider: ProcessCameraProvider? = null

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { req -> surfaceRequests.value = req }
        }

        val job = scope.launch {
            cameraProvider = ProcessCameraProvider.awaitInstance(context)
            val selector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
                           else CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
            cameraControl = camera.cameraControl

            // Apply initial zoom for the selected lens
            selectedLens?.let { lens ->
                camera.cameraControl.setZoomRatio(lens.zoomRatio)
            }
        }

        onDispose {
            job.cancel()
            cameraProvider?.unbindAll()
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
                .background(Color(0xFF1565C0).copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = selectedLens?.let { lens ->
                    "Physical Camera ID: ${lens.physicalCameraId}\n" +
                    "Focal Length: ${String.format(java.util.Locale.US, "%.1f", lens.focalLength)}mm\n" +
                    "Zoom Ratio: ${String.format(java.util.Locale.US, "%.1f", lens.zoomRatio)}x\n" +
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
            surfaceRequest?.let {
                CameraXViewfinder(
                    surfaceRequest = it,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
