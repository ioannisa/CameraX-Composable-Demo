package eu.anifantakis.camerax_demo.ui.screens.simplistic

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * NEW WAY: ContentScale & Alignment with CameraXViewfinder
 *
 * KEY FINDING: CameraXViewfinder accepts standard Compose ContentScale and Alignment
 * parameters — just like Image(). This means the camera preview behaves like any other
 * composable when it comes to scaling and positioning.
 *
 * PreviewView only offers a limited ScaleType enum with no separate Alignment control.
 * Compare with: legacy/LegacyContentScalePreview.kt to see the difference.
 */

private data class ScaleOption(val label: String, val scale: ContentScale)
private data class AlignmentOption(val label: String, val alignment: Alignment)

private val scaleOptions = listOf(
    ScaleOption("Crop", ContentScale.Crop),
    ScaleOption("Fit", ContentScale.Fit),
    ScaleOption("FillBounds", ContentScale.FillBounds),
    ScaleOption("FillWidth", ContentScale.FillWidth),
    ScaleOption("FillHeight", ContentScale.FillHeight),
    ScaleOption("Inside", ContentScale.Inside),
    ScaleOption("None", ContentScale.None),
)

private val alignmentOptions = listOf(
    AlignmentOption("Center", Alignment.Center),
    AlignmentOption("TopCenter", Alignment.TopCenter),
    AlignmentOption("TopStart", Alignment.TopStart),
    AlignmentOption("TopEnd", Alignment.TopEnd),
    AlignmentOption("BottomCenter", Alignment.BottomCenter),
    AlignmentOption("BottomStart", Alignment.BottomStart),
    AlignmentOption("BottomEnd", Alignment.BottomEnd),
    AlignmentOption("CenterStart", Alignment.CenterStart),
    AlignmentOption("CenterEnd", Alignment.CenterEnd),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContentScalePreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedScale by remember { mutableStateOf(scaleOptions[0]) }  // Crop
    var selectedAlignment by remember { mutableStateOf(alignmentOptions[0]) }  // Center

    val scope = rememberCoroutineScope()

    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        var cameraProvider: ProcessCameraProvider? = null
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { req -> surfaceRequests.value = req }
        }

        val job = scope.launch {
            cameraProvider = ProcessCameraProvider.awaitInstance(context)
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview
            )
        }

        onDispose {
            job.cancel()
            cameraProvider?.unbind(preview)
            preview.surfaceProvider = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Controls area — scrollable for small screens
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // ContentScale selector
            Text(
                "ContentScale",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 12.dp, top = 8.dp)
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                scaleOptions.forEach { option ->
                    FilterChip(
                        selected = selectedScale == option,
                        onClick = { selectedScale = option },
                        label = { Text(option.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Alignment selector
            Text(
                "Alignment",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                alignmentOptions.forEach { option ->
                    FilterChip(
                        selected = selectedAlignment == option,
                        onClick = { selectedAlignment = option },
                        label = { Text(option.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Status text
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(
                        Color.Green.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = "ContentScale.${selectedScale.label} + Alignment.${selectedAlignment.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }

        // Camera preview with selected ContentScale and Alignment
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            surfaceRequest?.let { req ->
                CameraXViewfinder(
                    surfaceRequest = req,
                    contentScale = selectedScale.scale,
                    alignment = selectedAlignment.alignment,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
