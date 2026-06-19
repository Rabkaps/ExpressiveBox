package com.hambalapps.expressivebox

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.hambalapps.expressivebox.theme.ExpressiveBoxTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.hambalapps.expressivebox.vpn.VpnServiceWrapper
import com.hambalapps.expressivebox.data.SettingsManager
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.io.File

class MainActivity : ComponentActivity() {

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
    if (isGranted) {
      VpnServiceWrapper.log("Notification permission granted")
    } else {
      VpnServiceWrapper.log("Notification permission denied")
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    DynamicColors.applyToActivityIfAvailable(this)
    super.onCreate(savedInstanceState)


    // Uncaught exception handler to log JVM crashes
    val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      try {
        val crashFile = File(cacheDir, "crash.log")
        crashFile.writeText("Crash in thread ${thread.name}:\n" + throwable.stackTraceToString())
      } catch (e: Exception) {
        // Ignore
      }
      originalHandler?.uncaughtException(thread, throwable)
    }

    // Check for previous crash log and load it on background thread
    lifecycleScope.launch(Dispatchers.IO) {
      try {
        VpnServiceWrapper.checkAndLoadCrashLog(applicationContext)
      } catch (e: Exception) {
        // Ignore
      }
    }

    // Request notification permission on Android 13+ (API 33+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }

    enableEdgeToEdge()
    setContent {
      @OptIn(ExperimentalMaterial3ExpressiveApi::class)
      ExpressiveBoxTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}

