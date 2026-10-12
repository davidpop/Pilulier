package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = SageGreenPrimary,
    onPrimary = SageGreenOnPrimary,
    primaryContainer = SageGreenContainer,
    onPrimaryContainer = SageGreenOnContainer,
    secondary = SoftBlueSecondary,
    onSecondary = SoftBlueOnSecondary,
    secondaryContainer = SoftBlueContainer,
    onSecondaryContainer = SoftBlueOnContainer,
    tertiary = SoftAmberTertiary,
    onTertiary = SageGreenOnPrimary,
    tertiaryContainer = SoftAmberContainer,
    onTertiaryContainer = SoftAmberOnContainer,
    background = OffWhiteBackground,
    onBackground = DarkCharcoalText,
    surface = PureWhiteSurface,
    onSurface = DarkCharcoalText,
    surfaceVariant = SageSurfaceVariant,
    onSurfaceVariant = MediumCharcoalText,
    outline = SageOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkSagePrimary,
    onPrimary = DarkSageOnPrimary,
    primaryContainer = DarkSageContainer,
    onPrimaryContainer = DarkSageOnContainer,
    secondary = DarkSoftBlueSecondary,
    onSecondary = DarkSoftBlueOnSecondary,
    secondaryContainer = DarkSoftBlueContainer,
    onSecondaryContainer = DarkSoftBlueOnContainer,
    tertiary = DarkSoftBlueSecondary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnBackground,
    outline = SageOutline
)

@Composable
fun PilulierTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep bespoke sage-green palette by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
