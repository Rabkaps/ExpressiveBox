### What's New in v1.0.64

This release includes the highly-anticipated premium visual redesign, transforming the dashboard layout with high-end Material 3 Expressive (MD3E) aesthetics.

#### Redesign Highlights
- **Dynamic Background Mesh**: Integrated a custom, slow-animated Canvas mesh background reflecting dynamic system Monet colors that transitions to a breathing central aura during active VPN connections.
- **Corner-Morphing Connect Control**: Connect button morphs shapes fluidly between a circle and a squircle depending on connection state, featuring a neon breathing glow expansion behind it when connected.
- **Real-Time Rolling Speed Graph**: Embedded a real-time cubic-bezier rolling graph displaying network speed trends (download and upload) over the last 20 seconds using Canvas.
- **Glassmorphic Cards & Gradient Borders**: Standardized all cards on a semi-transparent surface style with thin `1.dp` linear gradient outlines that trace the containers with high-density visual depth.
- **Dual-Flavored Release Pipeline**: Updated GitHub CI workflow to build, package, and release both the standard edition (`ExpressiveBox-standard.apk`) and the special edition (`ExpressiveBox-special.apk`) simultaneously.

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
