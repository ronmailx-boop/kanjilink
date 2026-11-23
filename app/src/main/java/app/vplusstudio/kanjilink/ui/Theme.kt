package app.vplusstudio.kanjilink.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Color

/** Kanji colour on the red app mark, same as the launcher icon. */
val Paper = Color(0xFFF7F3EA)

private val Vermilion = Color(0xFFC8402F)
private val VermilionLight = Color(0xFFE86A56)

private val Light = lightColorScheme(
    primary = Vermilion,
    onPrimary = Color.White,
    secondaryContainer = Color(0xFFF5D9D2),
    onSecondaryContainer = Color(0xFF5A1A10),
    background = Color(0xFFF7F3EA),
    surface = Color(0xFFFFFCF5),
    surfaceVariant = Color(0xFFEDE6D8),
    onBackground = Color(0xFF1C1B19),
    onSurface = Color(0xFF1C1B19),
    onSurfaceVariant = Color(0xFF5E5850),
    outlineVariant = Color(0xFFE3DACB),
)

private val Dark = darkColorScheme(
    primary = VermilionLight,
    onPrimary = Color(0xFF2A0A05),
    secondaryContainer = Color(0xFF4A2621),
    onSecondaryContainer = Color(0xFFF8D6CE),
    background = Color(0xFF111216),
    surface = Color(0xFF1B1C21),
    surfaceVariant = Color(0xFF2A2B31),
    onBackground = Color(0xFFF4F1EA),
    onSurface = Color(0xFFF4F1EA),
    onSurfaceVariant = Color(0xFFA9A59D),
    outlineVariant = Color(0xFF2E2F36),
)

@Composable
fun KanjilinkTheme(content: @Composable () -> Unit) {
    // The UI is English only, so keep it left-to-right even on phones set to Hebrew or Arabic.
    // Remove this when the app gets an RTL translation.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
    }
}
