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

private fun getSystemColor(context: Context, name: String, fallback: Color): Color {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val resId = context.resources.getIdentifier(name, "color", "android")
        if (resId != 0) {
            try {
                return Color(context.getColor(resId))
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    return fallback
}

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

    var colorScheme = when {
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
            // Read accent colors directly from system resources for robust Monet color adaptation
            val primary = getSystemColor(context, "system_accent1_200", DarkPrimary)
            val primaryLight = getSystemColor(context, "system_accent1_600", LightPrimary)
            
            val primaryContainer = getSystemColor(context, "system_accent1_700", DarkPrimaryContainer)
            val primaryContainerLight = getSystemColor(context, "system_accent1_100", LightPrimaryContainer)
            
            val onPrimary = getSystemColor(context, "system_accent1_800", DarkOnPrimary)
            val onPrimaryLight = getSystemColor(context, "system_accent1_0", LightOnPrimary)
            
            val onPrimaryContainer = getSystemColor(context, "system_accent1_100", DarkOnPrimaryContainer)
            val onPrimaryContainerLight = getSystemColor(context, "system_accent1_900", LightOnPrimaryContainer)
            
            val secondary = getSystemColor(context, "system_accent2_200", DarkSecondary)
            val secondaryLight = getSystemColor(context, "system_accent2_600", LightSecondary)
            
            val secondaryContainer = getSystemColor(context, "system_accent2_700", DarkSecondaryContainer)
            val secondaryContainerLight = getSystemColor(context, "system_accent2_100", LightSecondaryContainer)
            
            val onSecondary = getSystemColor(context, "system_accent2_800", DarkOnSecondary)
            val onSecondaryLight = getSystemColor(context, "system_accent2_0", LightOnSecondary)
            
            val onSecondaryContainer = getSystemColor(context, "system_accent2_100", DarkOnSecondaryContainer)
            val onSecondaryContainerLight = getSystemColor(context, "system_accent2_900", LightOnSecondaryContainer)

            val tertiary = getSystemColor(context, "system_accent3_200", DarkTertiary)
            val tertiaryLight = getSystemColor(context, "system_accent3_600", LightTertiary)

            val tertiaryContainer = getSystemColor(context, "system_accent3_700", DarkTertiaryContainer)
            val tertiaryContainerLight = getSystemColor(context, "system_accent3_100", LightTertiaryContainer)

            val onTertiary = getSystemColor(context, "system_accent3_800", DarkOnTertiary)
            val onTertiaryLight = getSystemColor(context, "system_accent3_0", LightOnTertiary)

            val onTertiaryContainer = getSystemColor(context, "system_accent3_100", DarkOnTertiaryContainer)
            val onTertiaryContainerLight = getSystemColor(context, "system_accent3_900", LightOnTertiaryContainer)

            val background = if (darkTheme) Color.Black else getSystemColor(context, "system_neutral1_10", LightBackground)
            val surface = if (darkTheme) Color.Black else getSystemColor(context, "system_neutral1_10", LightSurface)
            
            val onBackground = getSystemColor(context, "system_neutral1_100", DarkOnBackground)
            val onBackgroundLight = getSystemColor(context, "system_neutral1_900", LightOnBackground)
            
            val onSurface = getSystemColor(context, "system_neutral1_100", DarkOnSurface)
            val onSurfaceLight = getSystemColor(context, "system_neutral1_900", LightOnSurface)
            
            val surfaceVariant = if (darkTheme) Color.Black else getSystemColor(context, "system_neutral2_100", LightSurfaceVariant)
            val surfaceVariantLight = getSystemColor(context, "system_neutral2_100", LightSurfaceVariant)
            
            val onSurfaceVariant = getSystemColor(context, "system_neutral2_200", DarkOnSurfaceVariant)
            val onSurfaceVariantLight = getSystemColor(context, "system_neutral2_700", LightOnSurfaceVariant)
            
            val outline = getSystemColor(context, "system_neutral2_400", DarkOutline)
            val outlineLight = getSystemColor(context, "system_neutral2_500", LightOutline)

            if (darkTheme) {
                darkColorScheme(
                    primary = primary,
                    onPrimary = onPrimary,
                    primaryContainer = primaryContainer,
                    onPrimaryContainer = onPrimaryContainer,
                    secondary = secondary,
                    onSecondary = onSecondary,
                    secondaryContainer = secondaryContainer,
                    onSecondaryContainer = onSecondaryContainer,
                    tertiary = tertiary,
                    onTertiary = onTertiary,
                    tertiaryContainer = tertiaryContainer,
                    onTertiaryContainer = onTertiaryContainer,
                    background = background,
                    onBackground = onBackground,
                    surface = surface,
                    onSurface = onSurface,
                    surfaceVariant = surfaceVariant,
                    onSurfaceVariant = onSurfaceVariant,
                    outline = outline
                )
            } else {
                lightColorScheme(
                    primary = primaryLight,
                    onPrimary = onPrimaryLight,
                    primaryContainer = primaryContainerLight,
                    onPrimaryContainer = onPrimaryContainerLight,
                    secondary = secondaryLight,
                    onSecondary = onSecondaryLight,
                    secondaryContainer = secondaryContainerLight,
                    onSecondaryContainer = onSecondaryContainerLight,
                    tertiary = tertiaryLight,
                    onTertiary = onTertiaryLight,
                    tertiaryContainer = tertiaryContainerLight,
                    onTertiaryContainer = onTertiaryContainerLight,
                    background = background,
                    onBackground = onBackgroundLight,
                    surface = surface,
                    onSurface = onSurfaceLight,
                    surfaceVariant = surfaceVariantLight,
                    onSurfaceVariant = onSurfaceVariantLight,
                    outline = outlineLight
                )
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Removed pure black override to allow M3 Expressive container colors to show naturally
    // if (darkTheme) {
    //     colorScheme = colorScheme.copy(
    //         background = Color.Black,
    //         surface = Color.Black,
    //         surfaceContainer = Color.Black,
    //         surfaceContainerHigh = Color.Black,
    //         surfaceContainerHighest = Color.Black,
    //         surfaceContainerLow = Color.Black,
    //         surfaceContainerLowest = Color.Black,
    //         surfaceVariant = Color.Black
    //     )
    // }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
