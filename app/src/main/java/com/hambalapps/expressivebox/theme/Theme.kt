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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import android.app.WallpaperManager
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils


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

// Midnight Blue Custom Palette
private val MidnightLightColorScheme = lightColorScheme(
    primary = Color(0xFF1B365D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E2EC),
    onPrimaryContainer = Color(0xFF0A1D37),
    secondary = Color(0xFF486581),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF0F4F8),
    background = Color(0xFFF0F4F8),
    surface = Color(0xFFF0F4F8),
    onBackground = Color(0xFF102A43),
    onSurface = Color(0xFF102A43),
    outline = Color(0xFF627D98)
)

private val MidnightDarkColorScheme = darkColorScheme(
    primary = Color(0xFF9DB2C6),
    onPrimary = Color(0xFF0A1D37),
    primaryContainer = Color(0xFF1B365D),
    onPrimaryContainer = Color(0xFFD9E2EC),
    secondary = Color(0xFF627D98),
    onSecondary = Color(0xFF102A43),
    secondaryContainer = Color(0xFF243B53),
    background = Color(0xFF0F1E36),
    surface = Color(0xFF0F1E36),
    onBackground = Color(0xFFF0F4F8),
    onSurface = Color(0xFFF0F4F8),
    outline = Color(0xFF486581)
)

// Forest Green Custom Palette
private val ForestLightColorScheme = lightColorScheme(
    primary = Color(0xFF2E6F40),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4EDDA),
    onPrimaryContainer = Color(0xFF0A3013),
    secondary = Color(0xFF5A7361),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2EBE4),
    background = Color(0xFFF4FAF6),
    surface = Color(0xFFF4FAF6),
    onBackground = Color(0xFF112215),
    onSurface = Color(0xFF112215),
    outline = Color(0xFF708577)
)

private val ForestDarkColorScheme = darkColorScheme(
    primary = Color(0xFF90D5A1),
    onPrimary = Color(0xFF0B3A1C),
    primaryContainer = Color(0xFF2E6F40),
    onPrimaryContainer = Color(0xFFD4EDDA),
    secondary = Color(0xFFB1CBB7),
    onSecondary = Color(0xFF1C3423),
    secondaryContainer = Color(0xFF3A4E40),
    background = Color(0xFF111E15),
    surface = Color(0xFF111E15),
    onBackground = Color(0xFFE2EBE4),
    onSurface = Color(0xFFE2EBE4),
    outline = Color(0xFF5A7361)
)

// Sunset Orange Custom Palette
private val SunsetLightColorScheme = lightColorScheme(
    primary = Color(0xFFD35400),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFADBD8),
    onPrimaryContainer = Color(0xFF4A1504),
    secondary = Color(0xFF9E5A42),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF9EBEA),
    background = Color(0xFFFDF6F0),
    surface = Color(0xFFFDF6F0),
    onBackground = Color(0xFF2C130B),
    onSurface = Color(0xFF2C130B),
    outline = Color(0xFFB57C68)
)

private val SunsetDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFAB91),
    onPrimary = Color(0xFF5D2710),
    primaryContainer = Color(0xFFD35400),
    onPrimaryContainer = Color(0xFFFADBD8),
    secondary = Color(0xFFD7CCC8),
    onSecondary = Color(0xFF3E2723),
    secondaryContainer = Color(0xFF5D4037),
    background = Color(0xFF211511),
    surface = Color(0xFF211511),
    onBackground = Color(0xFFF5EEEE),
    onSurface = Color(0xFFF5EEEE),
    outline = Color(0xFF8D6E63)
)

// Ocean Teal Custom Palette
private val TealLightColorScheme = lightColorScheme(
    primary = Color(0xFF007A78),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC6ECEB),
    onPrimaryContainer = Color(0xFF002020),
    secondary = Color(0xFF4A6362),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E7),
    background = Color(0xFFF2FAF9),
    surface = Color(0xFFF2FAF9),
    onBackground = Color(0xFF051F1F),
    onSurface = Color(0xFF051F1F),
    outline = Color(0xFF6F8483)
)

private val TealDarkColorScheme = darkColorScheme(
    primary = Color(0xFF80CBC4),
    onPrimary = Color(0xFF003735),
    primaryContainer = Color(0xFF00504E),
    onPrimaryContainer = Color(0xFFC6ECEB),
    secondary = Color(0xFFB2DFDB),
    onSecondary = Color(0xFF1E3533),
    secondaryContainer = Color(0xFF334B49),
    background = Color(0xFF0E1A1A),
    surface = Color(0xFF0E1A1A),
    onBackground = Color(0xFFE0F2F1),
    onSurface = Color(0xFFE0F2F1),
    outline = Color(0xFF4A6362)
)

