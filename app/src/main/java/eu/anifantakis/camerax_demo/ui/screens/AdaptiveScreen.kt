package eu.anifantakis.camerax_demo.ui.screens

import androidx.camera.compose.CameraXViewfinder
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.anifantakis.camerax_demo.ui.CameraViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Simple "adaptive" layout sample:
 *  - Uses a switch to simulate a wide/expanded layout (like tabletop/foldables).
 *  - Shows that CameraXViewfinder behaves like any other composable in Rows/Columns.
 *  - The 9:16 aspect hints at letterboxing/cropping behavior in split mode.
 */
@Composable
fun AdaptiveScreen(
    vm: CameraViewModel = koinViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) { vm.bindPreview(lifecycleOwner) }
    val request by vm.surfaceRequest.collectAsState(initial = null)
    var expanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Adaptive/Foldables Demo", style = MaterialTheme.typography.titleMedium)
            // Real apps would react to WindowSizeClass or posture APIs; we keep a switch for clarity.
            Switch(checked = expanded, onCheckedChange = { expanded = it })
        }

        Spacer(Modifier.height(8.dp))

        AnimatedContent(targetState = expanded, label = "adaptive") { isExpanded ->
            if (isExpanded) {
                Row(Modifier.fillMaxSize()) {
                    request?.let {
                        CameraXViewfinder(
                            surfaceRequest = it,
                            modifier = Modifier.weight(1f).aspectRatio(9f / 16f)
                        )
                    }
                    Box(Modifier.weight(1f)) { DemoControls(Modifier.align(Alignment.Center)) }
                }
            } else {
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
    Row(modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { /* example toggle */ }) { Text("Grid") }
        OutlinedButton(onClick = { /* example toggle */ }) { Text("Level") }
    }
}
