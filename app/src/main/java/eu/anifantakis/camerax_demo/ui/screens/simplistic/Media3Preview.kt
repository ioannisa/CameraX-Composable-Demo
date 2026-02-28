package eu.anifantakis.camerax_demo.ui.screens.simplistic

import androidx.annotation.OptIn
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/**
 * CameraX + Media3 Pipeline — Simplistic Example
 *
 * Demonstrates the full capture → edit → playback pipeline:
 *  1. CAMERA: CameraXViewfinder + VideoCapture recording to a cache file
 *  2. PROCESSING: Transformer resizes the video to 720p
 *  3. PLAYBACK: ExoPlayer + PlayerSurface plays the processed result
 *
 * Self-contained — no ViewModel, all state lives in Compose.
 *
 * Video-only (no audio) to avoid RECORD_AUDIO permission complexity.
 * To add audio: call .withAudioEnabled() on prepareRecording().
 */
@OptIn(UnstableApi::class)
@Composable
fun Media3Preview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = ContextCompat.getMainExecutor(context)

    // 3-state pipeline
    var screenState by remember { mutableStateOf(ScreenState.Camera) }

    // Camera state
    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }

    // Playback state
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var outputFile by remember { mutableStateOf<File?>(null) }

    // Bind camera use cases
    LaunchedEffect(Unit) {
        val provider = ProcessCameraProvider.awaitInstance(context)

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { surfaceRequests.value = it }
        }

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
    }

    // Clean up ExoPlayer when leaving
    DisposableEffect(Unit) {
        onDispose {
            player?.release()
        }
    }

    when (screenState) {
        // ── CAMERA STATE ──────────────────────────────────────────
        ScreenState.Camera -> {
            Box(Modifier.fillMaxSize()) {
                surfaceRequest?.let { req ->
                    CameraXViewfinder(
                        surfaceRequest = req,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Button(
                    onClick = {
                        val vc = videoCapture ?: return@Button

                        if (isRecording) {
                            // Stop recording → triggers Finalize event
                            recording?.stop()
                        } else {
                            // Start recording to cache file
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
                                            // Move to processing with the recorded file
                                            outputFile = file
                                            screenState = ScreenState.Processing
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
        ScreenState.Processing -> {
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
                            // Clean up raw file
                            inputFile.delete()
                            outputFile = processedFile

                            // Prepare ExoPlayer for playback
                            val exo = ExoPlayer.Builder(context).build().apply {
                                setMediaItem(
                                    MediaItem.fromUri(processedFile.toURI().toString())
                                )
                                prepare()
                                playWhenReady = true
                                repeatMode = Player.REPEAT_MODE_ONE
                            }
                            player = exo

                            screenState = ScreenState.Playback
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            // On error, go back to camera
                            inputFile.delete()
                            screenState = ScreenState.Camera
                        }
                    })
                    .build()

                val mediaItem = MediaItem.fromUri(inputFile.toURI().toString())
                // Presentation.createForHeight(720) resizes to 720p.
                // Other effects are possible: grayscale, speed change, rotation, etc.
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
        ScreenState.Playback -> {
            Box(Modifier.fillMaxSize()) {
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
                        // Release player and go back to camera
                        player?.release()
                        player = null
                        outputFile?.delete()
                        outputFile = null
                        screenState = ScreenState.Camera
                    }) {
                        Text("Record Again")
                    }
                }
            }
        }
    }
}

private enum class ScreenState { Camera, Processing, Playback }
