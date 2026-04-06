package eu.anifantakis.camerax_demo.ui.screens.legacy.camerax_1_6_features

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionSessionConfig
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.anifantakis.camerax_demo.ui.screens.CameraLensInfo
import eu.anifantakis.camerax_demo.ui.screens.buildCameraSelectorForId
import eu.anifantakis.camerax_demo.ui.screens.enumerateCameraLenses

/**
 * LEGACY: CameraX Extensions with AndroidView + PreviewView
 *
 * CameraX 1.6 introduces ExtensionSessionConfig — extensions are now configured
 * through SessionConfig instead of swapping CameraSelectors. This screen uses
 * the new approach with the legacy DisposableEffect + callback pattern.
 *
 * KEY POINT: Extensions primarily affect CAPTURED images, not the live preview.
 * The preview may look identical across modes — the real difference is in the
 * saved photo. Extensions use multi-frame computational photography that runs
 * at capture time.
 *
 * LENS SELECTION: Extension availability varies per physical camera lens.
 * A mode that's unavailable on the wide lens may be available on the ultrawide.
 * This screen lets you switch lenses to explore availability differences.
 *
 * Extensions work identically with both PreviewView and CameraXViewfinder — same
 * ExtensionsManager API, same ExtensionSessionConfig binding, same result.
 *
 * Compare with: simplistic/ExtensionsPreview.kt for the modern Compose approach.
 */

enum class LegacyExtensionModeOption(val label: String, val mode: Int) {
    None("None", ExtensionMode.NONE),
    Auto("Auto", ExtensionMode.AUTO),
    Night("Night", ExtensionMode.NIGHT),
    Hdr("HDR", ExtensionMode.HDR),
    Bokeh("Bokeh", ExtensionMode.BOKEH),
    FaceRetouch("Face Retouch", ExtensionMode.FACE_RETOUCH),
}

@ExperimentalCamera2Interop
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LegacyExtensionsPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = ContextCompat.getMainExecutor(context)

    var selectedMode by rememberSaveable { mutableStateOf(LegacyExtensionModeOption.None) }
    var availability by remember { mutableStateOf(mapOf<Int, Boolean>()) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // Lens selection state
    var lenses by remember { mutableStateOf<List<CameraLensInfo>>(emptyList()) }
    var selectedLens by remember { mutableStateOf<CameraLensInfo?>(null) }

    val previewView = remember { PreviewView(context) }

    // Enumerate lenses on first composition
    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val discovered = enumerateCameraLenses(cameraProvider)
            lenses = discovered
            if (selectedLens == null && discovered.isNotEmpty()) {
                selectedLens = discovered.first()
            }
        }, mainExecutor)

        onDispose { }
    }

    // Re-check extension availability when lens changes
    DisposableEffect(selectedLens) {
        val lens = selectedLens
        if (lens != null) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val extensionsManagerFuture = ExtensionsManager.getInstanceAsync(context, cameraProvider)
                extensionsManagerFuture.addListener({
                    val extensionsManager = extensionsManagerFuture.get()
                    val baseSelector = buildCameraSelectorForId(lens.cameraId)

                    availability = LegacyExtensionModeOption.entries.associate { option ->
                        option.mode to if (option.mode == ExtensionMode.NONE) {
                            true
                        } else {
                            extensionsManager.isExtensionAvailable(baseSelector, option.mode)
                        }
                    }

                    // Reset to None — the previous extension may not be available on this lens
                    selectedMode = LegacyExtensionModeOption.None
                }, mainExecutor)
            }, mainExecutor)
        }

        onDispose { }
    }

    // Rebind camera whenever the selected lens or mode changes.
    // CameraX 1.6: Use ExtensionSessionConfig instead of swapping CameraSelectors.
    DisposableEffect(lifecycleOwner, selectedLens, selectedMode) {
        val preview = Preview.Builder().build()
        preview.surfaceProvider = previewView.surfaceProvider

        val lens = selectedLens
        if (lens != null) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val extensionsManagerFuture = ExtensionsManager.getInstanceAsync(context, cameraProvider)
                extensionsManagerFuture.addListener({
                    val extensionsManager = extensionsManagerFuture.get()
                    val baseSelector = buildCameraSelectorForId(lens.cameraId)

                    val imgCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    cameraProvider.unbindAll()

                    if (selectedMode.mode != ExtensionMode.NONE &&
                        extensionsManager.isExtensionAvailable(baseSelector, selectedMode.mode)
                    ) {
                        // 1.6 approach: bundle extension mode into SessionConfig
                        val sessionConfig = ExtensionSessionConfig(
                            mode = selectedMode.mode,
                            extensionsManager = extensionsManager,
                            useCases = listOf(preview, imgCapture)
                        )
                        cameraProvider.bindToLifecycle(lifecycleOwner, baseSelector, sessionConfig)
                    } else {
                        cameraProvider.bindToLifecycle(lifecycleOwner, baseSelector, preview, imgCapture)
                    }

                    imageCapture = imgCapture
                }, mainExecutor)
            }, mainExecutor)
        }

        onDispose {
            ProcessCameraProvider.getInstance(context).get().unbindAll()
            preview.surfaceProvider = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Lens selector
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            lenses.forEach { lens ->
                FilterChip(
                    selected = selectedLens == lens,
                    onClick = { selectedLens = lens },
                    label = { Text(lens.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Extension mode selector
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LegacyExtensionModeOption.entries.forEach { option ->
                val isAvailable = availability[option.mode] ?: (option.mode == ExtensionMode.NONE)
                FilterChip(
                    selected = selectedMode == option,
                    onClick = { if (isAvailable) selectedMode = option },
                    enabled = isAvailable,
                    label = { Text(option.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Status text
        val isAvailable = availability[selectedMode.mode] ?: (selectedMode.mode == ExtensionMode.NONE)
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth()
                .background(
                    if (isAvailable) Color.Green.copy(alpha = 0.2f)
                    else Color(0xFFFF9800).copy(alpha = 0.3f),
                    RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = when {
                    availability.isEmpty() -> "Checking extension availability..."
                    selectedMode == LegacyExtensionModeOption.None ->
                        "No extension active. Select a mode to enable hardware processing.\n" +
                        "Try switching lenses — availability varies per camera."
                    isAvailable ->
                        "${selectedMode.label} mode is ACTIVE. The preview may look similar — " +
                        "take a photo to see the real difference! Extensions use multi-frame " +
                        "processing that primarily affects captured images."
                    else -> "${selectedMode.label} is NOT available on this device."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }

        // Camera preview
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Capture button
            Button(
                onClick = {
                    val capture = imageCapture ?: return@Button

                    val suffix = if (selectedMode == LegacyExtensionModeOption.None) "NONE"
                                 else selectedMode.label.uppercase().replace(" ", "_")
                    val name = "EXT_${suffix}_${System.currentTimeMillis()}.jpg"
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, name)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/CameraX")
                        }
                    }

                    val outputOptions = ImageCapture.OutputFileOptions.Builder(
                        context.contentResolver,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                    ).build()

                    capture.takePicture(
                        outputOptions,
                        mainExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onError(e: ImageCaptureException) {
                                Toast.makeText(context, "Capture failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                Toast.makeText(
                                    context,
                                    "Saved: $name — compare with other modes in Gallery!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    if (selectedMode == LegacyExtensionModeOption.None) "Take Photo (No Extension)"
                    else "Take Photo (${selectedMode.label})"
                )
            }
        }
    }
}
