package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZeecrownLightColorScheme = lightColorScheme(
    primary = ZeecrownRoyalPurple,
    onPrimary = ZeecrownPureWhite,
    primaryContainer = ZeecrownSoftLavender,
    onPrimaryContainer = ZeecrownDeepPurple,
    secondary = ZeecrownDeepPurple,
    onSecondary = ZeecrownPureWhite,
    secondaryContainer = ZeecrownLightLilac,
    onSecondaryContainer = ZeecrownDeepPurple,
    tertiary = ZeecrownMetallicGold,
    onTertiary = ZeecrownPureWhite,
    tertiaryContainer = ZeecrownChampagneGold.copy(alpha = 0.35f),
    onTertiaryContainer = ZeecrownDeepPurple,
    background = ZeecrownWarmIvory,
    onBackground = ZeecrownObsidianInk,
    surface = ZeecrownPureWhite,
    onSurface = ZeecrownObsidianInk,
    surfaceVariant = ZeecrownLightLilac,
    onSurfaceVariant = ZeecrownMutedPlum,
    surfaceContainerLowest = ZeecrownPureWhite,
    surfaceContainerLow = Color(0xFFFAF7FD),
    surfaceContainer = ZeecrownLightLilac,
    surfaceContainerHigh = ZeecrownSoftLavender,
    surfaceContainerHighest = Color(0xFFE0D1F0),
    outline = ZeecrownMetallicGold,
    outlineVariant = ZeecrownElegantSilver,
    error = ZeecrownBurgundyError,
    onError = ZeecrownPureWhite,
    errorContainer = ZeecrownErrorContainer,
    onErrorContainer = ZeecrownBurgundyError,
)

private val ZeecrownDarkColorScheme = darkColorScheme(
    primary = ZeecrownSoftLavender,
    onPrimary = ZeecrownDeepPurple,
    primaryContainer = ZeecrownRoyalPurple,
    onPrimaryContainer = ZeecrownPureWhite,
    secondary = ZeecrownChampagneGold,
    onSecondary = ZeecrownDeepPurple,
    secondaryContainer = ZeecrownDarkSurfaceVariant,
    onSecondaryContainer = ZeecrownSoftLavender,
    tertiary = ZeecrownMetallicGold,
    onTertiary = ZeecrownDeepPurple,
    tertiaryContainer = Color(0xFF4F3E19),
    onTertiaryContainer = ZeecrownChampagneGold,
    background = ZeecrownDarkBg,
    onBackground = ZeecrownWarmIvory,
    surface = ZeecrownDarkSurface,
    onSurface = ZeecrownWarmIvory,
    surfaceVariant = ZeecrownDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFD5C7E4),
    outline = ZeecrownMetallicGold,
    outlineVariant = Color(0xFF472C66),
    error = Color(0xFFF2B8C2),
    onError = ZeecrownDeepPurple,
)

@Composable
fun ZeecrownTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Keep Zeecrown's Royal Purple, Pure White & Metallic Gold identity authoritative
    val colorScheme = if (darkTheme && isSystemInDarkTheme()) {
        ZeecrownDarkColorScheme
    } else {
        ZeecrownLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ZeecrownTypography,
        shapes = ZeecrownShapes,
        content = content,
    )
}
