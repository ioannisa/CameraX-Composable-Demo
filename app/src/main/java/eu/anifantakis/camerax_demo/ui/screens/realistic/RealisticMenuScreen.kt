package eu.anifantakis.camerax_demo.ui.screens.realistic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.anifantakis.camerax_demo.RealisticRoute
import eu.anifantakis.camerax_demo.ui.components.Permission
import eu.anifantakis.camerax_demo.ui.components.PermissionGate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Menu screen for Realistic examples.
 *
 * These demonstrate production-ready patterns using ViewModel
 * for state management and proper separation of concerns.
 */
@NonRestartableComposable
@Composable
fun RealisticMenuScreen(nav: NavController) {
    PermissionGate(
        permission = Permission.CAMERA,
        contentNonGranted = { missing, humanReadable, requestPermissions ->
            PermissionNonGrantedContent(
                permissionsNonGranted = missing.toImmutableList(),
                humanReadablePermissionsNonGranted = humanReadable,
                requestMissingPermissions = requestPermissions
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Realistic Examples", style = MaterialTheme.typography.headlineSmall)
            }
            item {
                Text(
                    "Production-ready patterns with ViewModel.\nProper separation of concerns.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Part 1: camera-compose ──────────────────────────────
            stickyHeader {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "Part 1: camera-compose",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Production-ready Compose patterns with ViewModel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(RealisticRoute.Preview.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Preview Only (VM)")
                }
            }
            item {
                Button(
                    onClick = { nav.navigate(RealisticRoute.Interactive.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Interactive (Focus & Zoom) (VM)")
                }
            }
            item {
                Button(
                    onClick = { nav.navigate(RealisticRoute.Capture.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Photo & Video Capture (VM)")
                }
            }
            item {
                Button(
                    onClick = { nav.navigate(RealisticRoute.Adaptive.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Adaptive/Foldables Demo (VM)")
                }
            }

            // ── Part 3: Broader Ecosystem ───────────────────────────
            stickyHeader {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "Part 3: Broader Ecosystem",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Pre-existing CameraX features with ViewModel architecture.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(RealisticRoute.MlKit.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ML Kit Vision Effects (VM)")
                }
            }
            item {
                Button(
                    onClick = { nav.navigate(RealisticRoute.Media3.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CameraX + Media3 Pipeline (VM)")
                }
            }
        }
    }
}

@Composable
private fun PermissionNonGrantedContent(
    modifier: Modifier = Modifier,
    permissionsNonGranted: ImmutableList<String>,
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
