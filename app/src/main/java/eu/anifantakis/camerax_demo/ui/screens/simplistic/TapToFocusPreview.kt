package eu.anifantakis.camerax_demo.ui.screens.simplistic

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.TimeUnit

/**
 * Tap-to-Focus & Pinch-to-Zoom - Simplistic Example
 *
 * TAP-TO-FOCUS - THE PROBLEM:
 *  - UI Space (Compose pixels) ≠ Camera Space (Sensor coordinates)
 *  - You must account for: Device Rotation, Sensor orientation (90° rotated),
 *    Content scaling (Fit, Crop, FillBounds), Front camera mirror
 *
 * TAP-TO-FOCUS - THE SOLUTION:
 *  - MutableCoordinateTransformer handles ALL the math for you!
 *  - Pass it to CameraXViewfinder, then use: with(transformer) { offset.transform() }
 *
 * PINCH-TO-ZOOM:
 *  - Standard Compose gestures — nothing camera-specific
 *  - Read current zoom from cameraInfo.zoomState
 *  - Multiply by gesture delta, clamp to min/max range
 *  - CameraX animates to new zoom with setZoomRatio()
 *
 * COMBINE THEM:
 *  - Chain multiple .pointerInput() modifiers on same viewfinder
 *  - They work together!
 *
 * Works in portrait, landscape, tablets, foldables, front camera mirrored.
 */
@Composable
fun TapToFocusPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    // Store camera reference for focus control
    var camera by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { req -> surfaceRequests.value = req }
        }
        cameraProvider.unbindAll()
        camera = cameraProvider.bindToLifecycle(
            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview
        )
    }

    // THE MAGIC: MutableCoordinateTransformer
    val transformer = remember { MutableCoordinateTransformer() }

    Box(Modifier.fillMaxSize()) {
        surfaceRequest?.let { req ->
            CameraXViewfinder(
                surfaceRequest = req,
                coordinateTransformer = transformer,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            // Transform: Compose → Camera
                            val pt = with(transformer) { offset.transform() }

                            // Create metering point factory with surface resolution
                            val factory = SurfaceOrientedMeteringPointFactory(
                                req.resolution.width.toFloat(),
                                req.resolution.height.toFloat()
                            )

                            val action = FocusMeteringAction.Builder(
                                factory.createPoint(pt.x, pt.y),
                                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                            )
                                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                .build()

                            camera?.cameraControl?.startFocusAndMetering(action)
                        }
                    }
                    // Pinch-to-Zoom: Chain multiple .pointerInput() - they work together!
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            val currentZoom = camera?.cameraInfo?.zoomState?.value ?: return@detectTransformGestures
                            val newRatio = (currentZoom.zoomRatio * zoom).coerceIn(
                                currentZoom.minZoomRatio,
                                currentZoom.maxZoomRatio
                            )
                            camera?.cameraControl?.setZoomRatio(newRatio)
                        }
                    }
            )
        }
    }
}
