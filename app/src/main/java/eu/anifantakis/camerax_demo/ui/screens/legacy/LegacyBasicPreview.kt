package eu.anifantakis.camerax_demo.ui.screens.legacy

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * LEGACY: Basic CameraX Preview using AndroidView + PreviewView
 *
 * THE OLD WAY (Imperative):
 *  - Create a PreviewView (Android View)
 *  - Wrap it in AndroidView composable
 *  - Hand off the surface provider to CameraX
 *
 * PROBLEMS WITH THIS APPROACH:
 *  1. View island inside Compose - different lifecycle, different paradigm
 *  2. PreviewView is a black box - can't clip, blur, or animate like Compose
 *  3. Manual coordination between View and Compose systems
 *  4. Imperative hand-off: "Here's a View, CameraX draw on it"
 *
 * Compare with: simplistic/BasicCameraPreview.kt for the new declarative way
 */
@Composable
fun LegacyBasicPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // THE OLD WAY: Create and remember a PreviewView
    val previewView = remember { PreviewView(context) }

    // Bind camera using the old blocking API
    LaunchedEffect(previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()

            // IMPERATIVE HAND-OFF:
            // "I have a View. CameraX, draw on it."
            preview.surfaceProvider = previewView.surfaceProvider

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview
            )
        }, ContextCompat.getMainExecutor(context))
    }

    // THE VIEW ISLAND: AndroidView wrapping PreviewView
    // This is a mini legacy world embedded inside Compose
    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}
