package eu.anifantakis.camerax_demo.ui.screens.realistic.media3

import androidx.camera.compose.CameraXViewfinder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import org.koin.androidx.compose.koinViewModel

/**
 * Media3Screen — thin composable, delegates all logic to [Media3ViewModel].
 *
 * Renders one of three states:
 *  1. Camera: CameraXViewfinder + Record/Stop button
 *  2. Processing: CircularProgressIndicator while Transformer runs
 *  3. Playback: ExoPlayer via PlayerSurface + Play/Pause + Record Again
 */
@Composable
fun Media3Screen(
    vm: Media3ViewModel = koinViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Bind camera when screen appears
    LaunchedEffect(Unit) { vm.bindCamera(lifecycleOwner) }

    val surfaceRequest by vm.surfaceRequest.collectAsStateWithLifecycle()
    val screenState by vm.screenState.collectAsStateWithLifecycle()
    val isRecording by vm.isRecording.collectAsStateWithLifecycle()
    val player by vm.player.collectAsStateWithLifecycle()

    when (screenState) {
        // ── CAMERA STATE ──────────────────────────────────────────
        Media3ScreenState.Camera -> {
            Box(Modifier.fillMaxSize()) {
                surfaceRequest?.let { req ->
                    CameraXViewfinder(
                        surfaceRequest = req,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Button(
                    onClick = { vm.toggleRecording() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp)
                ) {
                    Text(if (isRecording) "Stop" else "Record")
                }
            }
        }

        // ── PROCESSING STATE ──────────────────────────────────────
        Media3ScreenState.Processing -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(64.dp))
                Text(
                    "Processing video...",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        // ── PLAYBACK STATE ────────────────────────────────────────
        Media3ScreenState.Playback -> {
            Box(Modifier.fillMaxSize()) {
                player?.let { exo ->
                    PlayerSurface(
                        player = exo,
                        surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(onClick = {
                        player?.let { if (it.isPlaying) it.pause() else it.play() }
                    }) {
                        Text(if (player?.isPlaying == true) "Pause" else "Play")
                    }

                    Button(onClick = { vm.recordAgain() }) {
                        Text("Record Again")
                    }
                }
            }
        }
    }
}
