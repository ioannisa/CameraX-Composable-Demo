package eu.anifantakis.camerax_demo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.compose.CameraXViewfinder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.anifantakis.camerax_demo.ui.CameraViewModel
import eu.anifantakis.camerax_demo.ui.components.Permission
import eu.anifantakis.camerax_demo.ui.components.PermissionGate
import org.koin.androidx.compose.koinViewModel

/**
 * Full capture sample (photo + video).
 *
 * Patterns:
 *  - Bind [Preview], [ImageCapture] and [VideoCapture] together for a consistent pipeline.
 *  - Request RECORD_AUDIO lazily, exactly when the user taps "Record".
 *  - Reflect recording state in the UI (label toggles Record/Stop).
 */
@Composable
fun CaptureScreen(
    vm: CameraViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Bind all three use cases for this screen.
    LaunchedEffect(Unit) { vm.bindCapture(lifecycleOwner) }

    val request by vm.surfaceRequest.collectAsState(initial = null)
    val recording by vm.recording.collectAsState()

    Box(Modifier.fillMaxSize()) {
        request?.let { req ->
            CameraXViewfinder(surfaceRequest = req, modifier = Modifier.fillMaxSize())
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { vm.capturePhoto() }) {
                Text("Take Photo")
            }

            // Ask for mic only when the user initiates recording.
            // NOTE: Lint cannot infer PermissionGate's guarantee, so we add an explicit
            // checkSelfPermission here before calling a @RequiresPermission method.
            PermissionGate(permission = Permission.RECORD_AUDIO) {
                Button(onClick = {
                    val micGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (micGranted) {
                        vm.toggleRecording()
                    } else {
                        // Should not happen because PermissionGate only renders this slot
                        // when permission is granted — but guarding keeps Lint happy and
                        // prevents accidental crashes if the state changes.
                    }
                }) {
                    Text(if (recording == null) "Record" else "Stop")
                }
            }
        }
    }
}
