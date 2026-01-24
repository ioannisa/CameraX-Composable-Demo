# CameraX Goes Compose

### No more `AndroidView`. No more `PreviewView`. Just pure Compose.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-blue.svg)](https://kotlinlang.org)
[![CameraX](https://img.shields.io/badge/CameraX-1.5.2-orange.svg)](https://developer.android.com/jetpack/androidx/releases/camera)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2026.01-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

---

**Companion repository for:**
- **Article**: [Goodbye AndroidView: CameraX Goes Full Compose](https://proandroiddev.com/goodbye-androidview-camerax-goes-full-compose-4d21ca234c4e)
- **Presentation**: *"CameraX Goes Compose"* — Droidcon 2025

---

## The Problem We All Faced

```kotlin
// The old way - a View island in your Compose UI
AndroidView(
    factory = { context -> PreviewView(context) }
)
```

For years, camera development was where modern Android stopped. You could use Compose everywhere... except the camera screen forced you back into View interop.

**That era is over.**

## The New Way

```kotlin
// Pure Compose - no AndroidView, no PreviewView
surfaceRequest?.let {
    CameraXViewfinder(
        surfaceRequest = it,
        modifier = Modifier.fillMaxSize()
    )
}
```

Announced at **Google I/O 2025** and now **stable**, the new `CameraXViewfinder` composable renders camera frames directly in Compose. One paradigm. One mental model. Pure Compose.

---

## What This Demo Covers

| Category | What You'll Learn |
|----------|-------------------|
| **Basic Preview** | The new `SurfaceRequest` → `StateFlow` → `CameraXViewfinder` pattern |
| **Camera Switching** | `rememberSaveable` + `LaunchedEffect(selector)` for front/back toggle |
| **Tap-to-Focus** | `MutableCoordinateTransformer` — no more coordinate math nightmares |
| **Pinch-to-Zoom** | Standard Compose gestures with `detectTransformGestures` |
| **Photo & Video** | Binding `Preview` + `ImageCapture` + `VideoCapture` together |
| **Adaptive Layouts** | `WindowSizeClass` for foldables and tablets |
| **Compose Effects** | Blur, alpha, rotation, grayscale — impossible with PreviewView! |
| **Camera2Interop** | Manual ISO & shutter speed — extending CameraX with Camera2 parameters |
| **Permissions** | Declarative `PermissionGate` inside the Compose tree |
| **ViewModel Architecture** | Production-ready patterns with proper state management |

---

## Project Structure

```
├── legacy/                  # THE OLD WAY: AndroidView + PreviewView
│   ├── LegacyBasicPreview       → AndroidView wrapping PreviewView
│   ├── LegacyCameraSwitching    → DisposableEffect coordination
│   ├── LegacyTapToFocus         → View-based touch handling
│   ├── LegacyPhotoVideoCapture  → View island in Compose
│   ├── LegacyAdaptivePreview    → SurfaceView animation issues
│   ├── LegacyEffectsPreview     → Compose effects DON'T work!
│   └── LegacyManualExposure     → Camera2Interop for ISO/shutter
│
├── simplistic/              # THE NEW WAY: Pure Compose
│   ├── BasicCameraPreview       → CameraXViewfinder + StateFlow
│   ├── CameraSwitchingPreview   → LaunchedEffect(selector)
│   ├── TapToFocusPreview        → MutableCoordinateTransformer
│   ├── PhotoVideoCapturePreview → Multi-use-case binding
│   ├── AdaptivePreview          → Smooth Compose animations
│   ├── EffectsPreview           → Compose effects WORK!
│   └── ManualExposurePreview    → Camera2Interop for ISO/shutter
│
├── realistic/               # Production-ready with ViewModel
│   ├── CameraViewModel          → State management
│   ├── PreviewOnlyScreen        → Basic with ViewModel
│   ├── InteractivePreviewScreen → Focus/zoom with ViewModel
│   ├── CaptureScreen            → Full capture flow
│   └── AdaptiveScreen           → Adaptive with ViewModel
│
└── components/
    └── PermissionGate           → Declarative permission handling
```

**Legacy** = The old `AndroidView` + `PreviewView` approach (for comparison)
**Simplistic** = The new pure Compose approach (learn in isolation)
**Realistic** = Production-ready patterns with ViewModel

---

## The Core Pattern

CameraX now follows the same unidirectional data flow you use everywhere else:

```
┌─────────────┐      ┌──────────────┐      ┌───────────────────┐
│   CameraX   │ ──── │  StateFlow   │ ──── │ CameraXViewfinder │
│   Preview   │      │SurfaceRequest│      │    Composable     │
└─────────────┘      └──────────────┘      └───────────────────┘
    produces            holds                  consumes
```

```kotlin
@Composable
fun CameraPreview() {
    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val surfaceRequest by surfaceRequests.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { req -> surfaceRequests.value = req }
        }
        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
    }

    surfaceRequest?.let {
        CameraXViewfinder(surfaceRequest = it, modifier = Modifier.fillMaxSize())
    }
}
```

---

## Tap-to-Focus: The Magic

The old nightmare: Compose coords ≠ View coords ≠ Sensor coords. Manual matrix math. Testing marathons.

**The new reality:**

```kotlin
val transformer = remember { MutableCoordinateTransformer() }

CameraXViewfinder(
    surfaceRequest = request,
    coordinateTransformer = transformer,
    modifier = Modifier.pointerInput(Unit) {
        detectTapGestures { offset ->
            // One line. That's it.
            val pt = with(transformer) { offset.transform() }

            // Now use pt for focus metering
            val factory = SurfaceOrientedMeteringPointFactory(
                request.resolution.width.toFloat(),
                request.resolution.height.toFloat()
            )
            val action = FocusMeteringAction.Builder(
                factory.createPoint(pt.x, pt.y)
            ).build()
            camera.cameraControl.startFocusAndMetering(action)
        }
    }
)
```

Works in portrait. Works in landscape. Works on tablets. Works on foldables. Works with front camera mirroring.

**You're not doing the math. The transformer is.**

---

## Quick Start

### 1. Add Dependencies

```kotlin
// build.gradle.kts
val cameraxVersion = "1.5.2"

dependencies {
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-video:$cameraxVersion")
    implementation("androidx.camera:camera-compose:$cameraxVersion")  // The new one!
}
```

### 2. Add Permissions

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

### 3. Clone and Run

```bash
git clone https://github.com/ioannisa/camerax-compose-demo.git
cd camerax-compose-demo
./gradlew installDebug
```

---

## Why This Matters

| Old World | New World |
|-----------|-----------|
| `AndroidView` wrapper | Native `CameraXViewfinder` |
| `getInstance().get()` blocking | `awaitInstance()` coroutine |
| Manual coordinate transforms | `MutableCoordinateTransformer` |
| View lifecycle + Compose lifecycle | Just Compose lifecycle |
| Camera as "special case" | Camera as normal composable |

> *"For years, camera development was the place where modern Android stopped."*
>
> **That compromise is over.**

---

## The Rendering Black Box Problem

PreviewView is a **View** — a foreign object in your Compose tree. It doesn't participate in Compose's graphics layer:

| Effect | PreviewView | CameraXViewfinder EMBEDDED |
|--------|-------------|---------------------------|
| Circle Clip | ✓ | ✓ |
| Blur | ✗ | ✓ |
| Alpha 50% | ✗ | ✓ |
| Rotation | ✗ | ✓ |
| Grayscale | ✗ | ✓ |

```kotlin
// With PreviewView - these DON'T work:
AndroidView(
    factory = { PreviewView(it) },
    modifier = Modifier
        .blur(20.dp)        // ✗ Ignored
        .alpha(0.5f)        // ✗ Ignored
        .graphicsLayer {
            rotationZ = 15f  // ✗ Ignored
        }
)

// With CameraXViewfinder EMBEDDED - they ALL work:
CameraXViewfinder(
    surfaceRequest = request,
    implementationMode = ImplementationMode.EMBEDDED,
    modifier = Modifier
        .blur(20.dp)        // ✓ Works!
        .alpha(0.5f)        // ✓ Works!
        .graphicsLayer {
            rotationZ = 15f  // ✓ Works!
        }
)
```

**CameraXViewfinder with EMBEDDED mode is a true composable.** It plays by Compose rules.

---

## Architecture Deep Dive

```
┌─────────────────────────────────────┐
│  UI Layer                           │  ← CameraXViewfinder (Compose)
│  (or PreviewView for Views)         │
├─────────────────────────────────────┤
│  CameraX                            │  ← Same for both UI approaches
│  camera-core, camera-lifecycle      │
├─────────────────────────────────────┤
│  camera-camera2                     │  ← Bridge artifact
├─────────────────────────────────────┤
│  Camera2 Framework                  │  ← The engine (always running)
└─────────────────────────────────────┘
```

**Key insight**: Camera2 isn't "old" — it's the engine underneath everything. CameraX abstracts it. The only difference between XML and Compose is the top UI layer.

---

## Camera2Interop: Extending CameraX

CameraX covers most camera features, but some "pro camera" features like **manual ISO and shutter speed** aren't exposed directly. For these, you use `Camera2Interop` to add Camera2 parameters to your CameraX use cases.

### What CameraX Provides vs What Needs Camera2Interop

| Feature | CameraX Native | Camera2Interop Needed |
|---------|----------------|----------------------|
| Exposure Compensation (EV) | ✓ | |
| Manual ISO | | ✓ |
| Manual Shutter Speed | | ✓ |
| Disable Auto-Exposure | | ✓ |
| Torch Intensity | | ✓ |

### The Key Insight: Extending, Not Bypassing

```
┌─────────────────────────────────────────────────────────────┐
│  WRONG: "Camera2Interop bypasses CameraX"                   │
│  ─────────────────────────────────────────────────────────  │
│  CameraX ──┐                                                │
│            │ OR   ← Not how it works!                       │
│  Camera2 ──┘                                                │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  CORRECT: "Camera2Interop adds parameters TO CameraX"       │
│  ─────────────────────────────────────────────────────────  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  CameraX (still managing everything)                  │  │
│  │  ┌─────────────────────────────────────────────────┐  │  │
│  │  │  + Camera2 CaptureRequest parameters            │  │  │
│  │  └─────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Example: Manual Exposure Control

```kotlin
@ExperimentalCamera2Interop
fun buildManualExposurePreview(iso: Int, shutterNs: Long): Preview {
    val builder = Preview.Builder()

    // Add Camera2 parameters to the CameraX use case
    Camera2Interop.Extender(builder).apply {
        setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
        setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso)
        setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, shutterNs)
    }

    return builder.build()
}
```

### Works Identically for Both Approaches

Camera2Interop happens at the **use case builder level**, not the UI level. This means:

| Approach | Camera2Interop Code | Result |
|----------|---------------------|--------|
| Legacy (PreviewView) | Identical | Same manual exposure control |
| Compose (CameraXViewfinder) | Identical | Same manual exposure control |

The demo includes `ManualExposurePreview` in both `legacy/` and `simplistic/` packages to demonstrate this — the Camera2Interop code is exactly the same, only the preview rendering differs.

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Min SDK | 26 |
| Target SDK | 36 |
| Kotlin | 2.3.0 |
| Compose BOM | 2026.01.00 |
| CameraX | 1.5.2 |

---

## Learn More

- **Full Article**: [Goodbye AndroidView: CameraX Goes Full Compose](https://proandroiddev.com/goodbye-androidview-camerax-goes-full-compose-4d21ca234c4e)
- **CameraX Docs**: [developer.android.com/training/camerax](https://developer.android.com/training/camerax)
- **CameraX Release Notes**: [androidx.dev/releases/camera](https://developer.android.com/jetpack/androidx/releases/camera)

---

## Author

**Ioannis Anifantakis**
Staff Android Engineer @ novibet

[![GitHub](https://img.shields.io/badge/GitHub-ioannisa-181717?logo=github)](https://github.com/ioannisa)
[![Web](https://img.shields.io/badge/Web-anifantakis.eu-blue)](https://anifantakis.eu)

---

## License

```
Copyright 2025 Ioannis Anifantakis

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

---

<p align="center">
  <b>Now go remove that AndroidView wrapper.</b>
</p>
