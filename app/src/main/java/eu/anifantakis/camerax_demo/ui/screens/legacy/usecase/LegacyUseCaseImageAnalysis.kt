package eu.anifantakis.camerax_demo.ui.screens.legacy.usecase

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Use Case 4 of 4 (Legacy): ImageAnalysis.
 *
 * Preview rendered into [PreviewView]; ImageAnalysis runs a custom Analyzer
 * on a background executor that computes average luminance from the Y plane.
 *
 * Compare with: simplistic/usecase/UseCaseImageAnalysis.kt
 */
@Composable
fun LegacyUseCaseImageAnalysis() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    val previewView = remember { PreviewView(context) }
    var luma by remember { mutableFloatStateOf(0f) }

    DisposableEffect(lifecycleOwner) {
        var cameraProvider: ProcessCameraProvider? = null

        val preview = Preview.Builder().build().apply {
            surfaceProvider = previewView.surfaceProvider
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
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

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
