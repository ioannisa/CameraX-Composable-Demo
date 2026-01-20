package eu.anifantakis.camerax_demo.ui.screens.realistic

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
import eu.anifantakis.camerax_demo.RealisticRoute
import eu.anifantakis.camerax_demo.ui.components.Permission
import eu.anifantakis.camerax_demo.ui.components.PermissionGate

/**
 * Menu screen for Realistic examples.
 *
 * These demonstrate production-ready patterns using ViewModel
 * for state management and proper separation of concerns.
 */
@Composable
fun RealisticMenuScreen(nav: NavController) {
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
            Text("Realistic Examples", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Production-ready patterns with ViewModel.\nProper separation of concerns.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { nav.navigate(RealisticRoute.Preview.path) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Preview Only (VM)")
            }
            Button(
                onClick = { nav.navigate(RealisticRoute.Interactive.path) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Interactive (Focus & Zoom) (VM)")
            }
            Button(
                onClick = { nav.navigate(RealisticRoute.Capture.path) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Photo & Video Capture (VM)")
            }
            Button(
                onClick = { nav.navigate(RealisticRoute.Adaptive.path) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Adaptive/Foldables Demo (VM)")
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
