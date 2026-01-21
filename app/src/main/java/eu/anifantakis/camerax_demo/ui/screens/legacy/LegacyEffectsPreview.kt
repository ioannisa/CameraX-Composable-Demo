package eu.anifantakis.camerax_demo.ui.screens.legacy

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.os.Build
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * LEGACY: Effects with AndroidView + PreviewView
 *
 * KEY FINDING: PreviewView is still a View - it doesn't participate in
 * Compose's graphics layer. Some effects simply CANNOT work:
 *
 *  - Clip: Works in both modes (SurfaceView respects composable bounds)
 *  - Blur: NEVER works - not even in TextureView/COMPATIBLE mode!
 *
 * This is the fundamental limitation: PreviewView is a "foreign object"
 * in your Compose tree. It can't participate in Compose's rendering pipeline.
 *
 * Compare with: simplistic/EffectsPreview.kt where EMBEDDED mode enables blur
 * because CameraXViewfinder is a TRUE composable.
 */

enum class LegacyEffectType(val label: String) {
    None("None"),
    Circle("Circle Clip"),
    Blur("Blur"),
    Alpha("Alpha 50%"),
    Rotation("Rotation 15°"),
    Grayscale("Grayscale")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LegacyEffectsPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedEffect by remember { mutableStateOf(LegacyEffectType.None) }

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
        // Effect selector - using FlowRow for wrapping
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LegacyEffectType.entries.forEach { effect ->
                FilterChip(
                    selected = selectedEffect == effect,
                    onClick = { selectedEffect = effect },
                    label = { Text(effect.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Status message
        val effectWorks = selectedEffect == LegacyEffectType.Circle || selectedEffect == LegacyEffectType.None

        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(
                    if (effectWorks) Color.Green.copy(alpha = 0.2f)
                    else Color.Red.copy(alpha = 0.3f),
                    RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = when (selectedEffect) {
                    LegacyEffectType.None -> "Select an effect. Only Circle Clip works with PreviewView!"
                    LegacyEffectType.Circle -> "✓ Clip works! (This also works with CameraXViewfinder)"
                    LegacyEffectType.Blur -> "✗ BLUR DOESN'T WORK! PreviewView can't participate in Compose's graphics layer."
                    LegacyEffectType.Alpha -> "✗ ALPHA DOESN'T WORK! PreviewView ignores Compose transparency."
                    LegacyEffectType.Rotation -> "✗ ROTATION DOESN'T WORK! graphicsLayer transforms are ignored."
                    LegacyEffectType.Grayscale -> "✗ GRAYSCALE DOESN'T WORK! RenderEffect can't affect PreviewView."
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
                LegacyEffectType.None -> Modifier.fillMaxSize()

                LegacyEffectType.Circle -> Modifier
                    .size(280.dp)
                    .clip(CircleShape)

                LegacyEffectType.Blur -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier
                        .fillMaxSize()
                        .blur(20.dp)
                } else {
                    Modifier.fillMaxSize()
                }

                LegacyEffectType.Alpha -> Modifier
                    .fillMaxSize()
                    .alpha(0.5f)

                LegacyEffectType.Rotation -> Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ = 15f
                    }

                LegacyEffectType.Grayscale -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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

            AndroidView(
                factory = { previewView },
                modifier = effectModifier
            )
        }
    }
}
