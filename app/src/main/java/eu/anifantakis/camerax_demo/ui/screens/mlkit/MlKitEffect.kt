package eu.anifantakis.camerax_demo.ui.screens.mlkit

import android.graphics.PointF
import android.graphics.RectF
import androidx.compose.runtime.Immutable

/**
 * Available ML Kit vision effects.
 *
 * Each effect maps to a different ML Kit detector that processes
 * camera frames via ImageAnalysis in real time. All detectors implement
 * the ML Kit `Detector<T>` interface, so they work with CameraX's
 * `MlKitAnalyzer` convenience wrapper from `camera-mlkit-vision`.
 */
enum class MlKitEffect(val label: String) {
    FaceDetection("Face Detection"),
    PoseDetection("Pose Detection"),
    ObjectDetection("Object Detection"),
    BarcodeScanning("Barcode & QR Scanning")
}

// ── Result data classes ──────────────────────────────────────────────

@Immutable
data class FaceResult(
    val boundingBox: RectF,
    val landmarks: List<PointF>
)

@Immutable
data class PoseResult(
    val landmarks: List<PointF>,
    val connections: List<Pair<Int, Int>>
)

@Immutable
data class ObjectResult(
    val boundingBox: RectF,
    val labels: List<String>,
    val trackingId: Int?
)

@Immutable
data class BarcodeResult(
    val boundingBox: RectF,
    val displayValue: String
)
