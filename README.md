# ExpressiveBox

ExpressiveBox is a modern, high-performance VPN client for Android built entirely with **Jetpack Compose** and **Material Design 3**. It offers secure routing, modular connection profiles, and deep integration with the Android system's native VPN frameworks.

> [!NOTE]
> **Project Status**: ExpressiveBox is currently in its **active testing stage**. It supports standard subscription/profile parsing, custom DNS rules, split tunneling, and core connectivity using modern secure protocols (VLESS, Trojan, Shadowsocks, SOCKS5, HTTP, and HTTPS).
> 
> * **Persian (Farsi) language support is coming soon!**

---

## Key Features

### 🌟 Material 3 Expressive UI & Aesthetics
* **Asymmetric Corner Design**: Implements the Material 3 Expressive (MD3E) styling guide using dynamic asymmetric rounded shapes (`ExpressiveCardShape`, `ExpressiveButtonShape`, `ExpressiveChipShape`) for all cards, dialogs, buttons, text fields, and list items.
* **Animated Wavy Indicators**: Features a custom-rendered `CircularWavyProgressIndicator` that animates smoothly during connection/disconnection states.
* **Interactive Wave Visualizer**: Includes a real-time `Canvas`-based wave visualizer that flows dynamically when the VPN connection is active.
* **Tactile Press-Scale Effects**: Custom physics-based scale-press feedback on all interactive components for a premium feel.

### 🔌 Multi-Protocol & Manual Configuration
* **Extended Protocol Support**: Full routing capabilities for VLESS, Trojan, Shadowsocks, SOCKS5, HTTP, and HTTPS (with TLS/SNI injection).
* **Manual Node Creator & Form Editor**: Create nodes from scratch or edit existing ones using a comprehensive form editor (supporting protocol, remark, host, port, credentials/UUID, TLS toggles, and SNI names) or a raw config editor.
* **QR Code Scanner**: Fast configuration import by scanning QR codes using the device camera (powered by Android CameraX and Google ML Kit).

### ⚡ Traffic Stats & Routing Options
* **Real Traffic Speed Monitoring**: Displays live upload and download rates on the dashboard computed every second using system-level `android.net.TrafficStats`.
* **Automated Iran Routing**: Built-in rules for automatic identification of domestic Iranian domains and IP ranges (`geoip-ir`/`geosite-ir`), routing them directly (bypassing the VPN) to guarantee optimal speeds for domestic banking and services.
* **Bypass LAN**: Toggle option to bypass local area network traffic (private subnets like `192.168.x.x`, `10.x.x.x`), keeping local printer/router connections direct.

### 🔄 Subscriptions & Updates
* **Scheduled Auto-Updates**: Auto-update subscription lists silently on app startup, daily, or weekly, with manual pull-to-refresh buttons.
* **Smart Active Profile Selection**: Automatically switches to the first available backup node of a subscription if the current active server is modified or deleted during an update.

---

## Technical Specifications

* **Minimum SDK**: Android 7.0 (API Level 24)
* **Target SDK**: Android 16 (API Level 36)
* **Compile SDK**: Android 16 (API Level 37)
* **Supported ABI / Architecture**: `arm64-v8a` (64-bit ARM devices only for early testing stage)
* **UI Toolkit**: Jetpack Compose (Material 3 Expressive)
* **Language Toolchain**: Kotlin 2.x, Java 17

---

## Getting Started

### Prerequisites

* Android Studio (Ladybug or newer)
* Android SDK (API 34+)
* JDK 17

### Building the Project

1. Clone the repository:
   ```bash
   git clone https://github.com/Rabkaps/ExpressiveBox.git
   cd ExpressiveBox
   ```

2. Build the debug APK for the **Standard** flavor:
   ```bash
   ./gradlew assembleStandardDebug
   ```

3. Install the application on a connected device:
   ```bash
   ./gradlew installStandardDebug
   ```

---

## Development and Contributions

We follow the standard Git branching model. Please submit all feature additions or bug fixes via Pull Requests. Make sure to run static analysis and unit tests before committing changes.
