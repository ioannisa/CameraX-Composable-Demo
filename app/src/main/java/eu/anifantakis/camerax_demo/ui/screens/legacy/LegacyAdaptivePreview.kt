package eu.anifantakis.camerax_demo.ui.screens.legacy

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.anifantakis.camerax_demo.ui.screens.realistic.DevicePosture
import eu.anifantakis.camerax_demo.ui.screens.realistic.rememberDevicePosture

/**
 * LEGACY: Adaptive Layout using AndroidView + PreviewView
 *
 * THE OLD WAY (Imperative):
 *  - PreviewView wrapped in AndroidView
 *  - Same posture detection works (WindowInfoTracker + WindowSizeClass)
 *  - But the preview is still a View island
 *
 * PROBLEMS WITH THIS APPROACH:
 *  1. PreviewView doesn't animate smoothly with AnimatedContent
 *     because it's a separate rendering layer (SurfaceView)
 *  2. Can't apply Compose transformations to the preview during transitions
 *  3. Z-ordering issues possible between View and Compose layers
 *  4. Preview may flicker or show black frames during layout changes
 *
 * Because of problem #1, this legacy version uses a direct if/when instead
 * of AnimatedContent — a View can only have one parent at a time, and
 * AnimatedContent temporarily keeps both layouts during transitions.
 *
 * Compare with: simplistic/AdaptivePreview.kt for the new way.
 * CameraXViewfinder animates along with everything else because
 * it's a true composable, not a View.
 */
@Composable
fun LegacyAdaptivePreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }

    DisposableEffect(Unit) {
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview
            )
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            ProcessCameraProvider.getInstance(context).get().unbindAll()
            preview.surfaceProvider = null
        }
    }

    val posture = rememberDevicePosture()

    // PROBLEM: Can't use AnimatedContent with PreviewView!
    // A View can only have one parent at a time, and AnimatedContent
    // temporarily keeps both layouts during transitions.
    // This is a key limitation of the legacy approach.
    when (posture) {
        is DevicePosture.TableTop -> {
            // Horizontal hinge: preview top, controls bottom
            Column(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    LegacyControls()
                }
            }
        }

        is DevicePosture.Book -> {
            // Vertical hinge: preview left, controls right
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    LegacyControls()
                }
            }
        }

        is DevicePosture.TwoPane -> {
            // Wide screen: side-by-side
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    LegacyControls()
                }
            }
        }

        is DevicePosture.FullScreen -> {
            // Compact: full-screen preview with controls overlaid
            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
                LegacyControls(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun LegacyControls(modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Controls", fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { /* example */ }) { Text("Fake Btn 1") }
            OutlinedButton(onClick = { /* example */ }) { Text("Fake Btn 2") }
        }
    }
}
