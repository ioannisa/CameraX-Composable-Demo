package eu.anifantakis.camerax_demo.ui.screens.legacy

import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import java.io.File

/**
 * LEGACY: CameraX + Media3 Pipeline using AndroidView + PreviewView
 *
 * THE OLD WAY for camera preview:
 *  - PreviewView wrapped in AndroidView (the "view island")
 *  - DisposableEffect + ProcessCameraProvider.getInstance() callback binding
 *
 * THE NEW WAY for Media3 playback:
 *  - Media3 is UI-agnostic, so we still use Compose-native PlayerSurface
 *  - Transformer processing is identical regardless of how the camera preview is rendered
 *
 * Same 3-state pipeline: Camera → Processing → Playback
 *
 * Compare with: simplistic/Media3Preview.kt for the fully Compose-native approach
 */
@OptIn(UnstableApi::class)
@Composable
fun LegacyMedia3Preview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = ContextCompat.getMainExecutor(context)

    // 3-state pipeline
    var screenState by remember { mutableStateOf(LegacyScreenState.Camera) }

    // Camera state
    val previewView = remember { PreviewView(context) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // Playback state
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var outputFile by remember { mutableStateOf<File?>(null) }

    // Legacy camera binding via DisposableEffect + callback
    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            val vidCapture = VideoCapture.withOutput(recorder)

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                vidCapture
            )

            videoCapture = vidCapture
        }, mainExecutor)

        onDispose {
            recording?.stop()
            cameraProvider?.unbindAll()
            player?.release()
        }
    }

    when (screenState) {
        // ── CAMERA STATE ──────────────────────────────────────────
        LegacyScreenState.Camera -> {
            Box(Modifier.fillMaxSize()) {
                // THE VIEW ISLAND — legacy PreviewView
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                Button(
                    onClick = {
                        val vc = videoCapture ?: return@Button

                        if (isRecording) {
                            recording?.stop()
                        } else {
                            val file = File(
                                context.cacheDir,
                                "raw_${System.currentTimeMillis()}.mp4"
                            )
                            val fileOutput = FileOutputOptions.Builder(file).build()

                            recording = vc.output
                                .prepareRecording(context, fileOutput)
                                // .withAudioEnabled() // Requires RECORD_AUDIO permission
                                .start(mainExecutor) { event ->
                                    if (event is VideoRecordEvent.Finalize) {
                                        recording = null
                                        isRecording = false

                                        if (!event.hasError()) {
                                            outputFile = file
                                            screenState = LegacyScreenState.Processing
                                        }
                                    }
                                }
                            isRecording = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp)
                ) {
                    Text(if (isRecording) "Stop" else "Record")
                }
            }
        }

        // ── PROCESSING STATE ──────────────────────────────────────
        LegacyScreenState.Processing -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(64.dp))
                Text(
                    "Processing video...",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // Run Transformer to resize video to 720p
            LaunchedEffect(screenState) {
                val inputFile = outputFile ?: return@LaunchedEffect

                val processedFile = File(
                    context.cacheDir,
                    "processed_${System.currentTimeMillis()}.mp4"
                )

                val transformer = Transformer.Builder(context)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(
                            composition: Composition,
                            exportResult: ExportResult
                        ) {
                            inputFile.delete()
                            outputFile = processedFile

                            val exo = ExoPlayer.Builder(context).build().apply {
                                setMediaItem(
                                    MediaItem.fromUri(processedFile.toURI().toString())
                                )
                                prepare()
                                playWhenReady = true
                                repeatMode = Player.REPEAT_MODE_ONE
                            }
                            player = exo

                            screenState = LegacyScreenState.Playback
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            inputFile.delete()
                            screenState = LegacyScreenState.Camera
                        }
                    })
                    .build()

                val mediaItem = MediaItem.fromUri(inputFile.toURI().toString())
                val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                    .setEffects(
                        androidx.media3.transformer.Effects(
                            /* audioProcessors = */ listOf(),
                            /* videoEffects = */ listOf(
                                androidx.media3.effect.Presentation.createForHeight(720)
                            )
                        )
                    )
                    .build()

                transformer.start(editedMediaItem, processedFile.absolutePath)
            }
        }

        // ── PLAYBACK STATE ────────────────────────────────────────
        LegacyScreenState.Playback -> {
            Box(Modifier.fillMaxSize()) {
                // Media3 playback is Compose-native even in the "legacy" tier
                player?.let { exo ->
                    PlayerSurface(
                        player = exo,
                        surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(onClick = {
                        player?.let { if (it.isPlaying) it.pause() else it.play() }
                    }) {
                        Text(if (player?.isPlaying == true) "Pause" else "Play")
                    }

                    Button(onClick = {
                        player?.release()
                        player = null
                        outputFile?.delete()
                        outputFile = null
                        screenState = LegacyScreenState.Camera
                    }) {
                        Text("Record Again")
                    }
                }
            }
        }
    }
}

private enum class LegacyScreenState { Camera, Processing, Playback }
