package eu.anifantakis.camerax_demo.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Small, declarative permission helper for Compose.
 *
 * Why not a dialog in Activity? Keeping permission flow inside the Compose tree:
 *  - avoids fragment/Activity callbacks,
 *  - keeps the "ask again" UI colocated with the screen that needs it,
 *  - supports mic-only re-prompts (common for video recording).
 */
enum class Permission {
    CAMERA,
    RECORD_AUDIO
}

/**
 * Convenience overload for a single permission (most call sites).
 */
@Composable
fun PermissionGate(
    permission: Permission,
    modifier: Modifier = Modifier,
    requestOnLaunch: Boolean = true,
    // If not all requested permissions are granted, we render this slot.
    // We pass: the missing list, a human-readable label, and a function to (re)request any subset.
    contentNonGranted: @Composable ((missing: List<String>, humanReadable: String, requestPermissions: (List<String>) -> Unit) -> Unit)? = null,
    // When everything is granted, render the actual screen content.
    contentGranted: @Composable () -> Unit
) {
    PermissionGate(
        permissions = listOf(permission),
        modifier = modifier,
        requestOnLaunch = requestOnLaunch,
        contentNonGranted = contentNonGranted,
        contentGranted = contentGranted
    )
}

/**
 * Multiple-permission version (e.g., ask CAMERA+RECORD_AUDIO together for a recording screen).
 */
@SuppressLint("MissingPermission")
@Composable
fun PermissionGate(
    permissions: List<Permission>,
    modifier: Modifier = Modifier,
    requestOnLaunch: Boolean = true,
    contentNonGranted: @Composable ((missing: List<String>, humanReadable: String, requestPermissions: (List<String>) -> Unit) -> Unit)? = null,
    contentGranted: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Map enum → framework strings (kept distinct so we can address subsets later).
    val requestedPermissions = remember(permissions) {
        permissions.map {
            when (it) {
                Permission.CAMERA -> Manifest.permission.CAMERA
                Permission.RECORD_AUDIO -> Manifest.permission.RECORD_AUDIO
            }
        }.distinct()
    }

    // Track current grant state per permission (Compose-local state).
    var granted by remember(requestedPermissions) {
        mutableStateOf(
            requestedPermissions.associateWith { p ->
                ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    val allGranted by remember { derivedStateOf { granted.values.all { it } } }

    // A single launcher that can re-request any subset (CAMERA only, MIC only, or both).
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        granted = granted.toMutableMap().apply { putAll(results) }
    }

    // Function we pass down to the screen's "ask again" UI.
    val requestPermissions: (List<String>) -> Unit = remember(launcher) {
        { perms -> launcher.launch(perms.toTypedArray()) }
    }

    // Optionally request on first composition.
    LaunchedEffect(requestedPermissions) {
        if (requestOnLaunch && !allGranted) {
            requestPermissions(requestedPermissions)
        }
    }

    // Render based on state.
    if (allGranted) {
        contentGranted()
    } else {
        val missing = requestedPermissions.filter { granted[it] != true }
        val humanReadable = missing.joinToString(", ") { prettyName(it) }
        contentNonGranted?.invoke(missing, humanReadable, requestPermissions)
        // (We intentionally avoid a default fallback UI; each screen shows a tailored prompt.)
    }
}

/** Turns "android.permission.RECORD_AUDIO" into "Microphone" for copy that users can parse. */
private fun prettyName(androidPermission: String): String = when (androidPermission) {
    Manifest.permission.CAMERA -> "Camera"
    Manifest.permission.RECORD_AUDIO -> "Microphone"
    else -> androidPermission.substringAfterLast('.')
        .replace('_', ' ')
        .lowercase()
        .replaceFirstChar { it.titlecase() }
}
