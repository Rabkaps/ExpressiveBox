### What's New in v1.0.67

This release introduces critical performance optimizations to resolve startup stuttering and lags.

#### Performance Improvements
- **Native Dynamic Colors**: Replaced slow, custom reflection-based system resource lookups in `Theme` with native Android Material 3 Dynamic Color APIs, accelerating initial composition.
- **Off-Main-Thread Startup Checks**: Moved settings reads and subscription updates in `MainScreen` entirely to background IO threads to keep the main UI thread free of disk/network wait states on launch.
- **Removed Display Sync Pass**: Reverted programmatic preferred refresh rate adjustments on startup to prevent window layout pass freezes.
- **Production R8 Optimization**: Pipeline now builds optimized Release APKs with full code shrinking, optimizations, and baseline profiles.

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
