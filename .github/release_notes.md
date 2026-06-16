### What's New in v1.0.69

This release fixes a critical native Go library crash that occurred frequently during network state changes or high load on newer devices.

#### Fixes
- **Resolved Buffer Leak Panic Crash**: Fixed a native crash (`panic: leaking buffer`) in the `packetConnectionCopy` routine within the `libbox` core. Recompiled the Go core module without debug assertions, allowing it to gracefully release buffers under low-memory or high-latency network conditions (such as switching from Wi-Fi to mobile data) rather than terminating the VPN service.

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
