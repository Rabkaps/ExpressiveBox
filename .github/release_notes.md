### What's New in v1.0.63

#### Added
- Created `proguard-rules.pro` configuration for release minification (R8). Safely keeps JNI callback bindings (`libbox`), go-runtime wrappers (`go`), and serialization schemas (`kotlinx.serialization`) intact.

#### Fixed
- **Launch Performance Lag**: Resolved reflection-based accent color resource lookup jank by wrapping Monet theme palette derivation in a `remember` block, preventing `resources.getIdentifier` from being called 20+ times on every recomposition.
- **Scroll Frame Drops**: Replaced list auto-scroll coroutine animations (`animateScrollToItem()`) with instant programmatic jumps (`scrollToItem()`) inside the engine log viewer, avoiding continuous layout invalidation when receiving burst logs on connection.
- **Proxy List Scrolling Stutter**: Optimized item stagger animations in the main server listing. If the stagger entrance completes, individual items bypass reading animated progress states and render static draw parameters directly, eliminating recompositions during scrolling.
- **Immersive Edge-to-Edge Navigation**: Replaced the root container safe area/margin padding with a full-screen layout. The settings drawer now slides smoothly from the absolute screen boundaries, and action app bars stretch flush to status and navigation bar areas.
- **App Menu Version Display**: Bumped the displayed application version to `v1.0.63` in the Drawer layout.

#### Optimized
- Enabled R8 minification (`isMinifyEnabled = true`) on release configurations for maximum runtime execution speed, smaller APK size, and smoother layout animations.
- Bound debug signature config to release build type to simplify testing of production-optimized builds.

---

### ExpressiveBox Release Installation Note

When installing this APK, Google Play Protect may display a warning such as **"Blocked by Play Protect"** or **"Unrecognized Developer"**. 

This warning appears because the APK is built and signed using a developer debug signature rather than a signing key registered with the Google Play Store console.

#### How to proceed with installation:
1. In the Play Protect dialog, tap **"More details"**.
2. Tap **"Install anyway"** to complete the installation.
3. If the installation is still blocked, you can temporarily disable scanning:
   * Open the **Google Play Store** app.
   * Tap your profile icon in the top right -> **Play Protect** -> tap the **Settings** (gear) icon in the top right.
   * Toggle off **"Scan apps with Play Protect"**.
