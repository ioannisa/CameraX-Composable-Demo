package eu.anifantakis.camerax_demo.ui.screens.mlkit

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.collections.immutable.ImmutableList

/**
 * Compose Canvas overlay that draws ML Kit detection results.
 *
 * Each draw function uses [CoordinateTransform] to map from image-space
 * coordinates to the Canvas's display-space coordinates.
 */

// ── Colors ───────────────────────────────────────────────────────────

private val FaceBoxColor = Color.Green
private val FaceLandmarkColor = Color.Yellow
private val PoseJointColor = Color.Cyan
private val PoseLineColor = Color.Green
private val ObjectBoxColor = Color.Magenta
private val BarcodeBoxColor = Color(0xFFFF8800) // orange

// ── Face Detection Overlay ───────────────────────────────────────────

@Composable
fun FaceOverlay(
    faces: ImmutableList<FaceResult>,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val transform = CoordinateTransform(
            imageWidth, imageHeight, rotationDegrees, size, isFrontCamera
        )
        faces.forEach { face ->
            // Bounding box
            val rect = transform.mapRect(face.boundingBox)
            drawRect(
                color = FaceBoxColor,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                style = Stroke(width = 3f)
            )
            // Landmarks
            face.landmarks.forEach { lm ->
                val pt = transform.mapPoint(lm)
                drawCircle(FaceLandmarkColor, radius = 5f, center = pt)
            }
        }
    }
}

// ── Pose Detection Overlay ───────────────────────────────────────────

@Composable
fun PoseOverlay(
    poseResult: PoseResult?,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val pose = poseResult ?: return@Canvas
        val transform = CoordinateTransform(
            imageWidth, imageHeight, rotationDegrees, size, isFrontCamera
        )

        // Skeleton lines
        pose.connections.forEach { (fromIdx, toIdx) ->
            val from = transform.mapPoint(pose.landmarks[fromIdx])
            val to = transform.mapPoint(pose.landmarks[toIdx])
            drawLine(PoseLineColor, from, to, strokeWidth = 4f)
        }

        // Joint dots
        pose.landmarks.forEach { lm ->
            val pt = transform.mapPoint(lm)
            drawCircle(PoseJointColor, radius = 7f, center = pt)
        }
    }
}

// ── Object Detection Overlay ─────────────────────────────────────────

@Composable
fun ObjectOverlay(
    objects: List<ObjectResult>,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val transform = CoordinateTransform(
            imageWidth, imageHeight, rotationDegrees, size, isFrontCamera
        )
        objects.forEach { obj ->
            val rect = transform.mapRect(obj.boundingBox)
            drawRect(
                color = ObjectBoxColor,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                style = Stroke(width = 3f)
            )

            // Label text
            val label = buildString {
                if (obj.trackingId != null) append("#${obj.trackingId} ")
                append(obj.labels.joinToString())
            }
            if (label.isNotBlank()) {
                drawLabelText(label, rect.left, rect.top - 8f, ObjectBoxColor)
            }
        }
    }
}

// ── Barcode Scanning Overlay ─────────────────────────────────────────

@Composable
fun BarcodeOverlay(
    barcodes: List<BarcodeResult>,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val transform = CoordinateTransform(
            imageWidth, imageHeight, rotationDegrees, size, isFrontCamera
        )
        barcodes.forEach { barcode ->
            val rect = transform.mapRect(barcode.boundingBox)
            drawRect(
                color = BarcodeBoxColor,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                style = Stroke(width = 3f)
            )
            drawLabelText(barcode.displayValue, rect.left, rect.bottom + 20f, BarcodeBoxColor)
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────

private fun DrawScope.drawLabelText(text: String, x: Float, y: Float, color: Color) {
    val paint = android.graphics.Paint().apply {
        this.color = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
        textSize = 36f
        isAntiAlias = true
        setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
    }
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}
