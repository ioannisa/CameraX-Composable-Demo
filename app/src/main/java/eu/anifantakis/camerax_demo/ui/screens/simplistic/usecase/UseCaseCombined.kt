package eu.anifantakis.camerax_demo.ui.screens.simplistic.usecase

import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.anifantakis.camerax_demo.ui.components.Permission
import eu.anifantakis.camerax_demo.ui.components.PermissionGate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Combined: Preview + ImageCapture + VideoCapture + ImageAnalysis.
 *
 * All four CameraX use cases bound to a single camera session in one call:
 *
 *   bindToLifecycle(owner, selector, preview, imageCapture, videoCapture, imageAnalysis)
 *
 * The shared session means every use case sees frames from the same physical
 * sensor and stays mutually consistent. Note that combining all four requires
 * a camera that supports the resulting stream configuration — most modern
 * devices do, but on constrained hardware you may need to drop one.
 */
@SuppressLint("MissingPermission") // gated by PermissionGate + Permission.RECORD_AUDIO.isGranted()
@Composable
fun UseCaseCombined() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = ContextCompat.getMainExecutor(context)
    val scope = rememberCoroutineScope()
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var luma by remember { mutableFloatStateOf(0f) }

    DisposableEffect(lifecycleOwner) {
        var cameraProvider: ProcessCameraProvider? = null

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { req -> surfaceRequests.value = req }
        }

        val imgCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.FHD))
            .build()
        val vidCapture = VideoCapture.withOutput(recorder)

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
                imgCapture,
                vidCapture,
                analysis
            )
            imageCapture = imgCapture
            videoCapture = vidCapture
        }

        onDispose {
            job.cancel()
            recording?.stop()
            recording = null
            analysis.clearAnalyzer()
            cameraProvider?.unbindAll()
            preview.surfaceProvider = null
            imageCapture = null
            videoCapture = null
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

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = {
                val capture = imageCapture ?: return@Button

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/CameraX")
                    }
                }

                val output = ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                ).build()

                capture.takePicture(
                    output,
                    mainExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(result: ImageCapture.OutputFileResults) { /* saved */ }
                        override fun onError(e: ImageCaptureException) { /* handle */ }
                    }
                )
            }) {
                Text("Take Photo")
            }

            PermissionGate(permission = Permission.RECORD_AUDIO) {
                Button(onClick = {
                    if (!Permission.RECORD_AUDIO.isGranted(context)) return@Button
                    val vc = videoCapture ?: return@Button

                    recording?.let {
                        it.stop()
                        recording = null
                        return@Button
                    }

                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, "VID_${System.currentTimeMillis()}.mp4")
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/CameraX")
                        }
                    }

                    val output = MediaStoreOutputOptions.Builder(
                        context.contentResolver,
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    ).setContentValues(values).build()

                    recording = vc.output
                        .prepareRecording(context, output)
                        .withAudioEnabled()
                        .start(mainExecutor) { event ->
                            if (event is VideoRecordEvent.Finalize) recording = null
                        }
                }) {
                    Text(if (recording == null) "Record" else "Stop")
                }
            }
        }
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
