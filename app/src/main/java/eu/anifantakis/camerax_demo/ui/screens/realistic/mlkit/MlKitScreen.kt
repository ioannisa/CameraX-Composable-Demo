package eu.anifantakis.camerax_demo.ui.screens.realistic.mlkit

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.anifantakis.camerax_demo.ui.screens.mlkit.BarcodeOverlay
import eu.anifantakis.camerax_demo.ui.screens.mlkit.FaceOverlay
import eu.anifantakis.camerax_demo.ui.screens.mlkit.MlKitEffect
import eu.anifantakis.camerax_demo.ui.screens.mlkit.ObjectOverlay
import eu.anifantakis.camerax_demo.ui.screens.mlkit.PoseOverlay
import org.koin.androidx.compose.koinViewModel

/**
 * Realistic ML Kit screen — thin UI that delegates to [MlKitViewModel].
 *
 * Follows the same ViewModel-driven pattern as PreviewOnlyScreen / CaptureScreen:
 * the composable only collects state and calls ViewModel methods.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MlKitScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val vm: MlKitViewModel = koinViewModel()

    var useFrontCamera by rememberSaveable { mutableStateOf(false) }
    val cameraSelector =
        if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

    val surfaceRequest by vm.surfaceRequest.collectAsStateWithLifecycle()
    val selectedEffect by vm.selectedEffect.collectAsStateWithLifecycle()
    val faceResults by vm.faceResults.collectAsStateWithLifecycle()
    val poseResult by vm.poseResult.collectAsStateWithLifecycle()
    val objectResults by vm.objectResults.collectAsStateWithLifecycle()
    val barcodeResults by vm.barcodeResults.collectAsStateWithLifecycle()
    val imageWidth by vm.analysisImageWidth.collectAsStateWithLifecycle()
    val imageHeight by vm.analysisImageHeight.collectAsStateWithLifecycle()
    val rotation by vm.analysisRotation.collectAsStateWithLifecycle()

    // (Re)bind whenever the selected effect or camera changes.
    LaunchedEffect(selectedEffect, cameraSelector) {
        vm.bindWithEffect(lifecycleOwner, selectedEffect, cameraSelector)
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
                    onClick = { vm.selectEffect(effect) },
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
            if (imageWidth > 0) {
                when (selectedEffect) {
                    MlKitEffect.FaceDetection -> FaceOverlay(
                        faces = faceResults,
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        rotationDegrees = rotation,
                        isFrontCamera = useFrontCamera,
                        modifier = Modifier.fillMaxSize()
                    )
                    MlKitEffect.PoseDetection -> PoseOverlay(
                        poseResult = poseResult,
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        rotationDegrees = rotation,
                        isFrontCamera = useFrontCamera,
                        modifier = Modifier.fillMaxSize()
                    )
                    MlKitEffect.ObjectDetection -> ObjectOverlay(
                        objects = objectResults,
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        rotationDegrees = rotation,
                        isFrontCamera = useFrontCamera,
                        modifier = Modifier.fillMaxSize()
                    )
                    MlKitEffect.BarcodeScanning -> BarcodeOverlay(
                        barcodes = barcodeResults,
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        rotationDegrees = rotation,
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
