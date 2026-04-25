package eu.anifantakis.camerax_demo.ui.screens.simplistic.usecase

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Use Case 4 of 4: ImageAnalysis.
 *
 * The ImageAnalysis use case streams every frame to a custom Analyzer running
 * on a background executor. This is the entry point for ML, computer vision,
 * QR scanning, etc. Here we keep it minimal: compute average luminance from
 * the Y plane and surface it to the UI.
 *
 * The pipeline:
 *   Preview        --(SurfaceRequest)--> CameraXViewfinder
 *   ImageAnalysis  --(ImageProxy YUV)--> LumaAnalyzer --> UI text
 *
 * STRATEGY_KEEP_ONLY_LATEST drops backed-up frames so the analyzer never
 * lags behind the camera stream.
 */
@Composable
fun UseCaseImageAnalysis() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    var luma by remember { mutableFloatStateOf(0f) }

    DisposableEffect(lifecycleOwner) {
        var cameraProvider: ProcessCameraProvider? = null

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { req -> surfaceRequests.value = req }
        }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                setAnalyzer(analysisExecutor) { proxy ->
                    luma = proxy.averageLuma()
                    proxy.close()
                }
            }

        val job = scope.launch {
            cameraProvider = ProcessCameraProvider.awaitInstance(context)
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
        }

        onDispose {
            job.cancel()
            analysis.clearAnalyzer()
            cameraProvider?.unbindAll()
            preview.surfaceProvider = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        surfaceRequest?.let { req ->
            CameraXViewfinder(surfaceRequest = req, modifier = Modifier.fillMaxSize())
        }

        Text(
            text = "Avg Luma: ${"%.1f".format(luma)} / 255",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

/**
 * Reads the Y plane of a YUV_420_888 frame and returns the mean byte value
 * as an unsigned float in [0, 255]. Bright scenes trend toward 255, dark
 * toward 0.
 */
private fun ImageProxy.averageLuma(): Float {
    val buffer = planes[0].buffer
    buffer.rewind()
    val data = ByteArray(buffer.remaining())
    buffer.get(data)
    if (data.isEmpty()) return 0f
    var sum = 0L
    for (b in data) sum += (b.toInt() and 0xFF)
    return sum.toFloat() / data.size
}
