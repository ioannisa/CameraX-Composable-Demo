package eu.anifantakis.camerax_demo.ui.screens.simplistic.usecase

import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.anifantakis.camerax_demo.ui.components.Permission
import eu.anifantakis.camerax_demo.ui.components.PermissionGate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Use Case 3 of 4: VideoCapture.
 *
 * The VideoCapture use case records video (and audio) to a file on demand.
 * It is built around a `Recorder`, which exposes quality selection and
 * controls a `Recording` session.
 *
 * The pipeline:
 *   Preview       --(SurfaceRequest)--> CameraXViewfinder
 *   VideoCapture  --(prepareRecording.start)--> MediaStore (DCIM/CameraX)
 *
 * Audio is gated on RECORD_AUDIO permission via [PermissionGate].
 */
@SuppressLint("MissingPermission") // gated by PermissionGate + Permission.RECORD_AUDIO.isGranted()
@Composable
fun UseCaseVideoCapture() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = ContextCompat.getMainExecutor(context)
    val scope = rememberCoroutineScope()

    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }

    DisposableEffect(lifecycleOwner) {
        var cameraProvider: ProcessCameraProvider? = null

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { req -> surfaceRequests.value = req }
        }

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.FHD))
            .build()
        val capture = VideoCapture.withOutput(recorder)

        val job = scope.launch {
            cameraProvider = ProcessCameraProvider.awaitInstance(context)
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                capture
            )
            videoCapture = capture
        }

        onDispose {
            job.cancel()
            recording?.stop()
            recording = null
            cameraProvider?.unbindAll()
            preview.surfaceProvider = null
            videoCapture = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        surfaceRequest?.let { req ->
            CameraXViewfinder(surfaceRequest = req, modifier = Modifier.fillMaxSize())
        }

        PermissionGate(permission = Permission.RECORD_AUDIO) {
            Button(
                onClick = {
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
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
            ) {
                Text(if (recording == null) "Record" else "Stop")
            }
        }
    }
}
