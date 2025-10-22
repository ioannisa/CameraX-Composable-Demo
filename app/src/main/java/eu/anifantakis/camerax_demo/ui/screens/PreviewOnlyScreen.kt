package eu.anifantakis.camerax_demo.ui.screens

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.anifantakis.camerax_demo.ui.CameraViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Preview-only demo screen.
 *
 * Highlights:
 *  - Binds a Preview use case and feeds its SurfaceRequest to CameraXViewfinder.
 *  - Lets you flip between front/back cameras (rebinds the pipeline).
 *  - (Optional) Toggles the CameraXViewfinder "implementation mode":
 *      EXTERNAL  → SurfaceView-backed (best perf/latency; fewer composition tricks)
 *      EMBEDDED  → TextureView-backed (behaves like normal UI; more composition features)
 *
 * We default to EXTERNAL; toggle the chip to see EMBEDDED. This is purely educational—
 * leaving the default (not specifying a mode) is fine for most apps.
 */
@Composable
fun PreviewOnlyScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val vm: CameraViewModel = koinViewModel()

    // Persist selected lens across config changes.
    var useFront by rememberSaveable { mutableStateOf(false) }
    val selector =
        if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

    // (Re)bind the Preview use case whenever the lens changes.
    LaunchedEffect(selector) { vm.bindPreview(lifecycleOwner, selector) }

    // Implementation mode toggle (EXTERNAL vs EMBEDDED). Optional; for learning/comparison.
    var useEmbedded by rememberSaveable { mutableStateOf(false) }
    val mode =
        if (useEmbedded) ImplementationMode.EMBEDDED
        else ImplementationMode.EXTERNAL

    // Latest SurfaceRequest published by CameraX; null until the pipeline is ready.
    val request by vm.surfaceRequest.collectAsStateWithLifecycle(initialValue = null)

    Box(Modifier.fillMaxSize()) {
        // Render the live camera preview.
        request?.let { req ->
            CameraXViewfinder(
                surfaceRequest = req,
                implementationMode = mode, // comment out to rely on library default
                modifier = Modifier.fillMaxSize()
            )
        }

        // Quick UI controls:

        // Switch between front/back camera. Rebinding keeps state simple and robust.
        FloatingActionButton(
            onClick = { useFront = !useFront },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Cameraswitch,
                contentDescription = if (useFront) "Switch to back camera" else "Switch to front camera"
            )
        }

        // Toggle the underlying viewfinder implementation for educational comparison.
        AssistChip(
            onClick = { useEmbedded = !useEmbedded },
            label = {
                Text(
                    if (useEmbedded) "Mode: EMBEDDED" else "Mode: EXTERNAL"
                )
            },
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        )
    }
}
