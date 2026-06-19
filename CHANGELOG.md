# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.5.0] - 2026-06-19

### Added
- **Material 3 Expressive Animation & Transitions**: Added custom wavy circular progress indicators (`CircularWavyProgressIndicator`), pulsing connection status dot inside dashboard pills, and horizontal tab transition scaling and fade effects.
- **Swipeable Bottom Navigation**: Fully integrated `HorizontalPager` to allow users to swipe between Home, Configs, Logs, and Settings tabs smoothly.
- **DNS Poisoning & Hijacking Mitigation**: Implemented a kotlin-layer parallel DNS pre-resolver using public/secure DNS endpoints to bypass carrier DNS poisoning, injecting resolved IP directly into configurations.
- **Advanced Transport Protocol Creator/Editor**: Added transport configuration parameters inside the app creator/editor for VLESS, Trojan, and Shadowsocks protocols, supporting WebSocket, gRPC, mKCP, and HTTPUpgrade.
- **Translucent Popup Activity (`NodesPopupActivity`)**: Created a translucent overlay dialog activity launched directly from the connection status notification "List" action to display configurations, search, pings, and sub selector on top of other apps.
- **Subscription Auto-Connect & Selector**:
  - Auto-Connect: Evaluates latency of all servers in the active subscription in parallel on connection startup and connects to the one with the lowest delay.
  - Subscription Selector: Added a horizontal scrollable row of chips to easily switch between subscriptions on both the main screen nodes overlay and the popup dialog.
- **Improved Connection Notification visibility**: Re-configured standard notifications to use a default importance channel (`vpn_service_channel_v2`) and builder priority, keeping action buttons ("List" and "Disconnect") visible without being collapsed or hidden in silent sections.

### Fixed
- **Stuck Tab Animations**: Resolved a visual glitch where rapid tab jumps or jumps across non-adjacent tabs (e.g. Home to Logs) would cause the animated screen to get stuck in a scaled-down, faded-out intermediate state. Moved visual transforms inside the `graphicsLayer` lambda draw phase.
- **Live Stats Default Value**: Reconfigured default settings so that the live stats speed indicators are disabled by default.

## [1.0.69] - 2026-06-16

### Fixed
- **Sing-Box Buffer Leak Panic Crash**: Resolved a frequent native Go runtime crash (`panic: leaking buffer` inside the connection copy loop) on newer devices like Galaxy S22 Ultra and Galaxy A13 by recompiling the core `libbox` binary without debug assertions. In release mode, the core now safely releases the leaked buffers instead of triggering fatal panics.

## [1.0.68] - 2026-06-16

### Added
- **Material 3 Expressive Card Styling**: Upgraded all major layout cards (Connection Dashboard, Profile Selection, Available Nodes, Love Quotes, and Logs Console) with premium, dynamic gradient borders that adapt to active Monet accent colors.
- **AMOLED Dark Theme Elevation**: Updated dark theme color mapping in `Theme.kt` to define surfaces with premium, very dark obsidian shades rather than flat pitch-black. This provides clear container hierarchy and elevation shadows while maintaining a pitch-black background on AMOLED screens.

## [1.0.67] - 2026-06-16

### Fixed
- **Startup Resource Resolution Lag**: Replaced reflection-based custom dynamic system color resolution in `Theme.kt` with optimized native `dynamicDarkColorScheme` and `dynamicLightColorScheme` Material 3 APIs to eliminate resource-mapping latency.
- **Refresh Rate Overhead**: Reverted programmatic preferred refresh rate changes in `MainActivity.kt` to prevent layout pass locks and synchronization lags on startup.
- **Off-Main-Thread Subscription Check**: Moved the startup subscription checks and DataStore reads in `MainScreen` entirely to a background thread using `withContext(Dispatchers.IO)`.
- **Optimization Release**: Adjusted CI workflow and build scripts to compile and publish production-optimized Release APKs with full R8 optimization and code shrinking.

## [1.0.66] - 2026-06-16

