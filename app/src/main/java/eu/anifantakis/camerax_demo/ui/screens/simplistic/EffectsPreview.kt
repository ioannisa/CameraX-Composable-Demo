package eu.anifantakis.camerax_demo.ui.screens.simplistic

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * NEW WAY: Effects with CameraXViewfinder
 *
 * KEY FINDING: CameraXViewfinder with EMBEDDED mode is a TRUE composable
 * that participates in Compose's graphics layer. This enables effects that
 * are IMPOSSIBLE with PreviewView:
 *
 *  - Clip: Works in all modes (same as PreviewView)
 *  - Blur: ONLY works with CameraXViewfinder + EMBEDDED mode!
 *
 * PreviewView can NEVER do blur - even in TextureView/COMPATIBLE mode -
 * because it's still a View, not a true composable.
 *
 * This is the fundamental architectural difference:
 *  - PreviewView = View wrapped in Compose (foreign object)
 *  - CameraXViewfinder EMBEDDED = True composable (native citizen)
 */

enum class EffectType(val label: String) {
    None("None"),
    Circle("Circle Clip"),
    Blur("Blur"),
    Alpha("Alpha 50%"),
    Rotation("Rotation 15°"),
    Grayscale("Grayscale")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EffectsPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedEffect by remember { mutableStateOf(EffectType.None) }
    var useEmbedded by remember { mutableStateOf(true) } // Default to EMBEDDED for effects

    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { req -> surfaceRequests.value = req }
        }
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Implementation mode toggle
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            FilterChip(
                selected = !useEmbedded,
                onClick = { useEmbedded = false },
                label = { Text("EXTERNAL") }
            )
            FilterChip(
                selected = useEmbedded,
                onClick = { useEmbedded = true },
                label = { Text("EMBEDDED") },
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Effect selector - using FlowRow for wrapping
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            EffectType.entries.forEach { effect ->
                FilterChip(
                    selected = selectedEffect == effect,
                    onClick = { selectedEffect = effect },
                    label = { Text(effect.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Status message
        val needsEmbedded = selectedEffect in listOf(
            EffectType.Blur, EffectType.Alpha,
            EffectType.Rotation, EffectType.Grayscale
        )
        val effectWorksHere = useEmbedded || selectedEffect == EffectType.Circle || selectedEffect == EffectType.None

        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(
                    if (effectWorksHere) Color.Green.copy(alpha = 0.2f)
                    else Color.Red.copy(alpha = 0.3f),
                    RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = when {
                    selectedEffect == EffectType.None -> "Select an effect to test. Most require EMBEDDED mode!"
                    selectedEffect == EffectType.Circle -> "✓ Clip works in both modes (also works with PreviewView)"
                    !useEmbedded && needsEmbedded -> "✗ ${selectedEffect.label} doesn't work in EXTERNAL. Switch to EMBEDDED!"
                    useEmbedded && needsEmbedded -> "✓ ${selectedEffect.label} WORKS! This is IMPOSSIBLE with PreviewView!"
                    else -> "Testing ${selectedEffect.label}..."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }

        // Preview with effects
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            @Suppress("DEPRECATION")
            val effectModifier = when (selectedEffect) {
                EffectType.None -> Modifier.fillMaxSize()

                EffectType.Circle -> Modifier
                    .size(280.dp)
                    .clip(CircleShape)

                EffectType.Blur -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier
                        .fillMaxSize()
                        .blur(20.dp)
                } else {
                    Modifier.fillMaxSize()
                }

                EffectType.Alpha -> Modifier
                    .fillMaxSize()
                    .alpha(0.5f)

                EffectType.Rotation -> Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ = 15f
                    }

                EffectType.Grayscale -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val saturationMatrix = ColorMatrix().apply { setSaturation(0f) }
                            renderEffect = RenderEffect
                                .createColorFilterEffect(ColorMatrixColorFilter(saturationMatrix))
                                .asComposeRenderEffect()
                        }
                } else {
                    Modifier.fillMaxSize()
                }
            }

            // THE KEY: Just pass implementationMode as a parameter!
            surfaceRequest?.let {
                CameraXViewfinder(
                    surfaceRequest = it,
                    implementationMode = if (useEmbedded) ImplementationMode.EMBEDDED
                                         else ImplementationMode.EXTERNAL,
                    modifier = effectModifier
                )
            }
        }
    }
}
