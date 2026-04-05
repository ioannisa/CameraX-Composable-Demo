package eu.anifantakis.camerax_demo.ui.screens

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider

/**
 * Shared utilities for enumerating physical camera lenses.
 *
 * Modern phones have multiple physical cameras behind "back camera" —
 * ultrawide, wide, telephoto, etc. CameraX's DEFAULT_BACK_CAMERA selects
 * the logical (fused) camera.
 *
 * ## Two approaches to multi-lens access
 *
 * **1. Independent camera binding (enumerateCameraLenses)**
 * Uses Camera2CameraInfo to read each camera's focal length and facing,
 * then builds a CameraSelector that targets a specific camera ID.
 * Only works for cameras that CameraX can bind to independently —
 * which on virtually all modern Android flagships (Pixel, Samsung Galaxy,
 * OnePlus, Xiaomi, etc.) means just one logical back camera and one
 * logical front camera. The ultrawide and telephoto sensors are physical
 * sub-cameras within the logical camera and are NOT independently bindable.
 *
 * **2. Zoom-ratio-based switching (enumerateZoomLenses)**
 * Queries the logical camera's physical sub-cameras via Camera2's
 * CameraManager, reads each sub-camera's focal length, and calculates
 * the zoom ratio needed to activate it. The app then binds to the
 * logical camera and calls setZoomRatio() — the HAL seamlessly switches
 * between physical sensors. This approach works on all modern flagships
 * and is how the stock camera apps (Google Camera, Samsung Camera) work.
 */

data class CameraLensInfo(
    val cameraId: String,
    val label: String,
    val focalLength: Float,
    val lensFacing: Int
)

/**
 * Represents a physical sub-camera within a logical camera,
 * accessible via zoom ratio rather than direct binding.
 */
data class ZoomLensInfo(
    val physicalCameraId: String,
    val label: String,
    val focalLength: Float,
    val zoomRatio: Float
)

/**
 * Discovers all independently bindable cameras and classifies them by focal length.
 *
 * Filters out non-standard cameras (IR sensors, depth cameras) by requiring
 * BACKWARD_COMPATIBLE capability — this prevents phantom entries like a
 * second "front camera" that's actually an IR sensor for face unlock.
 *
 * Note: On virtually all modern Android flagships, this returns only one back
 * camera (the logical camera) and one front camera. The ultrawide and telephoto
 * are physical sub-cameras that aren't independently bindable. For multi-lens
 * access on those devices, use [enumerateZoomLenses] instead.
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

/**
 * Discovers physical sub-cameras within the logical camera and calculates
 * the zoom ratio needed to activate each one.
 *
 * Requires API 28+ (Android P) for [CameraCharacteristics.getPhysicalCameraIds].
 * On older devices, returns a single entry at 1x zoom.
 *
 * How it works:
 *  1. Finds the logical camera for the given [lensFacing]
 *  2. Queries Camera2's CameraManager for its physical sub-camera IDs
 *  3. Reads each sub-camera's focal length AND sensor physical size
 *  4. Calculates the correct zoom ratio using both values:
 *
 *     zoomRatio = (physicalFL × referenceSensorWidth) /
 *                 (referenceFL × physicalSensorWidth)
 *
 *     Using focal length alone is WRONG because the physical sub-cameras
 *     have different sensor sizes. The field of view depends on both
 *     focal length and sensor dimension: FOV ≈ sensorWidth / focalLength.
 *     The zoom ratio is the inverse ratio of the FOVs.
 *
 * The caller should bind to the logical camera (DEFAULT_BACK_CAMERA) and
 * use [androidx.camera.core.CameraControl.setZoomRatio] to switch lenses.
 */
@ExperimentalCamera2Interop
fun enumerateZoomLenses(
    context: Context,
    cameraProvider: ProcessCameraProvider,
    lensFacing: Int = CameraCharacteristics.LENS_FACING_BACK
): List<ZoomLensInfo> {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    // Find the logical camera for this facing direction
    val logicalCameraInfo = cameraProvider.availableCameraInfos.firstOrNull { cameraInfo ->
        val camera2Info = Camera2CameraInfo.from(cameraInfo)
        val facing = camera2Info.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
        facing == lensFacing
    } ?: return emptyList()

    val logicalCameraId = Camera2CameraInfo.from(logicalCameraInfo).cameraId
    val logicalChars = cameraManager.getCameraCharacteristics(logicalCameraId)

    // Reference properties — the logical camera's defaults (what CameraX treats as 1x zoom)
    val referenceFocalLength = logicalChars
        .get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        ?.firstOrNull() ?: return emptyList()
    val referenceSensorSize = logicalChars
        .get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        ?: return emptyList()

    // API 28+ required for physical camera IDs
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        return listOf(
            ZoomLensInfo(
                physicalCameraId = logicalCameraId,
                label = "1x ${classifyLens(referenceFocalLength)} (${String.format("%.1f", referenceFocalLength)}mm)",
                focalLength = referenceFocalLength,
                zoomRatio = 1.0f
            )
        )
    }

    val physicalIds = logicalChars.physicalCameraIds
    if (physicalIds.isEmpty()) {
        // Not a multi-camera — just the logical camera itself
        return listOf(
            ZoomLensInfo(
                physicalCameraId = logicalCameraId,
                label = "1x ${classifyLens(referenceFocalLength)} (${String.format("%.1f", referenceFocalLength)}mm)",
                focalLength = referenceFocalLength,
                zoomRatio = 1.0f
            )
        )
    }

    // Collect properties from each backward-compatible physical sub-camera
    return physicalIds.mapNotNull { physicalId ->
        val chars = cameraManager.getCameraCharacteristics(physicalId)
        val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        val isCompatible = capabilities?.any {
            it == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
        } == true
        if (!isCompatible) return@mapNotNull null

        val focalLength = chars
            .get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            ?.firstOrNull() ?: return@mapNotNull null
        val sensorSize = chars
            .get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            ?: return@mapNotNull null

        // Correct zoom ratio: accounts for BOTH focal length AND sensor size.
        // FOV ≈ sensorWidth / focalLength, zoom = FOV_reference / FOV_physical
        val zoomRatio = (focalLength * referenceSensorSize.width) /
                        (referenceFocalLength * sensorSize.width)

        val displayZoom = if (zoomRatio < 1f) "%.1fx".format(zoomRatio)
                          else "%.0fx".format(zoomRatio)

        ZoomLensInfo(
            physicalCameraId = physicalId,
            label = "$displayZoom ${classifyLens(focalLength)} (${String.format("%.1f", focalLength)}mm)",
            focalLength = focalLength,
            zoomRatio = zoomRatio
        )
    }.sortedBy { it.zoomRatio }
}

private fun classifyLens(focalLength: Float): String = when {
    focalLength < 3f -> "Ultrawide"
    focalLength < 7f -> "Wide"
    focalLength < 12f -> "Telephoto"
    else -> "Super Telephoto"
}
