package eu.anifantakis.camerax_demo.ui.screens.simplistic

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.anifantakis.camerax_demo.ui.screens.realistic.DevicePosture
import eu.anifantakis.camerax_demo.ui.screens.realistic.rememberDevicePosture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Foldables & Adaptive UIs — Simplistic Example
 *
 * Combines WindowInfoTracker (foldable hinge detection) with WindowSizeClass
 * (width-based breakpoints) via [rememberDevicePosture] to produce four layouts:
 *
 *  - FullScreen: compact phone — preview fills screen, controls overlaid at bottom
 *  - TwoPane: tablet / landscape / fully-opened foldable — side-by-side Row
 *  - TableTop: half-opened foldable, horizontal hinge — preview top, controls bottom
 *  - Book: half-opened foldable, vertical hinge — preview left, controls right
 *
 * This is the self-contained version (no ViewModel). Camera binding happens
 * inline via LaunchedEffect. Compare with realistic/AdaptiveScreen.kt for the
 * ViewModel-based approach.
 */
@Composable
fun AdaptivePreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val scope = rememberCoroutineScope()

    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner) {
        var cameraProvider: ProcessCameraProvider? = null
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { req -> surfaceRequests.value = req }
        }

        val job = scope.launch {
            cameraProvider = ProcessCameraProvider.awaitInstance(context)
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview
            )
        }

        onDispose {
            job.cancel()
            cameraProvider?.unbind(preview)
            preview.surfaceProvider = null
        }
    }

    val posture = rememberDevicePosture()

    AnimatedContent(targetState = posture, label = "adaptive") { currentPosture ->
        when (currentPosture) {
            is DevicePosture.TableTop -> {
                Column(Modifier.fillMaxSize()) {
                    surfaceRequest?.let {
                        CameraXViewfinder(
                            surfaceRequest = it,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        Controls(Modifier.align(Alignment.Center))
                    }
                }
            }

            is DevicePosture.Book -> {
                Row(Modifier.fillMaxSize()) {
                    surfaceRequest?.let {
                        CameraXViewfinder(
                            surfaceRequest = it,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        Controls(Modifier.align(Alignment.Center))
                    }
                }
            }

            is DevicePosture.TwoPane -> {
                Row(Modifier.fillMaxSize()) {
                    surfaceRequest?.let {
                        CameraXViewfinder(
                            surfaceRequest = it,
                            modifier = Modifier.weight(1f).aspectRatio(9f / 16f)
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        Controls(Modifier.align(Alignment.Center))
                    }
                }
            }

            is DevicePosture.FullScreen -> {
                Box(Modifier.fillMaxSize()) {
                    surfaceRequest?.let {
                        CameraXViewfinder(surfaceRequest = it, modifier = Modifier.fillMaxSize())
                    }
                    Controls(Modifier.align(Alignment.BottomCenter))
                }
            }
        }
    }
}

@Composable
private fun Controls(modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Controls", fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { /* example */ }) { Text("Fake Btn 1") }
            OutlinedButton(onClick = { /* example */ }) { Text("Fake Btn 2") }
        }
    }
}
