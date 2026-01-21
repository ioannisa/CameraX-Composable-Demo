package eu.anifantakis.camerax_demo.ui.screens.legacy

import android.app.Activity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * LEGACY: Adaptive Layout using AndroidView + PreviewView
 *
 * THE OLD WAY (Imperative):
 *  - PreviewView wrapped in AndroidView
 *  - Same WindowSizeClass detection works (Compose feature)
 *  - But the preview is still a View island
 *
 * PROBLEMS WITH THIS APPROACH:
 *  1. PreviewView doesn't animate smoothly with AnimatedContent
 *     because it's a separate rendering layer (SurfaceView)
 *  2. Can't apply Compose transformations to the preview during transitions
 *  3. Z-ordering issues possible between View and Compose layers
 *  4. Preview may flicker or show black frames during layout changes
 *
 * Compare with: simplistic/AdaptivePreview.kt for the new way
 * The new CameraXViewfinder animates along with everything else
 * because it's a true composable, not a View.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun LegacyAdaptivePreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }

    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

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

    // WindowSizeClass works fine - it's a Compose feature
    val windowSizeClass = calculateWindowSizeClass(context as Activity)
    val isExpanded = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    // PROBLEM: Can't use AnimatedContent with PreviewView!
    // A View can only have one parent at a time, and AnimatedContent
    // temporarily keeps both layouts during transitions.
    //
    // This is a key limitation of the legacy approach:
    // - No smooth animations between layouts
    // - Abrupt layout changes when orientation changes
    //
    // Compare with simplistic/AdaptivePreview.kt where AnimatedContent
    // works beautifully because CameraXViewfinder is a true composable.
    if (isExpanded) {
        // Medium/Expanded: Row with preview + controls side by side
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // THE VIEW ISLAND - can't animate with Compose
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Controls panel on the right
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LegacyControls()
            }
        }
    } else {
        // Compact: Full-screen preview with controls overlaid
        Box(Modifier.fillMaxSize()) {
            // THE VIEW ISLAND
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
            LegacyControls(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun LegacyControls(modifier: Modifier = Modifier) {
    Row(modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { /* example */ }) { Text("Grid") }
        OutlinedButton(onClick = { /* example */ }) { Text("Level") }
    }
}
