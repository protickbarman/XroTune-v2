package com.xrotune.app.ui.theme

import android.os.Build
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xrotune.app.ui.components.NoRippleIndication

private val DarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF351B5E),
    onPrimaryContainer = VioletGlow,
    secondary = CoralPulse,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF471828),
    onSecondaryContainer = Color(0xFFFFB3C0),
    tertiary = MintSignal,
    onTertiary = Color.Black,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = CharcoalSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = PlumSlateSurface,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color.Transparent,
    error = ErrorCrimson,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF7C3AED),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF3B0764),
    secondary = CoralPulse,
    onSecondary = Color.White,
    tertiary = MintSignal,
    background = LightBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color.Transparent,
    error = ErrorCrimson,
    onError = Color.White
)

val XroTuneShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun XroTuneTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalIndication provides NoRippleIndication) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = XroTuneTypography,
            shapes = XroTuneShapes,
            content = content
        )
    }
}
