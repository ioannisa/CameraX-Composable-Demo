package eu.anifantakis.camerax_demo.ui.screens.legacy

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

/**
 * LEGACY: ScaleType with AndroidView + PreviewView
 *
 * PreviewView only supports a fixed ScaleType enum that combines scaling AND alignment
 * into a single value. There is NO separate Alignment parameter.
 *
 * Available ScaleTypes:
 *  - FILL_CENTER, FILL_START, FILL_END  → crop-based (like ContentScale.Crop)
 *  - FIT_CENTER, FIT_START, FIT_END     → letterbox-based (like ContentScale.Fit)
 *
 * Compare with: simplistic/ContentScalePreview.kt where CameraXViewfinder accepts
 * independent ContentScale AND Alignment — just like Image().
 */

private data class LegacyScaleOption(val label: String, val scaleType: PreviewView.ScaleType)

private val legacyScaleOptions = listOf(
    LegacyScaleOption("FILL_CENTER", PreviewView.ScaleType.FILL_CENTER),
    LegacyScaleOption("FILL_START", PreviewView.ScaleType.FILL_START),
    LegacyScaleOption("FILL_END", PreviewView.ScaleType.FILL_END),
    LegacyScaleOption("FIT_CENTER", PreviewView.ScaleType.FIT_CENTER),
    LegacyScaleOption("FIT_START", PreviewView.ScaleType.FIT_START),
    LegacyScaleOption("FIT_END", PreviewView.ScaleType.FIT_END),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LegacyContentScalePreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedScale by remember { mutableStateOf(legacyScaleOptions[0]) }  // FILL_CENTER

    val previewView = remember { PreviewView(context) }

    DisposableEffect(previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            preview.surfaceProvider = previewView.surfaceProvider

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview
            )
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            ProcessCameraProvider.getInstance(context).get().unbindAll()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ScaleType selector
        Text(
            "PreviewView.ScaleType",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 12.dp, top = 8.dp)
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            legacyScaleOptions.forEach { option ->
                FilterChip(
                    selected = selectedScale == option,
                    onClick = { selectedScale = option },
                    label = { Text(option.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Status text
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(
                    Color.Yellow.copy(alpha = 0.25f),
                    RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = "ScaleType.${selectedScale.label}\n" +
                        "PreviewView has no Alignment parameter — CameraXViewfinder adds this!",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }

        // Preview with selected ScaleType — applied via AndroidView update block
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { previewView },
                update = { view -> view.scaleType = selectedScale.scaleType },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