// Royal Amethyst Custom Palette
private val AmethystLightColorScheme = lightColorScheme(
    primary = Color(0xFF6F35A5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF1E6FF),
    onPrimaryContainer = Color(0xFF2D005D),
    secondary = Color(0xFF665A73),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDE6F2),
    background = Color(0xFFFAF8FD),
    surface = Color(0xFFFAF8FD),
    onBackground = Color(0xFF1C1A22),
    onSurface = Color(0xFF1C1A22),
    outline = Color(0xFF81758F)
)

private val AmethystDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD7BDE2),
    onPrimary = Color(0xFF4A157D),
    primaryContainer = Color(0xFF6F35A5),
    onPrimaryContainer = Color(0xFFF1E6FF),
    secondary = Color(0xFFD2C4D9),
    onSecondary = Color(0xFF382E43),
    secondaryContainer = Color(0xFF4F435A),
    background = Color(0xFF16121D),
    surface = Color(0xFF16121D),
    onBackground = Color(0xFFEDE8F2),
    onSurface = Color(0xFFEDE8F2),
    outline = Color(0xFF665A73)
)

// Nordic Slate Custom Palette
private val SlateLightColorScheme = lightColorScheme(
    primary = Color(0xFF4F6272),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7E2EC),
    onPrimaryContainer = Color(0xFF0F1E2A),
    secondary = Color(0xFF5B6975),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5EDF4),
    background = Color(0xFFF2F6F9),
    surface = Color(0xFFF2F6F9),
    onBackground = Color(0xFF172026),
    onSurface = Color(0xFF172026),
    outline = Color(0xFF75828D)
)

private val SlateDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA5B8C8),
    onPrimary = Color(0xFF1C2D3C),
    primaryContainer = Color(0xFF384755),
    onPrimaryContainer = Color(0xFFD7E2EC),
    secondary = Color(0xFFBAC4CE),
    onSecondary = Color(0xFF24303B),
    secondaryContainer = Color(0xFF3B4854),
    background = Color(0xFF14191E),
    surface = Color(0xFF14191E),
    onBackground = Color(0xFFEBF0F5),
    onSurface = Color(0xFFEBF0F5),
    outline = Color(0xFF5B6975)
)

private fun Color.isMuted(): Boolean {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val saturation = if (max == 0f) 0f else (max - min) / max
    return saturation < 0.22f
}

private fun Color.isReallyGrayscale(): Boolean {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val saturation = if (max == 0f) 0f else (max - min) / max
    return saturation < 0.08f
}

private fun Color.harmonizeWith(brandColor: Color, ratio: Float = 0.35f): Color {
    return Color(
        red = this.red * (1f - ratio) + brandColor.red * ratio,
        green = this.green * (1f - ratio) + brandColor.green * ratio,
        blue = this.blue * (1f - ratio) + brandColor.blue * ratio,
        alpha = this.alpha
    )
}

private fun tintColor(baseColor: Color, blendColor: Color, ratio: Float): Color {
    return Color(
        red = baseColor.red * (1f - ratio) + blendColor.red * ratio,
        green = baseColor.green * (1f - ratio) + blendColor.green * ratio,
        blue = baseColor.blue * (1f - ratio) + blendColor.blue * ratio,
        alpha = baseColor.alpha
    )
}

private fun getWallpaperSeedColor(context: Context): Color {
    // 1. Try to extract from home screen wallpaper colors first (Android 8.1+)
    try {
        val wallpaperManager = WallpaperManager.getInstance(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val wallpaperColors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            if (wallpaperColors != null) {
                val extracted = Color(wallpaperColors.primaryColor.toArgb())
                if (!extracted.isReallyGrayscale()) {
                    return extracted
                }
            }
        }
    } catch (e: Exception) {
        // Safe catch
    }

    // 2. Fallback to retrieving the actual Monet accent color from system resources (Android 12+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            val sysColor = context.resources.getColor(android.R.color.system_accent1_500, context.theme)
            val extracted = Color(sysColor)
            if (!extracted.isReallyGrayscale()) {
                return extracted
            }
        } catch (e: Exception) {
            // Safe catch
        }
    }

    // 3. Ultimate Fallback brand color (Lavender for Standard, Cherry for Special)
    return if (Config.IS_SPECIAL) Color(0xFFD03A60) else Color(0xFF624FBE)
}

