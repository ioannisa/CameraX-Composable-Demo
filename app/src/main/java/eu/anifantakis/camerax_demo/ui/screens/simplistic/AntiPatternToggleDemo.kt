package eu.anifantakis.camerax_demo.ui.screens.simplistic

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * ==================================================================
 * ANTI-PATTERN DEMO: Toggle Camera On/Off Without Cleanup
 * ==================================================================
 *
 * This screen lets you toggle the camera section on and off.
 * It contains TWO implementations side by side:
 *
 * [AntiPatternToggleDemo] — uses [LaunchedEffect] (BROKEN):
 *   1. Toggle ON  → LaunchedEffect binds Preview to the lifecycle owner.
 *   2. Toggle OFF → composable leaves, coroutine cancelled, but use case stays bound.
 *   3. Toggle ON  → new LaunchedEffect binds a SECOND Preview to the same lifecycle.
 *   CameraX does not allow duplicate use case types — this crashes or silently breaks.
 *
 * [FixedToggleDemo] — uses [DisposableEffect] (CORRECT):
 *   1. Toggle ON  → DisposableEffect binds Preview.
 *   2. Toggle OFF → onDispose fires, explicitly unbinds Preview.
 *   3. Toggle ON  → fresh bind with no stale use cases. Works every time.
 *
 * Try both: toggle the camera on, off, and on again. The broken version
 * will fail on the second "on". The fixed version works indefinitely.
 */

// ════════════════════════════════════════════════════════════════════
//  BROKEN: LaunchedEffect — no cleanup on toggle off
// ════════════════════════════════════════════════════════════════════

@Composable
fun AntiPatternToggleDemo() {
    var showCamera by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = { showCamera = !showCamera },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(if (showCamera) "Hide Camera (BROKEN)" else "Show Camera (BROKEN)")
        }

        if (showCamera) {
            // This section enters and leaves composition with the toggle.
            // LaunchedEffect binds the camera but never unbinds it.
            BrokenCameraSection()
        }
    }
}

@Composable
private fun BrokenCameraSection() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    // BUG: LaunchedEffect cancels the coroutine on exit, but does NOT unbind the Preview.
    // Next time this composable enters, a second Preview is bound → crash or broken state.
    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { req -> surfaceRequests.value = req }
        }

        // Uncommenting the line below prevents the crash, but this is STILL an anti-pattern:
        // the camera stays open (green light on) after toggling off, because nothing unbinds
        // it when the composable leaves. Only DisposableEffect releases it at the right moment.
        // cameraProvider.unbindAll()

        cameraProvider.bindToLifecycle(
            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview
        )
    }

    Box(Modifier.fillMaxSize()) {
        surfaceRequest?.let {
            CameraXViewfinder(surfaceRequest = it, modifier = Modifier.fillMaxSize())
        }
        Text(
            "LaunchedEffect — toggle off and on again to see the problem",
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}

// ════════════════════════════════════════════════════════════════════
//  FIXED: DisposableEffect — explicit cleanup on toggle off
// ════════════════════════════════════════════════════════════════════

@Composable
fun FixedToggleDemo() {
    var showCamera by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = { showCamera = !showCamera },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(if (showCamera) "Hide Camera (FIXED)" else "Show Camera (FIXED)")
        }

        if (showCamera) {
            FixedCameraSection()
        }
    }
}

@Composable
private fun FixedCameraSection() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    // CORRECT: DisposableEffect unbinds the Preview when the composable leaves.
    // Next time it enters, the bind starts from a clean state.
    DisposableEffect(Unit) {
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

    Box(Modifier.fillMaxSize()) {
        surfaceRequest?.let {
            CameraXViewfinder(surfaceRequest = it, modifier = Modifier.fillMaxSize())
        }
        Text(
            "DisposableEffect — toggle as many times as you want",
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}
