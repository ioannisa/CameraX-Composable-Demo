package eu.anifantakis.camerax_demo.ui.screens

import android.hardware.camera2.CameraCharacteristics
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider

/**
 * Shared utilities for enumerating physical camera lenses.
 *
 * Modern phones have multiple physical cameras behind "back camera" —
 * ultrawide, wide, telephoto, etc. CameraX's DEFAULT_BACK_CAMERA selects
 * the logical (fused) camera. To access individual lenses, we use
 * Camera2CameraInfo to read each camera's focal length and facing,
 * then build a CameraSelector that targets a specific camera ID.
 *
 * Important: availableCameraInfos only returns cameras that CameraX can
 * bind to independently. On many Samsung devices (e.g. Galaxy S24 Ultra),
 * telephoto lenses are physical sub-cameras within a logical camera and
 * are NOT independently bindable — they're accessed via zoom ratio on
 * the logical camera instead.
 */

data class CameraLensInfo(
    val cameraId: String,
    val label: String,
    val focalLength: Float,
    val lensFacing: Int
)

/**
 * Discovers all bindable cameras and classifies them by focal length.
 *
 * Filters out non-standard cameras (IR sensors, depth cameras) by requiring
 * BACKWARD_COMPATIBLE capability — this prevents phantom entries like a
 * second "front camera" that's actually an IR sensor for face unlock.
 *
 * Classification uses physical focal length (not 35mm equivalent):
 *  - Ultrawide: < 3mm
 *  - Wide: 3mm – 7mm
 *  - Telephoto: 7mm – 12mm
 *  - Super Telephoto: >= 12mm
 */
@ExperimentalCamera2Interop
fun enumerateCameraLenses(cameraProvider: ProcessCameraProvider): List<CameraLensInfo> {
    return cameraProvider.availableCameraInfos.mapNotNull { cameraInfo ->
        val camera2Info = Camera2CameraInfo.from(cameraInfo)
        val cameraId = camera2Info.cameraId

        // Filter out IR sensors, depth cameras, and other non-standard cameras.
        // Only BACKWARD_COMPATIBLE cameras support preview + capture.
        val capabilities = camera2Info.getCameraCharacteristic(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
        )
        val isBackwardCompatible = capabilities?.any {
            it == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
        } == true
        if (!isBackwardCompatible) return@mapNotNull null

        val facing = camera2Info.getCameraCharacteristic(
            CameraCharacteristics.LENS_FACING
        ) ?: return@mapNotNull null

        val focalLengths = camera2Info.getCameraCharacteristic(
            CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
        ) ?: return@mapNotNull null

        if (focalLengths.isEmpty()) return@mapNotNull null

        val focalLength = focalLengths[0]
        val facingLabel = if (facing == CameraCharacteristics.LENS_FACING_FRONT) "Front" else "Back"
        val typeLabel = classifyLens(focalLength)

        CameraLensInfo(
            cameraId = cameraId,
            label = "$facingLabel $typeLabel (${String.format("%.1f", focalLength)}mm)",
            focalLength = focalLength,
            lensFacing = facing
        )
    }.sortedWith(
        compareBy<CameraLensInfo> {
            // Back cameras first
            if (it.lensFacing == CameraCharacteristics.LENS_FACING_BACK) 0 else 1
        }.thenBy { it.focalLength }
    )
}

/**
 * Builds a CameraSelector that targets a specific camera by ID.
 *
 * Uses addCameraFilter to match only the camera whose Camera2CameraInfo.cameraId
 * equals the requested ID. This bypasses the logical camera grouping that
 * DEFAULT_BACK_CAMERA uses.
 */
@ExperimentalCamera2Interop
fun buildCameraSelectorForId(cameraId: String): CameraSelector {
    return CameraSelector.Builder()
        .addCameraFilter { cameras ->
            cameras.filter { cameraInfo ->
                Camera2CameraInfo.from(cameraInfo).cameraId == cameraId
            }
        }
        .build()
}

private fun classifyLens(focalLength: Float): String = when {
    focalLength < 3f -> "Ultrawide"
    focalLength < 7f -> "Wide"
    focalLength < 12f -> "Telephoto"
    else -> "Super Telephoto"
}