private fun generateColorSchemeFromSeed(seed: Color, isDark: Boolean): androidx.compose.material3.ColorScheme {
    val seedArgb = seed.toArgb()
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seedArgb, hsl)
    
    val hue = hsl[0]
    // Coerce saturation to a minimum of 0.48 to ensure a vibrant palette even if the wallpaper is desaturated/grayscale
    val saturation = hsl[1].coerceAtLeast(0.48f)
    
    // Helper to build color from HSL
    fun colorFromHsl(h: Float, s: Float, l: Float): Color {
        val rgb = ColorUtils.HSLToColor(floatArrayOf(h, s, l))
        return Color(rgb)
    }
    
    return if (isDark) {
        // Dark Mode Color Scheme
        darkColorScheme(
            primary = colorFromHsl(hue, saturation.coerceAtMost(0.6f), 0.8f),
            onPrimary = colorFromHsl(hue, saturation.coerceAtMost(0.4f), 0.2f),
            primaryContainer = colorFromHsl(hue, saturation.coerceAtMost(0.5f), 0.3f),
            onPrimaryContainer = colorFromHsl(hue, saturation.coerceAtMost(0.3f), 0.9f),
            
            secondary = colorFromHsl((hue + 15f) % 360f, saturation.coerceAtMost(0.4f), 0.7f),
            onSecondary = colorFromHsl((hue + 15f) % 360f, saturation.coerceAtMost(0.3f), 0.2f),
            secondaryContainer = colorFromHsl((hue + 15f) % 360f, saturation.coerceAtMost(0.3f), 0.25f),
            onSecondaryContainer = colorFromHsl((hue + 15f) % 360f, saturation.coerceAtMost(0.2f), 0.85f),
            
            tertiary = colorFromHsl((hue + 60f) % 360f, saturation.coerceAtMost(0.5f), 0.75f),
            onTertiary = colorFromHsl((hue + 60f) % 360f, saturation.coerceAtMost(0.4f), 0.2f),
            tertiaryContainer = colorFromHsl((hue + 60f) % 360f, saturation.coerceAtMost(0.4f), 0.25f),
            onTertiaryContainer = colorFromHsl((hue + 60f) % 360f, saturation.coerceAtMost(0.3f), 0.85f),
            
            background = Color.Black,
            onBackground = Color(0xFFE6E1E5),
            surface = Color.Black,
            onSurface = Color(0xFFE6E1E5),
            surfaceVariant = colorFromHsl(hue, saturation.coerceAtMost(0.2f), 0.12f),
            onSurfaceVariant = Color(0xFFCAC4D0),
            outline = Color(0xFF938F99)
        )
    } else {
        // Light Mode Color Scheme
        lightColorScheme(
            primary = colorFromHsl(hue, saturation.coerceAtLeast(0.4f), 0.4f),
            onPrimary = Color.White,
            primaryContainer = colorFromHsl(hue, saturation.coerceAtMost(0.3f), 0.9f),
            onPrimaryContainer = colorFromHsl(hue, saturation.coerceAtLeast(0.5f), 0.15f),
            
            secondary = colorFromHsl((hue + 15f) % 360f, saturation.coerceAtMost(0.3f), 0.45f),
            onSecondary = Color.White,
            secondaryContainer = colorFromHsl((hue + 15f) % 360f, saturation.coerceAtMost(0.2f), 0.92f),
            onSecondaryContainer = colorFromHsl((hue + 15f) % 360f, saturation.coerceAtLeast(0.4f), 0.2f),
            
            tertiary = colorFromHsl((hue + 60f) % 360f, saturation.coerceAtMost(0.4f), 0.45f),
            onTertiary = Color.White,
            tertiaryContainer = colorFromHsl((hue + 60f) % 360f, saturation.coerceAtMost(0.2f), 0.92f),
            onTertiaryContainer = colorFromHsl((hue + 60f) % 360f, saturation.coerceAtLeast(0.4f), 0.2f),
            
            background = colorFromHsl(hue, saturation.coerceAtMost(0.1f), 0.98f),
            onBackground = Color(0xFF1D1B20),
            surface = colorFromHsl(hue, saturation.coerceAtMost(0.1f), 0.98f),
            onSurface = Color(0xFF1D1B20),
            surfaceVariant = colorFromHsl(hue, saturation.coerceAtMost(0.15f), 0.9f),
            onSurfaceVariant = Color(0xFF49454F),
            outline = Color(0xFF79747E)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Enabled by default for native Monet accent coloring
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    
    // Check settings theme preference (defaulting to cherry_blossom for Special, dynamic for Standard)
    val defaultThemeKey = if (Config.IS_SPECIAL) "cherry_blossom" else "dynamic"
    val specialTheme = settingsManager.specialTheme.collectAsState(initial = defaultThemeKey).value
    val themeMode = settingsManager.themeMode.collectAsState(initial = "system").value
    val cardStyle = settingsManager.cardStyle.collectAsState(initial = "glass").value

    val isDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val defaultSeed = remember { if (Config.IS_SPECIAL) Color(0xFFD03A60) else Color(0xFF624FBE) }
    val wallpaperSeedColorState = remember { androidx.compose.runtime.mutableStateOf(defaultSeed) }
    androidx.compose.runtime.LaunchedEffect(context) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val seed = getWallpaperSeedColor(context)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                wallpaperSeedColorState.value = seed
            }
        }
    }

    val colorScheme = remember(isDark, dynamicColor, specialTheme, cardStyle, wallpaperSeedColorState.value) {
        val isDynamic = specialTheme == "dynamic" && dynamicColor
        val baseScheme = when {
            isDynamic -> {
                generateColorSchemeFromSeed(wallpaperSeedColorState.value, isDark)
            }
            else -> {
                when (specialTheme) {
                    "lavender_dreams" -> if (isDark) LavenderDarkColorScheme else LavenderLightColorScheme
                    "rose_gold" -> if (isDark) RoseGoldDarkColorScheme else RoseGoldLightColorScheme
                    "cherry_blossom" -> if (isDark) CherryDarkColorScheme else CherryLightColorScheme
                    "midnight_blue" -> if (isDark) MidnightDarkColorScheme else MidnightLightColorScheme
                    "forest_green" -> if (isDark) ForestDarkColorScheme else ForestLightColorScheme
                    "sunset_orange" -> if (isDark) SunsetDarkColorScheme else SunsetLightColorScheme
                    "ocean_teal" -> if (isDark) TealDarkColorScheme else TealLightColorScheme
                    "royal_amethyst" -> if (isDark) AmethystDarkColorScheme else AmethystLightColorScheme
                    "nordic_slate" -> if (isDark) SlateDarkColorScheme else SlateLightColorScheme
                    else -> if (isDark) CherryDarkColorScheme else CherryLightColorScheme
                }
            }
        }

        // Material Expressive Color & Surface Strategy
        if (isDark) {
            val bgRatio = if (cardStyle == "pastel") 0.06f else 0.0f
            val svRatio = if (cardStyle == "pastel") 0.20f else 0.12f
            val sclowestRatio = if (cardStyle == "pastel") 0.04f else 0.0f
            val sclowRatio = if (cardStyle == "pastel") 0.10f else 0.06f
            val scRatio = if (cardStyle == "pastel") 0.16f else 0.10f
            val schighRatio = if (cardStyle == "pastel") 0.24f else 0.16f
            val schighestRatio = if (cardStyle == "pastel") 0.32f else 0.22f
            
            baseScheme.copy(
                background = tintColor(Color.Black, baseScheme.primary, ratio = bgRatio),
                surface = tintColor(Color.Black, baseScheme.primary, ratio = bgRatio),
                surfaceVariant = tintColor(Color.Black, baseScheme.primary, ratio = svRatio),
                surfaceContainerLowest = tintColor(Color.Black, baseScheme.primary, ratio = sclowestRatio),
                surfaceContainerLow = tintColor(Color.Black, baseScheme.primary, ratio = sclowRatio),
                surfaceContainer = tintColor(Color.Black, baseScheme.primary, ratio = scRatio),
                surfaceContainerHigh = tintColor(Color.Black, baseScheme.primary, ratio = schighRatio),
                surfaceContainerHighest = tintColor(Color.Black, baseScheme.primary, ratio = schighestRatio)
            )
        } else {
            val bgRatio = if (cardStyle == "pastel") 0.12f else 0.05f
            val surfaceRatio = if (cardStyle == "pastel") 0.12f else 0.05f
            val svRatio = if (cardStyle == "pastel") 0.16f else 0.08f
            val sclowestRatio = if (cardStyle == "pastel") 0.06f else 0.02f
            val sclowRatio = if (cardStyle == "pastel") 0.10f else 0.06f
            val scRatio = if (cardStyle == "pastel") 0.14f else 0.09f
            val schighRatio = if (cardStyle == "pastel") 0.18f else 0.12f
            val schighestRatio = if (cardStyle == "pastel") 0.24f else 0.16f
            
            baseScheme.copy(
                background = tintColor(Color.White, baseScheme.primary, ratio = bgRatio),
                surface = tintColor(Color.White, baseScheme.primary, ratio = surfaceRatio),
                surfaceVariant = tintColor(Color.White, baseScheme.primary, ratio = svRatio),
                surfaceContainerLowest = tintColor(Color.White, baseScheme.primary, ratio = sclowestRatio),
                surfaceContainerLow = tintColor(Color.White, baseScheme.primary, ratio = sclowRatio),
                surfaceContainer = tintColor(Color.White, baseScheme.primary, ratio = scRatio),
                surfaceContainerHigh = tintColor(Color.White, baseScheme.primary, ratio = schighRatio),
                surfaceContainerHighest = tintColor(Color.White, baseScheme.primary, ratio = schighestRatio)
            )
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography,
        motionScheme = remember { MotionScheme.expressive() },
        content = content
    )
}
