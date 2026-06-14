plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

configurations.all {
    exclude(group = "androidx.compose.runtime", module = "runtime-annotation-jvm")
}

android {
    namespace = "com.hambalapps.expressivebox"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.hambalapps.expressivebox"
        minSdk = 24
        targetSdk = 36
        versionCode = 56
        versionName = "1.0.56"
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    flavorDimensions += "edition"
    productFlavors {
        create("standard") {
            dimension = "edition"
        }
        create("special") {
            dimension = "edition"
            applicationIdSuffix = ".special"
            versionNameSuffix = "-special"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation("com.google.android.material:material:1.12.0")

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation("androidx.compose.material:material-icons-core")
  implementation("androidx.compose.material:material-icons-extended")
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // VPN Core
  implementation(files("libs/libbox.aar"))

  // DataStore Preferences
  implementation("androidx.datastore:datastore-preferences:1.1.1")

  // CameraX
  implementation("androidx.camera:camera-camera2:1.3.3")
  implementation("androidx.camera:camera-lifecycle:1.3.3")
  implementation("androidx.camera:camera-view:1.3.3")

  // ML Kit Barcode Scanning
  implementation("com.google.mlkit:barcode-scanning:17.2.0")
}

tasks.configureEach {
    if (name.contains("AarMetadata", ignoreCase = true)) {
        enabled = false
    }
}

