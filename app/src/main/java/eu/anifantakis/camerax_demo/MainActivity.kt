package eu.anifantakis.camerax_demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import eu.anifantakis.camerax_demo.ui.theme.CameraXComposableDemoTheme

/**
 * Single-activity entry point.
 * - Enables edge-to-edge to give the camera more space.
 * - Hosts the app's NavHost inside a Material3 Scaffold.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CameraXComposableDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Main navigation graph (see: CameraNavHost in your project)
                    CameraNavHost(paddingValues = innerPadding)
                }
            }
        }
    }
}