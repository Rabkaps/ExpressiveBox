package com.hambalapps.expressivebox.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.hambalapps.expressivebox.Config
import com.hambalapps.expressivebox.data.SettingsManager


private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = ErrorRed,
    onError = OnErrorRed,
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = ErrorRed,
    onError = OnErrorRed,
    outline = LightOutline
)

// Cherry Blossom Custom Palette
private val CherryLightColorScheme = lightColorScheme(
    primary = Color(0xFFD03A60),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3F0015),
    secondary = Color(0xFF7D5260),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9E3),
    background = Color(0xFFFFF8F9),
    surface = Color(0xFFFFF8F9),
    onBackground = Color(0xFF25191C),
    onSurface = Color(0xFF25191C),
    outline = Color(0xFF857376)
)

private val CherryDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB0C8),
    onPrimary = Color(0xFF5F0024),
    primaryContainer = Color(0xFF80083B),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFE8B9C7),
    onSecondary = Color(0xFF462631),
    secondaryContainer = Color(0xFF5E3C49),
    background = Color(0xFF1F1A1B),
    surface = Color(0xFF1F1A1B),
    onBackground = Color(0xFFEAE0E1),
    onSurface = Color(0xFFEAE0E1),
    outline = Color(0xFF9F8C90)
)

// Lavender Dreams Custom Palette
private val LavenderLightColorScheme = lightColorScheme(
    primary = Color(0xFF624FBE),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6DEFF),
    onPrimaryContainer = Color(0xFF1C0062),
    secondary = Color(0xFF605B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6E0F9),
    background = Color(0xFFFAF8FF),
    surface = Color(0xFFFAF8FF),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    outline = Color(0xFF7B758F)
)

private val LavenderDarkColorScheme = darkColorScheme(
    primary = Color(0xFFC8BFFF),
    onPrimary = Color(0xFF321190),
    primaryContainer = Color(0xFF4A35A5),
    onPrimaryContainer = Color(0xFFE6DEFF),
    secondary = Color(0xFFC9C4DC),
    onSecondary = Color(0xFF322E41),
    secondaryContainer = Color(0xFF484459),
    background = Color(0xFF141318),
    surface = Color(0xFF141318),
    onBackground = Color(0xFFE6E1E6),
    onSurface = Color(0xFFE6E1E6),
    outline = Color(0xFF938F9F)
)

// Minimalist Rose Gold Custom Palette
private val RoseGoldLightColorScheme = lightColorScheme(
    primary = Color(0xFF944B56),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9DD),
    onPrimaryContainer = Color(0xFF3C0715),
    secondary = Color(0xFF775357),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9DC),
    background = Color(0xFFFFF8F8),
    surface = Color(0xFFFFF8F8),
    onBackground = Color(0xFF22191A),
    onSurface = Color(0xFF22191A),
    outline = Color(0xFF857375)
)

private val RoseGoldDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB2BB),
    onPrimary = Color(0xFF5A1D29),
    primaryContainer = Color(0xFF77343F),
    onPrimaryContainer = Color(0xFFFFD9DD),
    secondary = Color(0xFFE5BCC0),
    onSecondary = Color(0xFF44292C),
    secondaryContainer = Color(0xFF5D3F42),
    background = Color(0xFF201A1B),
    surface = Color(0xFF201A1B),
    onBackground = Color(0xFFECE0E1),
    onSurface = Color(0xFFECE0E1),
    outline = Color(0xFF9F8C8E)
)

@Composable
fun ExpressiveBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Enabled by default for native Monet accent coloring
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    
    // Check if it's the special edition and fetch setting
    val specialTheme = if (Config.IS_SPECIAL) {
        settingsManager.specialTheme.collectAsState(initial = "cherry_blossom").value
    } else {
        "none"
    }

    val colorScheme = remember(darkTheme, dynamicColor, specialTheme) {
        val baseScheme = when {
            Config.IS_SPECIAL -> {
                when (specialTheme) {
                    "lavender_dreams" -> {
                        if (darkTheme) LavenderDarkColorScheme else LavenderLightColorScheme
                    }
                    "rose_gold" -> {
                        if (darkTheme) RoseGoldDarkColorScheme else RoseGoldLightColorScheme
                    }
                    else -> { // cherry_blossom or fallback
                        if (darkTheme) CherryDarkColorScheme else CherryLightColorScheme
                    }
                }
            }
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

        if (darkTheme) {
            baseScheme.copy(
                background = Color.Black,
                surface = Color(0xFF0C0C0E),
                surfaceVariant = Color(0xFF131316),
                surfaceContainer = Color(0xFF0F0F12),
                surfaceContainerHigh = Color(0xFF161619),
                surfaceContainerHighest = Color(0xFF1D1D22),
                surfaceContainerLow = Color(0xFF09090B),
                surfaceContainerLowest = Color(0xFF050507)
            )
        } else {
            baseScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
