package eu.anifantakis.camerax_demo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.anifantakis.camerax_demo.Route
import eu.anifantakis.camerax_demo.ui.components.Permission
import eu.anifantakis.camerax_demo.ui.components.PermissionGate

/**
 * Landing screen:
 *  - Requests CAMERA immediately so all samples can show a preview without extra prompts.
 *  - Shows navigation to each focused demo screen.
 *
 * Tip for readers: this mirrors real apps that ask for the camera once on entry to camera flows,
 * while deferring RECORD_AUDIO until the user actually tries to record video.
 */
@Composable
fun MainMenuScreen(nav: NavController) {
    PermissionGate(
        permission = Permission.CAMERA,
        contentNonGranted = { missing, humanReadable, requestPermissions ->
            // Minimal, inline prompt to re-ask for CAMERA if previously denied.
            PermissionNonGrantedContent(
                permissionsNonGranted = missing,
                humanReadablePermissionsNonGranted = humanReadable,
                requestMissingPermissions = requestPermissions
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text("CameraX Compose Examples", style = MaterialTheme.typography.headlineSmall)

            // Each route isolates a specific concept so readers can test them independently.
            Button(onClick = { nav.navigate(Route.Preview.path) }, modifier = Modifier.fillMaxWidth()) {
                Text("Preview Only (VM)")
            }
            Button(onClick = { nav.navigate(Route.Interactive.path) }, modifier = Modifier.fillMaxWidth()) {
                Text("Interactive (Focus & Zoom) (VM)")
            }
            Button(onClick = { nav.navigate(Route.Capture.path) }, modifier = Modifier.fillMaxWidth()) {
                Text("Photo & Video Capture (VM)")
            }
            Button(onClick = { nav.navigate(Route.Adaptive.path) }, modifier = Modifier.fillMaxWidth()) {
                Text("Adaptive/Foldables Demo (VM)")
            }
        }
    }
}

/**
 * Tiny reusable UI that demonstrates PermissionGate's re-request callback.
 * Click "Grant ..." to re-launch the system permission dialog for the missing set.
 */
@Composable
private fun PermissionNonGrantedContent(
    modifier: Modifier = Modifier,
    permissionsNonGranted: List<String>,
    humanReadablePermissionsNonGranted: String,
    requestMissingPermissions: (List<String>) -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = buildString {
                append("This screen needs: ")
                append(humanReadablePermissionsNonGranted)
                append(".")
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { requestMissingPermissions(permissionsNonGranted) }) {
            Text(
                if (permissionsNonGranted.size == 1) "Grant $humanReadablePermissionsNonGranted"
                else "Grant Permissions"
            )
        }
    }
}
