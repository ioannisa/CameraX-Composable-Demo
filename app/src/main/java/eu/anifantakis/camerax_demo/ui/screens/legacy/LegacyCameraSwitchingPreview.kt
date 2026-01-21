package eu.anifantakis.camerax_demo.ui.screens.legacy

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * LEGACY: Camera Switching using AndroidView + PreviewView
 *
 * THE OLD WAY (Imperative):
 *  - Manually rebind camera when selector changes
 *  - DisposableEffect to handle cleanup
 *  - PreviewView wrapped in AndroidView
 *
 * PROBLEMS WITH THIS APPROACH:
 *  1. Must manually coordinate state changes with View updates
 *  2. DisposableEffect for cleanup adds complexity
 *  3. Blocking .get() call (though we use addListener here)
 *  4. Two separate systems (Compose state + View) must stay in sync
 *
 * Compare with: simplistic/CameraSwitchingPreview.kt for the new way
 */
@Composable
fun LegacyCameraSwitchingPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State for camera selection
    var useFront by rememberSaveable { mutableStateOf(false) }
    val selector = if (useFront)
        CameraSelector.DEFAULT_FRONT_CAMERA
    else
        CameraSelector.DEFAULT_BACK_CAMERA

    // THE OLD WAY: Create and remember a PreviewView
    val previewView = remember { PreviewView(context) }

    // Track camera provider for cleanup
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // DisposableEffect to handle rebinding when selector changes
    // This is the imperative coordination dance
    DisposableEffect(selector) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            // Must unbind before rebinding with new selector
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview)
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    Box(Modifier.fillMaxSize()) {
        // THE VIEW ISLAND
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        FloatingActionButton(
            onClick = { useFront = !useFront },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Rounded.Cameraswitch, contentDescription = "Switch camera")
        }
    }
}
