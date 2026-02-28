package eu.anifantakis.camerax_demo.ui.screens.mlkit

import android.graphics.PointF
import android.graphics.RectF
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.interfaces.Detector
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

/**
 * ML Kit detector factory and result converters.
 *
 * CameraX's `MlKitAnalyzer` (from camera-mlkit-vision) handles the
 * ImageProxy → InputImage conversion and lifecycle. We just need to:
 *  1. Create the right detector for each effect
 *  2. Convert raw ML Kit results into our overlay-friendly data classes
 */

// ── Detector Factory ─────────────────────────────────────────────────

/** Creates the ML Kit detector for the given effect. */
fun createDetector(effect: MlKitEffect): Detector<*> = when (effect) {
    MlKitEffect.FaceDetection -> FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()
    )
    MlKitEffect.PoseDetection -> PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
    )
    MlKitEffect.ObjectDetection -> ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .enableMultipleObjects()
            .build()
    )
    MlKitEffect.BarcodeScanning -> BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
    )
}

// ── Result Converters ────────────────────────────────────────────────

fun List<Face>.toFaceResults() = map { face ->
    FaceResult(
        boundingBox = RectF(face.boundingBox),
        landmarks = face.allLandmarks.map { PointF(it.position.x, it.position.y) }
    )
}

fun Pose.toPoseResult(): PoseResult? {
    if (allPoseLandmarks.isEmpty()) return null
    val points = allPoseLandmarks.map { PointF(it.position.x, it.position.y) }
    val connections = POSE_CONNECTIONS.mapNotNull { (fromType, toType) ->
        val fromIdx = allPoseLandmarks.indexOfFirst { it.landmarkType == fromType }
        val toIdx = allPoseLandmarks.indexOfFirst { it.landmarkType == toType }
        if (fromIdx >= 0 && toIdx >= 0) fromIdx to toIdx else null
    }
    return PoseResult(landmarks = points, connections = connections)
}

fun List<DetectedObject>.toObjectResults() = map { obj ->
    ObjectResult(
        boundingBox = RectF(obj.boundingBox),
        labels = obj.labels.map { it.text },
        trackingId = obj.trackingId
    )
}

fun List<Barcode>.toBarcodeResults() = mapNotNull { barcode ->
    val box = barcode.boundingBox ?: return@mapNotNull null
    BarcodeResult(
        boundingBox = RectF(box),
        displayValue = barcode.displayValue ?: barcode.rawValue ?: "Unknown"
    )
}

// ── Pose Skeleton Connections ────────────────────────────────────────

/** Standard skeleton connections for visualization. */
val POSE_CONNECTIONS = listOf(
    // Face
    PoseLandmark.LEFT_EAR to PoseLandmark.LEFT_EYE,
    PoseLandmark.LEFT_EYE to PoseLandmark.NOSE,
    PoseLandmark.NOSE to PoseLandmark.RIGHT_EYE,
    PoseLandmark.RIGHT_EYE to PoseLandmark.RIGHT_EAR,
    // Upper body
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
    PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
    PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
    PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
    // Torso
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
    PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
    PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
    // Lower body
    PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
    PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
    PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
    PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE
)
