package eu.anifantakis.camerax_demo.ui.screens.mlkit

import android.graphics.PointF
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/**
 * Maps ML Kit image-space coordinates to Canvas overlay-space.
 *
 * ML Kit returns results in the coordinate system of the analyzed image
 * (e.g., 640x480). The Canvas overlay matches the composable's display
 * size. This transform handles:
 *
 *  1. **Rotation** — ImageProxy.rotationDegrees (typically 90 on most phones)
 *     swaps image width/height so the "rotated image" matches display orientation.
 *  2. **Scaling** — Aspect-fill (CameraX's default FillCenter behavior):
 *     `scale = max(scaleX, scaleY)` so the image covers the entire view.
 *  3. **Centering** — Offset to center the scaled image in the view.
 *  4. **Front-camera mirroring** — Horizontal flip of x-coordinate.
 */
class CoordinateTransform(
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int,
    private val viewSize: Size,
    private val isFrontCamera: Boolean
) {
    private val scale: Float
    private val offsetX: Float
    private val offsetY: Float
    private val rotatedWidth: Float
    private val rotatedHeight: Float

    init {
        // After rotation, the effective image dimensions may swap
        val needsSwap = rotationDegrees == 90 || rotationDegrees == 270
        rotatedWidth = if (needsSwap) imageHeight.toFloat() else imageWidth.toFloat()
        rotatedHeight = if (needsSwap) imageWidth.toFloat() else imageHeight.toFloat()

        // Aspect-fill: scale so the image covers the entire view
        val scaleX = viewSize.width / rotatedWidth
        val scaleY = viewSize.height / rotatedHeight
        scale = maxOf(scaleX, scaleY)

        // Center the scaled image
        offsetX = (viewSize.width - rotatedWidth * scale) / 2f
        offsetY = (viewSize.height - rotatedHeight * scale) / 2f
    }

    fun mapPoint(point: PointF): Offset {
        var x = point.x * scale + offsetX
        val y = point.y * scale + offsetY
        if (isFrontCamera) {
            x = viewSize.width - x
        }
        return Offset(x, y)
    }

    fun mapRect(rect: RectF): Rect {
        val topLeft = mapPoint(PointF(rect.left, rect.top))
        val bottomRight = mapPoint(PointF(rect.right, rect.bottom))
        return if (isFrontCamera) {
            // When mirrored, left/right swap
            Rect(bottomRight.x, topLeft.y, topLeft.x, bottomRight.y)
        } else {
            Rect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
        }
    }
}
