package eu.anifantakis.camerax_demo.ui.screens.realistic.mlkit

import android.app.Application
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import eu.anifantakis.camerax_demo.ui.screens.mlkit.BarcodeResult
import eu.anifantakis.camerax_demo.ui.screens.mlkit.FaceResult
import eu.anifantakis.camerax_demo.ui.screens.mlkit.MlKitEffect
import eu.anifantakis.camerax_demo.ui.screens.mlkit.ObjectResult
import eu.anifantakis.camerax_demo.ui.screens.mlkit.PoseResult
import eu.anifantakis.camerax_demo.ui.screens.mlkit.toBarcodeResults
import eu.anifantakis.camerax_demo.ui.screens.mlkit.toFaceResults
import eu.anifantakis.camerax_demo.ui.screens.mlkit.toObjectResults
import eu.anifantakis.camerax_demo.ui.screens.mlkit.toPoseResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * MlKitViewModel:
 *
 * Owns the CameraX pipeline (Preview + ImageAnalysis) and the ML Kit detector
 * lifecycle. Exposes detection results as state flows for the UI to render.
 *
 * Uses CameraX's `MlKitAnalyzer` (from camera-mlkit-vision) to bridge
 * ImageAnalysis frames to ML Kit detectors automatically.
 */
@Stable
class MlKitViewModel(app: Application) : AndroidViewModel(app) {

    // ── UI-observed state ────────────────────────────────────────────

    /** Latest SurfaceRequest; feed into CameraXViewfinder. */
    val surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)

    /** Currently selected ML Kit effect. */
    val selectedEffect = MutableStateFlow(MlKitEffect.FaceDetection)

    /** Detection results — one flow per effect type. */
    val faceResults = MutableStateFlow(emptyList<FaceResult>())
    val poseResult = MutableStateFlow<PoseResult?>(null)
    val objectResults = MutableStateFlow(emptyList<ObjectResult>())
    val barcodeResults = MutableStateFlow(emptyList<BarcodeResult>())

    /** Image dimensions from the analysis pipeline (for coordinate transform). */
    val analysisImageWidth = MutableStateFlow(0)
    val analysisImageHeight = MutableStateFlow(0)
    val analysisRotation = MutableStateFlow(0)

    // ── Internal ─────────────────────────────────────────────────────

    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private suspend fun provider(): ProcessCameraProvider =
        ProcessCameraProvider.awaitInstance(getApplication())

    // ── Effect selection ─────────────────────────────────────────────

    fun selectEffect(effect: MlKitEffect) {
        selectedEffect.value = effect
    }

    // ── Camera binding ───────────────────────────────────────────────

    /**
     * Binds Preview + ImageAnalysis with the ML Kit detector for [effect].
     *
     * Called from the UI's LaunchedEffect whenever the selected effect changes.
     * MlKitAnalyzer handles InputImage creation and ImageProxy lifecycle.
     */
    fun bindWithEffect(
        lifecycleOwner: LifecycleOwner,
        effect: MlKitEffect,
        selector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    ) {
        viewModelScope.launch {
            val provider = provider()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider { req -> surfaceRequest.value = req }
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val analyzer = createAnalyzer(effect)
            imageAnalysis.setAnalyzer(analysisExecutor, analyzer)

            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)

            // Read actual analysis resolution after binding for coordinate transform
            imageAnalysis.resolutionInfo?.let { info ->
                analysisImageWidth.value = info.resolution.width
                analysisImageHeight.value = info.resolution.height
                analysisRotation.value = info.rotationDegrees
            }
        }
    }

    // ── Analyzer factory ─────────────────────────────────────────────

    /**
     * Creates a typed MlKitAnalyzer for the given effect.
     *
     * Each branch keeps the detector typed so result.getValue() returns
     * the correct ML Kit type without unchecked casts.
     */
    private fun createAnalyzer(effect: MlKitEffect): MlKitAnalyzer = when (effect) {
        MlKitEffect.FaceDetection -> {
            val det = FaceDetection.getClient(
                FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .build()
            )
            MlKitAnalyzer(
                listOf(det),
                ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                analysisExecutor
            ) { result ->
                val faces: List<Face> = result.getValue(det) ?: return@MlKitAnalyzer
                faceResults.value = faces.toFaceResults()
            }
        }
        MlKitEffect.PoseDetection -> {
            val det = PoseDetection.getClient(
                PoseDetectorOptions.Builder()
                    .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                    .build()
            )
            MlKitAnalyzer(
                listOf(det),
                ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                analysisExecutor
            ) { result ->
                val pose: Pose = result.getValue(det) ?: return@MlKitAnalyzer
                poseResult.value = pose.toPoseResult()
            }
        }
        MlKitEffect.ObjectDetection -> {
            val det = ObjectDetection.getClient(
                ObjectDetectorOptions.Builder()
                    .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                    .enableClassification()
                    .enableMultipleObjects()
                    .build()
            )
            MlKitAnalyzer(
                listOf(det),
                ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                analysisExecutor
            ) { result ->
                val objects: List<DetectedObject> = result.getValue(det) ?: return@MlKitAnalyzer
                objectResults.value = objects.toObjectResults()
            }
        }
        MlKitEffect.BarcodeScanning -> {
            val det = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build()
            )
            MlKitAnalyzer(
                listOf(det),
                ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                analysisExecutor
            ) { result ->
                val barcodes: List<Barcode> = result.getValue(det) ?: return@MlKitAnalyzer
                barcodeResults.value = barcodes.toBarcodeResults()
            }
        }
    }
}
