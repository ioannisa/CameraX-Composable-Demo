package eu.anifantakis.camerax_demo.ui.screens.legacy

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.TimeUnit

/**
 * LEGACY: Tap-to-Focus & Pinch-to-Zoom using AndroidView + PreviewView
 *
 * THE OLD WAY (Imperative):
 *  - Use PreviewView's built-in MeteringPointFactory
 *  - Set up touch listeners on the View
 *  - Use ScaleGestureDetector for pinch-to-zoom
 *
 * PROBLEMS WITH THIS APPROACH:
 *  1. Touch handling is on the View, not in Compose
 *  2. Must use View's touch listener system (onTouchListener)
 *  3. ScaleGestureDetector is Android View API, not Compose gestures
 *  4. Coordinate transforms handled by PreviewView's factory
 *     (works, but ties you to PreviewView)
 *  5. Mixing Compose UI with View touch handling is awkward
 *
 * NOTE: PreviewView.MeteringPointFactory handles transforms internally,
 * but you're locked into PreviewView's coordinate system.
 * With the new CameraXViewfinder + MutableCoordinateTransformer,
 * you get the same correct transforms but in pure Compose.
 *
 * Compare with: simplistic/TapToFocusPreview.kt for the new declarative way
 */
@Composable
fun LegacyTapToFocusPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            provider.unbindAll()
            val cam = provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview
            )
            camera = cam

            // THE OLD WAY: Set up touch listener on the View
            // This is View-based touch handling, not Compose gestures
            setupTouchListeners(previewView, cam)

        }, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    // THE VIEW ISLAND: All touch handling happens on the View
    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Set up tap-to-focus and pinch-to-zoom on PreviewView
 *
 * This is the OLD imperative approach:
 * - Uses View's setOnTouchListener
 * - Uses ScaleGestureDetector (Android View API)
 * - Uses PreviewView's MeteringPointFactory
 */
@SuppressLint("ClickableViewAccessibility")
private fun setupTouchListeners(previewView: PreviewView, camera: Camera?) {
    // PINCH-TO-ZOOM: Using ScaleGestureDetector (Android View API)
    val scaleGestureDetector = ScaleGestureDetector(
        previewView.context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val cam = camera ?: return false
                val zoomState = cam.cameraInfo.zoomState.value ?: return false

                val newRatio = (zoomState.zoomRatio * detector.scaleFactor).coerceIn(
                    zoomState.minZoomRatio,
                    zoomState.maxZoomRatio
                )
                cam.cameraControl.setZoomRatio(newRatio)
                return true
            }
        }
    )

    // THE OLD TOUCH HANDLING: View's onTouchListener
    previewView.setOnTouchListener { _: View, event: MotionEvent ->
        // Let scale detector handle pinch gestures
        scaleGestureDetector.onTouchEvent(event)

        // Handle single taps for focus
        if (event.action == MotionEvent.ACTION_UP && !scaleGestureDetector.isInProgress) {
            val cam = camera ?: return@setOnTouchListener true

            // PreviewView's MeteringPointFactory handles coordinate transforms
            // This works, but ties you to PreviewView
            val factory = previewView.meteringPointFactory
            val point = factory.createPoint(event.x, event.y)

            val action = FocusMeteringAction.Builder(
                point,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
            )
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build()

            cam.cameraControl.startFocusAndMetering(action)
        }

        true
    }
}
