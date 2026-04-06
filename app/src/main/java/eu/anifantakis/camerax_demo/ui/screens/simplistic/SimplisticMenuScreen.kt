package eu.anifantakis.camerax_demo.ui.screens.simplistic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.anifantakis.camerax_demo.SimplisticRoute
import eu.anifantakis.camerax_demo.ui.components.Permission
import eu.anifantakis.camerax_demo.ui.components.PermissionGate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Menu screen for Simplistic examples.
 *
 * These are self-contained, minimal examples perfect for learning
 * the core CameraX + Compose patterns without ViewModel abstraction.
 */
@NonRestartableComposable
@Composable
fun SimplisticMenuScreen(nav: NavController) {
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
                Text("Simplistic Examples", style = MaterialTheme.typography.headlineSmall)
            }
            item {
                Text(
                    "Self-contained, minimal examples.\nNo ViewModel - just Compose patterns.",
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
                        "Compose-native integration with CameraXViewfinder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(SimplisticRoute.BasicPreview.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Basic Camera Preview")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(SimplisticRoute.CameraSwitching.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Camera Switching")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(SimplisticRoute.TapToFocus.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tap-to-Focus & Pinch-to-Zoom")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(SimplisticRoute.PhotoVideoCapture.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Photo & Video Capture")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(SimplisticRoute.Effects.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Compose Effects (All Work!)")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(SimplisticRoute.ContentScale.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ContentScale & Alignment")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(SimplisticRoute.Adaptive.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Foldables & Adaptive UIs")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(SimplisticRoute.ManualExposure.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manual Exposure (Camera2Interop)")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(SimplisticRoute.FullCamera.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Full Camera (Switch + Capture)")
                }
            }

            // ── Part 2: CameraX 1.5 / 1.6 ──────────────────────────────
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
                        "Part 2: CameraX 1.5 / 1.6",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Platform-wide improvements — same APIs in Legacy and Compose.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    onClick = { nav.navigate(SimplisticRoute.SessionConfig.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SessionConfig + Feature Groups")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(SimplisticRoute.Extensions.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ExtensionSessionConfig (1.6)")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(SimplisticRoute.FeatureGroups.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Feature Groups Query")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(SimplisticRoute.HighSpeedVideo.path) },
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
                        "Pre-existing CameraX features — smoother in Compose, same APIs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(SimplisticRoute.MlKit.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ML Kit Vision Effects")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(SimplisticRoute.LensSelection.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Physical Lens Selection")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(SimplisticRoute.ZoomLensSelection.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Zoom-Based Lens Selection")
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(SimplisticRoute.Media3.path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CameraX + Media3 Pipeline")
                }
            }

            // ── Anti-Pattern vs Best Practice ──────────────────────
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
                        "ANTI-PATTERN vs BEST PRACTICE",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                    Text(
                        "Toggle camera on/off — compare LaunchedEffect (crashes)\nwith DisposableEffect (works correctly).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(SimplisticRoute.AntiPatternToggle.path) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text("LaunchedEffect Toggle (BROKEN)", color = Color.White)
                }
            }

            item {
                Button(
                    onClick = { nav.navigate(SimplisticRoute.FixedToggle.path) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32)
                    )
                ) {
                    Text("DisposableEffect Toggle (CORRECT)", color = Color.White)
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
