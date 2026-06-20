package com.hambalapps.expressivebox.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import com.hambalapps.expressivebox.theme.ExpressiveBoxTheme
import com.hambalapps.expressivebox.desktop.ui.MainScreen

fun main() = application {
    val windowState = rememberWindowState(
        width = 1000.dp,
        height = 700.dp
    )
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "ExpressiveBox Desktop"
    ) {
        ExpressiveBoxTheme {
            MainScreen()
        }
    }
}
