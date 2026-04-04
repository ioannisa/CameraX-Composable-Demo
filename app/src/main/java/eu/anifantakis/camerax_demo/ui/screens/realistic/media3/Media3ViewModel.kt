package eu.anifantakis.camerax_demo.ui.screens.realistic.media3

import android.app.Application
import androidx.annotation.OptIn
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
import androidx.compose.runtime.Stable
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Media3ViewModel — owns the full capture → edit → playback pipeline.
 *
 * State management:
 *  - [surfaceRequest]: emitted by CameraX Preview for CameraXViewfinder
 *  - [screenState]: 3-state machine (Camera → Processing → Playback)
 *  - [isRecording]: whether a video recording is currently active
 *  - [player]: ExoPlayer instance for the Playback state (null otherwise)
 *
 * The ViewModel owns ExoPlayer lifecycle — it's created after Transformer
 * finishes and released on [recordAgain] or [onCleared].
 */
@Stable
class Media3ViewModel(app: Application) : AndroidViewModel(app) {

    // ── UI-observed state ─────────────────────────────────────────
    val surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val screenState = MutableStateFlow(Media3ScreenState.Camera)
    val isRecording = MutableStateFlow(false)
    val player = MutableStateFlow<ExoPlayer?>(null)

    // ── Internal state ────────────────────────────────────────────
    private val appContext = app.applicationContext
    private val mainExecutor get() = ContextCompat.getMainExecutor(appContext)

    /** Cached reference for cleanup in [unbindCamera] and [onCleared]. */
    private var cameraProvider: ProcessCameraProvider? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var rawFile: File? = null
    private var processedFile: File? = null

    // ── Camera binding ────────────────────────────────────────────

    fun bindCamera(lifecycleOwner: LifecycleOwner) {
        viewModelScope.launch {
            val provider = ProcessCameraProvider.awaitInstance(getApplication())
            cameraProvider = provider

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider { req -> surfaceRequest.value = req }
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
    }

    // ── Recording ─────────────────────────────────────────────────

    fun toggleRecording() {
        val vc = videoCapture ?: return

        if (isRecording.value) {
            recording?.stop()
            return
        }

        val file = File(appContext.cacheDir, "raw_${System.currentTimeMillis()}.mp4")
        rawFile = file
        val fileOutput = FileOutputOptions.Builder(file).build()

        recording = vc.output
            .prepareRecording(appContext, fileOutput)
            // .withAudioEnabled() // Requires RECORD_AUDIO permission
            .start(mainExecutor) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    recording = null
                    isRecording.value = false

                    if (!event.hasError()) {
                        processRecording(file)
                    }
                }
            }
        isRecording.value = true
    }

    // ── Transformer processing ────────────────────────────────────

    @OptIn(UnstableApi::class)
    private fun processRecording(inputFile: File) {
        screenState.value = Media3ScreenState.Processing

        val output = File(appContext.cacheDir, "processed_${System.currentTimeMillis()}.mp4")
        processedFile = output

        val transformer = Transformer.Builder(appContext)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(
                    composition: Composition,
                    exportResult: ExportResult
                ) {
                    inputFile.delete()
                    preparePlayback(output)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    inputFile.delete()
                    output.delete()
                    screenState.value = Media3ScreenState.Camera
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

        transformer.start(editedMediaItem, output.absolutePath)
    }

    // ── Playback ──────────────────────────────────────────────────

    private fun preparePlayback(file: File) {
        val exo = ExoPlayer.Builder(appContext).build().apply {
            setMediaItem(MediaItem.fromUri(file.toURI().toString()))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
        }
        player.value = exo
        screenState.value = Media3ScreenState.Playback
    }

    fun recordAgain() {
        player.value?.release()
        player.value = null
        processedFile?.delete()
        processedFile = null
        screenState.value = Media3ScreenState.Camera
    }

    // ── Cleanup ──────────────────────────────────────────────────

    /** Explicitly unbind all camera use cases. Called when the screen leaves composition. */
    fun unbindCamera() {
        cameraProvider?.unbindAll()
    }

    override fun onCleared() {
        super.onCleared()
        cameraProvider?.unbindAll()
        player.value?.release()
        rawFile?.delete()
        processedFile?.delete()
    }
}

enum class Media3ScreenState { Camera, Processing, Playback }
