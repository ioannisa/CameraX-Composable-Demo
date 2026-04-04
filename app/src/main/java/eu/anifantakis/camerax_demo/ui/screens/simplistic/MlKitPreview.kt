package eu.anifantakis.camerax_demo.ui.screens.simplistic

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import eu.anifantakis.camerax_demo.ui.screens.mlkit.*
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * NEW WAY: ML Kit Vision Effects with CameraXViewfinder
 *
 * Uses CameraX's `MlKitAnalyzer` (from camera-mlkit-vision) to connect
 * ML Kit detectors to the ImageAnalysis pipeline. MlKitAnalyzer handles
 * InputImage creation and ImageProxy lifecycle automatically.
 *
 * KEY CONCEPT: ImageAnalysis is a CameraX pipeline-level use case.
 * It works identically with both CameraXViewfinder and PreviewView
 * because it operates on the camera stream, not the rendering surface.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MlKitPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    var selectedEffect by rememberSaveable { mutableStateOf(MlKitEffect.FaceDetection) }
    var useFrontCamera by rememberSaveable { mutableStateOf(false) }
    val cameraSelector =
        if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

    val scope = rememberCoroutineScope()

    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    // ML Kit result state
    var faceResults by remember { mutableStateOf(emptyList<FaceResult>()) }
    var poseResult by remember { mutableStateOf<PoseResult?>(null) }
    var objectResults by remember { mutableStateOf(emptyList<ObjectResult>()) }
    var barcodeResults by remember { mutableStateOf(emptyList<BarcodeResult>()) }

    // Image dimensions for coordinate transform (from resolutionInfo after binding)
    var analysisImageWidth by remember { mutableIntStateOf(0) }
    var analysisImageHeight by remember { mutableIntStateOf(0) }
    var analysisRotation by remember { mutableIntStateOf(0) }

    // Rebind when effect or camera changes
    DisposableEffect(selectedEffect, cameraSelector) {
        var cameraProvider: ProcessCameraProvider? = null

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { req -> surfaceRequests.value = req }
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        // Create typed detector + MlKitAnalyzer per effect.
        // Each branch keeps the detector typed so result.getValue() returns
        // the correct type without casting.
        val analyzer = when (selectedEffect) {
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
                    faceResults = faces.toFaceResults()
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
                    poseResult = pose.toPoseResult()
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
                    objectResults = objects.toObjectResults()
                }
            }
            MlKitEffect.BarcodeScanning -> {
                val det = com.google.mlkit.vision.barcode.BarcodeScanning.getClient(
                    com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build()
                )
                MlKitAnalyzer(
                    listOf(det),
                    ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                    analysisExecutor
                ) { result ->
                    val barcodes: List<Barcode> = result.getValue(det) ?: return@MlKitAnalyzer
                    barcodeResults = barcodes.toBarcodeResults()
                }
            }
        }

        imageAnalysis.setAnalyzer(analysisExecutor, analyzer)

        val job = scope.launch {
            cameraProvider = ProcessCameraProvider.awaitInstance(context)
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )

            // Read actual analysis resolution after binding for coordinate transform
            imageAnalysis.resolutionInfo?.let { info ->
                analysisImageWidth = info.resolution.width
                analysisImageHeight = info.resolution.height
                analysisRotation = info.rotationDegrees
            }
        }

        onDispose {
            job.cancel()
            imageAnalysis.clearAnalyzer()
            cameraProvider?.unbindAll()
            preview.surfaceProvider = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Effect selector chips
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MlKitEffect.entries.forEach { effect ->
                FilterChip(
                    selected = selectedEffect == effect,
                    onClick = { selectedEffect = effect },
                    label = { Text(effect.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Camera preview + ML Kit overlay
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            surfaceRequest?.let { req ->
                CameraXViewfinder(
                    surfaceRequest = req,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Draw overlay on top of the viewfinder
            if (analysisImageWidth > 0) {
                when (selectedEffect) {
                    MlKitEffect.FaceDetection -> FaceOverlay(
                        faces = faceResults.toImmutableList(),
                        imageWidth = analysisImageWidth,
                        imageHeight = analysisImageHeight,
                        rotationDegrees = analysisRotation,
                        isFrontCamera = useFrontCamera,
                        modifier = Modifier.fillMaxSize()
                    )
                    MlKitEffect.PoseDetection -> PoseOverlay(
                        poseResult = poseResult,
                        imageWidth = analysisImageWidth,
                        imageHeight = analysisImageHeight,
                        rotationDegrees = analysisRotation,
                        isFrontCamera = useFrontCamera,
                        modifier = Modifier.fillMaxSize()
                    )
                    MlKitEffect.ObjectDetection -> ObjectOverlay(
                        objects = objectResults,
                        imageWidth = analysisImageWidth,
                        imageHeight = analysisImageHeight,
                        rotationDegrees = analysisRotation,
                        isFrontCamera = useFrontCamera,
                        modifier = Modifier.fillMaxSize()
                    )
                    MlKitEffect.BarcodeScanning -> BarcodeOverlay(
                        barcodes = barcodeResults,
                        imageWidth = analysisImageWidth,
                        imageHeight = analysisImageHeight,
                        rotationDegrees = analysisRotation,
                        isFrontCamera = useFrontCamera,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Camera switch FAB
            FloatingActionButton(
                onClick = { useFrontCamera = !useFrontCamera },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Rounded.Cameraswitch, contentDescription = "Switch camera")
            }
        }
    }
}
