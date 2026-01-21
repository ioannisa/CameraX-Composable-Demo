package eu.anifantakis.camerax_demo.ui.screens.legacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.anifantakis.camerax_demo.LegacyRoute
import eu.anifantakis.camerax_demo.ui.components.Permission
import eu.anifantakis.camerax_demo.ui.components.PermissionGate

/**
 * Menu screen for Legacy (AndroidView + PreviewView) examples.
 *
 * These examples demonstrate the OLD way of doing camera in Compose:
 * - AndroidView wrapping PreviewView
 * - View-based touch handling
 * - Imperative camera binding
 *
 * Compare these with the Simplistic examples to see the difference!
 */
@Composable
fun LegacyMenuScreen(nav: NavController) {
    PermissionGate(
        permission = Permission.CAMERA,
        contentNonGranted = { missing, humanReadable, requestPermissions ->
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
            Text("Legacy Examples", style = MaterialTheme.typography.headlineSmall)
            Text(
                "The OLD way: AndroidView + PreviewView.\nCompare with Simplistic to see the difference!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { nav.navigate(LegacyRoute.BasicPreview.path) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Basic Camera Preview")
            }

            Button(
                onClick = { nav.navigate(LegacyRoute.CameraSwitching.path) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Camera Switching")
            }

            Button(
                onClick = { nav.navigate(LegacyRoute.TapToFocus.path) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tap-to-Focus & Pinch-to-Zoom")
            }

            Button(
                onClick = { nav.navigate(LegacyRoute.PhotoVideoCapture.path) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Photo & Video Capture")
            }

            Button(
                onClick = { nav.navigate(LegacyRoute.Adaptive.path) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Foldables & Adaptive UIs")
            }

            Button(
                onClick = { nav.navigate(LegacyRoute.Effects.path) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Compose Effects (Most Fail!)")
            }
        }
    }
}

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
            text = "This screen needs: $humanReadablePermissionsNonGranted.",
            style = MaterialTheme.typography.titleMedium
        )
        Button(
            onClick = { requestMissingPermissions(permissionsNonGranted) },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                if (permissionsNonGranted.size == 1) "Grant $humanReadablePermissionsNonGranted"
                else "Grant Permissions"
            )
        }
    }
}
