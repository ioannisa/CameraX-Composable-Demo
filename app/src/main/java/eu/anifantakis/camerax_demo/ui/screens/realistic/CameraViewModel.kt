package eu.anifantakis.camerax_demo.ui.screens.realistic

import android.Manifest
import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresPermission
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
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
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * CameraViewModel:
 *
 * CameraX "use cases" (Preview, ImageCapture, VideoCapture) are configured here and
 * exposed to the UI as simple state flows. The composables remain lightweight and react
 * to state changes (e.g., new SurfaceRequest).
 *
 * Key CameraX types used in this file:
 *  - ProcessCameraProvider: entry point to bind/unbind use cases to a lifecycle.
 *  - Preview: requests a drawing surface (SurfaceRequest) for the live camera feed.
 *  - ImageCapture: takes still photos (JPEG).
 *  - VideoCapture + Recorder: records encoded video to an output (e.g., MediaStore).
 *  - Camera: gives cameraInfo (zoomState) and cameraControl (zoom/focus commands).
 */
class CameraViewModel(app: Application) : AndroidViewModel(app) {

    // ---- UI-observed state ---------------------------------------------------------------------

    /** Latest SurfaceRequest; feed into CameraXViewfinder to render preview frames. */
    val surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)

    /** Bound camera instance; exposes cameraInfo/cameraControl for interactions. */
    val camera = MutableStateFlow<Camera?>(null)

    /** Still capture use case; set when bindCapture() is active. */
    val imageCapture = MutableStateFlow<ImageCapture?>(null)

    /** Video capture use case (Recorder-backed); set when bindCapture() is active. */
    val videoCapture = MutableStateFlow<VideoCapture<Recorder>?>(null)

    /** Currently active recording handle (null when idle). */
    val recording = MutableStateFlow<Recording?>(null)

    // ---- Android infrastructure (executors, app context) --------------------------------------

    private val appContext = app.applicationContext
    private val mainExecutor: Executor get() = ContextCompat.getMainExecutor(appContext)

    // FYI: Old-school way to await a ListenableFuture provider. Left for reference.
    private suspend fun provider_old(): ProcessCameraProvider =
        suspendCancellableCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(appContext)
            future.addListener(
                { try { cont.resume(future.get()) } catch (t: Throwable) { cont.cancel(t) } },
                mainExecutor
            )
            cont.invokeOnCancellation { future.cancel(true) }
        }

    /** Preferred suspending API (CameraX 1.4+): clean, cancellation-aware provider. */
    private suspend fun provider(): ProcessCameraProvider =
        ProcessCameraProvider.awaitInstance(getApplication())

    // ---- Binding use cases (where the pipeline is defined) -------------------------------------

    /**
     * Preview-only binding.
     *
     * Flow:
     *  1) Build Preview and route its SurfaceRequest to [surfaceRequest].
     *  2) Unbind previous use cases (only one set can be active).
     *  3) Bind to [lifecycleOwner] with the given [selector] (front/back).
     */
    fun bindPreview(
        lifecycleOwner: LifecycleOwner,
        selector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    ) {
        viewModelScope.launch {
            val provider = provider()

            val preview = Preview.Builder().build().apply {
                // CameraX will call this whenever it needs a surface to draw into.
                setSurfaceProvider { req -> surfaceRequest.value = req }
            }

            provider.unbindAll()

            // Resulting Camera gives us cameraInfo/control for zoom/focus later.
            camera.value = provider.bindToLifecycle(lifecycleOwner, selector, preview)

            // Ensure capture use cases are cleared in preview-only mode.
            imageCapture.value = null
            videoCapture.value = null
        }
    }

    /**
     * Full binding (Preview + ImageCapture + VideoCapture).
     *
     * Notes on quality:
     *  - ImageCapture CAPTURE_MODE_MINIMIZE_LATENCY → faster shutter; consider MAX_QUALITY if needed.
     *  - Recorder quality via QualitySelector (e.g., FHD/HD/UHD).
     */
    fun bindCapture(
        lifecycleOwner: LifecycleOwner,
        selector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
        quality: Quality = Quality.FHD
    ) {
        viewModelScope.launch {
            val provider = provider()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider { req -> surfaceRequest.value = req }
            }

            val img = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(quality))
                .build()
            val vid = VideoCapture.withOutput(recorder)

            provider.unbindAll()

            // Bind all use cases together so they share the same internal camera session.
            camera.value = provider.bindToLifecycle(lifecycleOwner, selector, preview, img, vid)
            imageCapture.value = img
            videoCapture.value = vid
        }
    }

    // ---- User interactions (focus/zoom) --------------------------------------------------------

    /**
     * Tap-to-focus around a point in the preview.
     *
     * Why the transformer?
     *  - UI coordinates are in Compose pixels; the camera operates in "surface space".
     *  - MutableCoordinateTransformer performs the correct mapping (rotation, crop/scale).
     */
    fun onTapToFocus(
        uiOffset: Offset,
        transformer: MutableCoordinateTransformer,
        request: SurfaceRequest
    ) {
        val cam = camera.value ?: return

        // Convert UI tap → surface pixel coordinates.
        val pt = with(transformer) { uiOffset.transform() }

        // Build a metering point for AF/AE using the actual surface resolution.
        val factory = SurfaceOrientedMeteringPointFactory(
            request.resolution.width.toFloat(),
            request.resolution.height.toFloat()
        )

        val action = FocusMeteringAction.Builder(
            factory.createPoint(pt.x, pt.y),
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        )
            .setAutoCancelDuration(3, TimeUnit.SECONDS) // Return to continuous AF/AE after a short window.
            .build()

        cam.cameraControl.startFocusAndMetering(action)
    }

    /**
     * Pinch zoom handler.
     *  - We read current zoom state (ratio + min/max) from cameraInfo.
     *  - Apply multiplicative delta and clamp; CameraX handles the rest.
     */
    fun onZoomChange(zoomChange: Float) {
        val cam = camera.value ?: return
        val z = cam.cameraInfo.zoomState.value ?: return
        val newRatio = (z.zoomRatio * zoomChange).coerceIn(z.minZoomRatio, z.maxZoomRatio)
        cam.cameraControl.setZoomRatio(newRatio)
    }

    // ---- Capture (photo/video) to MediaStore ---------------------------------------------------

    /**
     * Capture a JPEG to MediaStore (public gallery).
     * On Android 10+ we also set RELATIVE_PATH → DCIM/CameraX.
     */
    fun capturePhoto() {
        val capture = imageCapture.value ?: return

        val name = "IMG_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/CameraX")
            }
        }

        val out = ImageCapture.OutputFileOptions.Builder(
            appContext.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ).build()

        capture.takePicture(out, mainExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                // Hook for UI feedback/logging.
            }
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                // Hook: output.savedUri available for sharing/snackbar.
            }
        })
    }

    /**
     * Start/stop video recording to MediaStore.
     *
     * Assumptions:
     *  - RECORD_AUDIO has been granted (gated in UI by PermissionGate).
     *  - If you want "silent" recording when mic is denied, make .withAudioEnabled() conditional.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun toggleRecording() {
        val vc = videoCapture.value ?: return

        // Stop current recording if running.
        recording.value?.let {
            it.stop()
            recording.value = null
            return
        }

        // Prepare a new recording session.
        val name = "VID_${System.currentTimeMillis()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/CameraX")
            }
        }

        val out = MediaStoreOutputOptions.Builder(
            appContext.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(values).build()

        recording.value = vc.output
            .prepareRecording(appContext, out)
            .withAudioEnabled() // change to conditional if you want silent-video fallback
            .start(mainExecutor) { event ->
                // We only need to clear the handle once finalized; inspect event for errors if needed.
                if (event is VideoRecordEvent.Finalize) {
                    recording.value = null
                }
            }
    }
}
