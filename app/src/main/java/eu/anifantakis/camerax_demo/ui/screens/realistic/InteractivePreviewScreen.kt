package eu.anifantakis.camerax_demo.ui.screens.realistic

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.SurfaceRequest
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.koin.androidx.compose.koinViewModel

/**
 * Interactive preview demonstrating input→camera mapping:
 *
 *  - Tap-to-focus:
 *      Compose coordinates MUST be transformed to surface coordinates to account
 *      for scaling, rotation and aspect crop. CameraX provides MutableCoordinateTransformer
 *      to handle that mapping safely.
 *
 *  - Pinch-to-zoom:
 *      We apply a multiplicative delta and clamp it within CameraX's reported
 *      min/max zoom range (via cameraInfo.zoomState).
 */
@Composable
fun InteractivePreviewScreen(
    vm: CameraViewModel = koinViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) { vm.bindPreview(lifecycleOwner) }
    val request by vm.surfaceRequest.collectAsState(initial = null)

    // Used by CameraXViewfinder to map UI space → camera surface space.
    val transformer = remember { MutableCoordinateTransformer() }

    Box(Modifier.fillMaxSize()) {
        request?.let { req: SurfaceRequest ->
            CameraXViewfinder(
                surfaceRequest = req,
                coordinateTransformer = transformer,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // Tap-to-focus: forward the transformed tap to CameraX.
                        detectTapGestures { uiOffset ->
                            vm.onTapToFocus(uiOffset, transformer, req)
                        }
                    }
                    .pointerInput(Unit) {
                        // Pinch-to-zoom: multiplicative zoom delta handled in VM.
                        detectTransformGestures { _, _, zoomChange, _ ->
                            vm.onZoomChange(zoomChange)
                        }
                    }
            )
        }
    }
}
