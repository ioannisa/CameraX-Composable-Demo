package eu.anifantakis.camerax_demo.ui.screens.legacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.anifantakis.camerax_demo.LegacyRoute
import eu.anifantakis.camerax_demo.ui.components.Permission
import eu.anifantakis.camerax_demo.ui.components.PermissionGate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

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
@NonRestartableComposable
@Composable
fun LegacyMenuScreen(nav: NavController) {
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
                Text("Legacy Examples", style = MaterialTheme.typography.headlineSmall)
            }
            item {
                Text(
                    "The OLD way: AndroidView + PreviewView.\nCompare with Simplistic to see the difference!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Part 1: camera-compose (legacy counterparts) ────────
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
                        "Legacy counterparts using AndroidView + PreviewView.\nCompare side-by-side with the Simplistic tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(LegacyRoute.BasicPreview.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Basic Camera Preview")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(LegacyRoute.CameraSwitching.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Camera Switching")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(LegacyRoute.TapToFocus.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tap-to-Focus & Pinch-to-Zoom")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(LegacyRoute.Controller.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tap-to-Focus & Zoom (Controller)")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(LegacyRoute.PhotoVideoCapture.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Photo & Video Capture")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(LegacyRoute.Effects.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Compose Effects (Most Fail!)")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(LegacyRoute.ContentScale.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ScaleType (Legacy Scaling)")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(LegacyRoute.Adaptive.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Foldables & Adaptive UIs")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(LegacyRoute.ManualExposure.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manual Exposure (Camera2Interop)")
                }
            }

            // ── Part 2: CameraX 1.6 Features ──────────────────────
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
                        "Part 2: CameraX 1.6 Features",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "SessionConfig, ExtensionSessionConfig, suspending APIs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(LegacyRoute.SessionConfig.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SessionConfig + Feature Groups")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(LegacyRoute.Extensions.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ExtensionSessionConfig (1.6)")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(LegacyRoute.FeatureGroups.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Feature Groups Query")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(LegacyRoute.HighSpeedVideo.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("High-Speed Video (120/240fps)")
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
                        "Pre-existing CameraX features — same APIs in Legacy and Compose.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(LegacyRoute.MlKit.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ML Kit Vision Effects")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(LegacyRoute.LensSelection.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Physical Lens Selection")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(LegacyRoute.ZoomLensSelection.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Zoom-Based Lens Selection")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(LegacyRoute.Media3.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CameraX + Media3 Pipeline")
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
