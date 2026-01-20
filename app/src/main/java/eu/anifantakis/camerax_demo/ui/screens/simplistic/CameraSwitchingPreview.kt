package eu.anifantakis.camerax_demo.ui.screens.simplistic

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Camera Switching - Simplistic Example
 *
 * Key Insights:
 *  1. LaunchedEffect(selector) - When selector changes (front↔back),
 *     the effect re-runs and rebinds the camera.
 *
 *  2. DON'T FORGET: unbindAll() - Always unbind previous use cases
 *     before rebinding with new selector.
 *
 *  3. rememberSaveable - Survives rotation and config changes.
 *     Without it, phone rotation resets camera choice.
 */
@Composable
fun CameraSwitchingPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    // Persists across config changes
    var useFront by rememberSaveable { mutableStateOf(false) }

    val selector = if (useFront)
        CameraSelector.DEFAULT_FRONT_CAMERA
    else
        CameraSelector.DEFAULT_BACK_CAMERA

    // We need to store preview in remember so it persists across recompositions
    val preview = remember {
        Preview.Builder().build().apply {
            setSurfaceProvider { req -> surfaceRequests.value = req }
        }
    }

    LaunchedEffect(selector) {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner, selector, preview
        )
    }

    Box(Modifier.fillMaxSize()) {
        surfaceRequest?.let {
            CameraXViewfinder(surfaceRequest = it, modifier = Modifier.fillMaxSize())
        }

        // FAB to toggle
        FloatingActionButton(
            onClick = { useFront = !useFront },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Cameraswitch,
                contentDescription = "Switch camera"
            )
        }
    }
}
