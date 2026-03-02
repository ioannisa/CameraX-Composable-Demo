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
import androidx.compose.runtime.key
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
 * IMPLEMENTATION MODES (PreviewView):
 *  - PERFORMANCE (SurfaceView) — Hardware overlay outside the rendering pipeline.
 *    Best latency, but only Clip works. All other Compose effects are ignored.
 *  - COMPATIBLE (TextureView) — Texture participates in the rendering pipeline.
 *    The demonstrated visual effects (blur, alpha, rotation, RenderEffect, clip) work.
 *
 * These mirror the same high-level tradeoff as CameraXViewfinder's EXTERNAL / EMBEDDED:
 *  - PERFORMANCE ≈ EXTERNAL: Only clip works
 *  - COMPATIBLE ≈ EMBEDDED: The demonstrated effects work
 *
 * So why prefer CameraXViewfinder? Not because of effect support — because of
 * architecture. With PreviewView you're managing a View lifecycle inside Compose
 * (two systems). With CameraXViewfinder, the preview is a native composable
 * (one system, idiomatic Compose, unidirectional data flow).
 *
 * Compare with: simplistic/EffectsPreview.kt for the CameraXViewfinder equivalent.
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

    // Toggle between PERFORMANCE (SurfaceView) and COMPATIBLE (TextureView).
    // These are the legacy equivalents of CameraXViewfinder's EXTERNAL / EMBEDDED.
    var useCompatible by remember { mutableStateOf(false) }

    // Recreate PreviewView when mode changes — implementationMode must be set before binding.
    val previewView = remember(useCompatible) {
        PreviewView(context).apply {
            implementationMode = if (useCompatible)
                PreviewView.ImplementationMode.COMPATIBLE
            else
                PreviewView.ImplementationMode.PERFORMANCE
        }
    }

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
        // Implementation mode toggle — mirrors EffectsPreview's EXTERNAL/EMBEDDED toggle
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            FilterChip(
                selected = !useCompatible,
                onClick = { useCompatible = false },
                label = { Text("PERFORMANCE") }
            )
            FilterChip(
                selected = useCompatible,
                onClick = { useCompatible = true },
                label = { Text("COMPATIBLE") },
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Effect selector
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
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

        // Status message — varies by mode AND effect
        val modeName = if (useCompatible) "COMPATIBLE" else "PERFORMANCE"
        val needsCompatible = selectedEffect !in listOf(
            LegacyEffectType.None, LegacyEffectType.Circle
        )
        val effectWorks = useCompatible || !needsCompatible

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
                text = when {
                    selectedEffect == LegacyEffectType.None ->
                        "Mode: $modeName. Select an effect to test."

                    selectedEffect == LegacyEffectType.Circle ->
                        "✓ Clip works in both modes! (Also works with CameraXViewfinder)"

                    useCompatible ->
                        "✓ ${selectedEffect.label} works in COMPATIBLE! TextureView participates in the rendering pipeline, so Compose effects apply."

                    else ->
                        "✗ ${selectedEffect.label} doesn't work in PERFORMANCE. SurfaceView is a hardware overlay outside the rendering pipeline. Switch to COMPATIBLE."
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

            // key() forces Compose to tear down and recreate the AndroidView when
            // the mode changes — necessary because factory only runs once per composition.
            key(useCompatible) {
                AndroidView(
                    factory = { previewView },
                    modifier = effectModifier
                )
            }
        }
    }
}
