package eu.anifantakis.camerax_demo.ui.screens.realistic

import android.app.Activity
import androidx.camera.compose.CameraXViewfinder
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
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import org.koin.androidx.compose.koinViewModel

/**
 * Device posture detected from WindowInfoTracker + WindowSizeClass.
 *
 * Priority:
 *  1. If a HALF_OPENED FoldingFeature exists → TableTop or Book (real foldable, mid-fold)
 *  2. Else fall back to WindowSizeClass width → FullScreen or TwoPane
 */
sealed interface DevicePosture {
    /** Phone in portrait, or any compact-width window */
    data object FullScreen : DevicePosture

    /** Large enough width (tablet, landscape, fully-opened foldable) — two-pane layout */
    data object TwoPane : DevicePosture

    /** Foldable half-opened with a horizontal hinge — top half preview, bottom half controls */
    data class TableTop(val hingePosition: Int) : DevicePosture

    /** Foldable half-opened with a vertical hinge — side-by-side like a book */
    data class Book(val hingePosition: Int) : DevicePosture
}

/**
 * Observes [WindowInfoTracker] for [FoldingFeature] changes and combines with
 * [WindowWidthSizeClass] to produce a [DevicePosture].
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberDevicePosture(): DevicePosture {
    val context = LocalContext.current
    val activity = context as Activity

    val windowSizeClass = calculateWindowSizeClass(activity)
    var foldingFeature by remember { mutableStateOf<FoldingFeature?>(null) }

    LaunchedEffect(activity) {
        WindowInfoTracker.getOrCreate(activity)
            .windowLayoutInfo(activity)
            .collect { layoutInfo ->
                foldingFeature = layoutInfo.displayFeatures
                    .filterIsInstance<FoldingFeature>()
                    .firstOrNull()
            }
    }

    return remember(foldingFeature, windowSizeClass) {
        val fold = foldingFeature
        when {
            // Half-opened foldable — hinge orientation determines layout
            fold != null && fold.state == FoldingFeature.State.HALF_OPENED -> {
                if (fold.orientation == FoldingFeature.Orientation.HORIZONTAL) {
                    DevicePosture.TableTop(hingePosition = fold.bounds.top)
                } else {
                    DevicePosture.Book(hingePosition = fold.bounds.left)
                }
            }
            // Not folding — fall back to window width
            windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact -> {
                DevicePosture.TwoPane
            }
            else -> DevicePosture.FullScreen
        }
    }
}

/**
 * Foldables & Adaptive UIs — Realistic Example (with ViewModel)
 *
 * Combines WindowInfoTracker (foldable hinge detection) with WindowSizeClass
 * (width-based breakpoints) to produce four layout variants:
 *
 *  - FullScreen: compact phone — preview fills screen, controls overlaid at bottom
 *  - TwoPane: tablet / landscape / fully-opened foldable — side-by-side Row
 *  - TableTop: half-opened foldable, horizontal hinge — preview on top, controls on bottom
 *  - Book: half-opened foldable, vertical hinge — preview left, controls right
 */
@Composable
fun AdaptiveScreen(
    vm: CameraViewModel = koinViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(Unit) {
        vm.bindPreview(lifecycleOwner)
        onDispose { vm.unbindCamera() }
    }
    val request by vm.surfaceRequest.collectAsStateWithLifecycle(initialValue = null)

    val posture = rememberDevicePosture()

    AnimatedContent(targetState = posture, label = "adaptive") { currentPosture ->
        when (currentPosture) {
            is DevicePosture.TableTop -> {
                // Horizontal hinge: preview on top half, controls below the hinge
                Column(Modifier.fillMaxSize()) {
                    request?.let {
                        CameraXViewfinder(
                            surfaceRequest = it,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        DemoControls(Modifier.align(Alignment.Center))
                    }
                }
            }

            is DevicePosture.Book -> {
                // Vertical hinge: preview on left, controls on right
                Row(Modifier.fillMaxSize()) {
                    request?.let {
                        CameraXViewfinder(
                            surfaceRequest = it,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        DemoControls(Modifier.align(Alignment.Center))
                    }
                }
            }

            is DevicePosture.TwoPane -> {
                // Wide screen (tablet, landscape, fully-opened foldable): side-by-side
                Row(Modifier.fillMaxSize()) {
                    request?.let {
                        CameraXViewfinder(
                            surfaceRequest = it,
                            modifier = Modifier.weight(1f).aspectRatio(9f / 16f)
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        DemoControls(Modifier.align(Alignment.Center))
                    }
                }
            }

            is DevicePosture.FullScreen -> {
                // Compact phone: full-screen preview with controls overlaid
                Box(Modifier.fillMaxSize()) {
                    request?.let {
                        CameraXViewfinder(surfaceRequest = it, modifier = Modifier.fillMaxSize())
                    }
                    DemoControls(Modifier.align(Alignment.BottomCenter))
                }
            }
        }
    }
}

@Composable
private fun DemoControls(modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Controls", fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { /* example toggle */ }) { Text("Fake Btn 1") }
            OutlinedButton(onClick = { /* example toggle */ }) { Text("Fake Btm 2") }
        }
    }
}