### Fixed
- **VRR Refresh Rate Lock (Cold Launch Stutter)**: Programmatically requested high refresh rate (120Hz) on application launch inside `MainActivity`. This bypasses variable refresh rate (VRR) display bugs on Samsung/OnePlus/Xiaomi/Pixel devices that lock the window to a low refresh rate (30Hz/60Hz) on cold startup (reverted in v1.0.67).
- **Node List Transition Lag**: Removed expensive per-item coroutines and float animations inside the available nodes list `LazyColumn` to eliminate lag/stutter when drawing the list, regardless of the user's node count.
- **Drawer Version**: Displays version `v1.0.66`.

## [1.0.65] - 2026-06-16

### Fixed
- **Performance / Lag**: Resolved startup stutter and lags by optimizing DataStore flows in `SettingsManager` to ignore redundant duplicate emissions, caching system accent color lookups in `Theme` (bypassing expensive reflection), and moving crash log reading off the main thread to a background coroutine.
- **Design Revert**: Reverted the mesh background, corner-morphing button, rolling speed graph, and glassmorphic cards back to standard lightweight Material 3 style to restore snappy performance and clean interface.
- **Drawer Version**: Correctly displays version `v1.0.65`.

## [1.0.64] - 2026-06-16

### Added
- **Dynamic Background Mesh**: Integrated a custom, slow-animated Canvas mesh background reflecting dynamic system Monet colors that transitions to a breathing central aura during active VPN connections.
- **Corner-Morphing Connect Control**: Connect button morphs shapes fluidly between a circle and a squircle depending on connection state, featuring a neon breathing glow expansion behind it when connected.
- **Real-Time Rolling Speed Graph**: Embedded a real-time cubic-bezier rolling graph displaying network speed trends (download and upload) over the last 20 seconds using Canvas.
- **Glassmorphic Cards & Gradient Borders**: Standardized all cards on a semi-transparent surface style with thin `1.dp` linear gradient outlines that trace the containers with high-density visual depth.
- **Dual-Flavored Release Pipeline**: Updated GitHub CI workflow to build, package, and release both the standard edition (`ExpressiveBox-standard.apk`) and the special edition (`ExpressiveBox-special.apk`) simultaneously.

### Fixed
- **App Menu Version Display**: Bumped the displayed application version to `v1.0.64` in the Drawer layout.

## [1.0.63] - 2026-06-16

### Added
- Created `proguard-rules.pro` configuration for release minification (R8). Safely keeps JNI callback bindings (`libbox`), go-runtime wrappers (`go`), and serialization schemas (`kotlinx.serialization`) intact.

### Fixed
- **Launch Performance Lag**: Resolved reflection-based accent color resource lookup jank by wrapping Monet theme palette derivation in a `remember` block, preventing `resources.getIdentifier` from being called 20+ times on every recomposition.
- **Scroll Frame Drops**: Replaced list auto-scroll coroutine animations (`animateScrollToItem()`) with instant programmatic jumps (`scrollToItem()`) inside the engine log viewer, avoiding continuous layout invalidation when receiving burst logs on connection.
- **Proxy List Scrolling Stutter**: Optimized item stagger animations in the main server listing. If the stagger entrance completes, individual items bypass reading animated progress states and render static draw parameters directly, eliminating recompositions during scrolling.
- **Immersive Edge-to-Edge Navigation**: Replaced the root container safe area/margin padding with a full-screen layout. The settings drawer now slides smoothly from the absolute screen boundaries, and action app bars stretch flush to status and navigation bar areas.
- **App Menu Version Display**: Bumped the displayed application version to `v1.0.63` in the Drawer layout.

### Optimized
- Enabled R8 minification (`isMinifyEnabled = true`) on release configurations for maximum runtime execution speed, smaller APK size, and smoother layout animations.
- Bound debug signature config to release build type to simplify testing of production-optimized builds.

---

## [1.0.62] - 2026-06-15

### Added
- Refactored `WaveVisualizer` in `MainScreen.kt` using `rememberInfiniteTransition()` to shift wave computations entirely to the GPU Draw phase, dropping CPU composition cycles during active animation.
- Moved package manager app scanner queries to `Dispatchers.IO` background thread in `SplitTunnelingScreen.kt` to avoid blocking the Main thread.
- Enabled spring-based slide-and-fade navigation transitions using Compose Navigation 3.
