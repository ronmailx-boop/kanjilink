package app.vplusstudio.kanjilink.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vplusstudio.kanjilink.data.Kanji
import app.vplusstudio.kanjilink.data.Study
import app.vplusstudio.kanjilink.work.KanjiRenderer

/** Japanese glyph shapes for kanji, whatever the phone's language. */
private val JaStyle = TextStyle(localeList = LocaleList("ja"))

@Composable
fun Page(
    title: String,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) TextButton(onClick = onBack) { Text("← Back") }
                Text(
                    title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(start = if (onBack == null) 4.dp else 0.dp),
                )
                action?.invoke()
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun KanjiCard(k: Kanji, reveal: Boolean = true) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "JLPT N${k.level} · ${k.strokes} strokes",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(k.char, style = JaStyle.copy(fontSize = 120.sp), modifier = Modifier.padding(vertical = 4.dp))
            if (reveal) {
                Text(
                    k.meanings.take(4).joinToString(", "),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                ReadingRow("On", k.on, k.onRomaji)
                ReadingRow("Kun", k.kun, k.kunRomaji)
            }
        }
    }
}

@Composable
private fun ReadingRow(label: String, kana: List<String>, romaji: List<String>) {
    if (kana.isEmpty()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(end = 12.dp, top = 2.dp),
        )
        Text(
            kana.take(4).mapIndexed { i, r -> "$r ${romaji[i]}" }.joinToString("   "),
            style = JaStyle.merge(MaterialTheme.typography.bodyLarge),
        )
    }
}

@Composable
fun AnswerButtons(onAgain: () -> Unit, onKnow: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onAgain, modifier = Modifier.weight(1f).height(52.dp)) { Text("Again") }
        Button(onClick = onKnow, modifier = Modifier.weight(1f).height(52.dp)) { Text("I know it") }
    }
}

@Composable
fun Onboarding(onStart: (level: Int, perDay: Int) -> Unit) {
    var level = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(5) }
    var perDay = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(5) }
    Page("Kanjilink") {
        Text("Learn kanji every time you look at your phone.", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "A new kanji appears on your lock screen, in a widget and in a gentle notification. " +
                "Tap “I know it” or “Again” and Kanjilink spaces the reviews for you.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Text("Start at", style = MaterialTheme.typography.titleMedium)
        ChipRow(listOf(5, 4, 3, 2, 1), level.intValue, { "N$it" }) { level.intValue = it }
        Text(
            "N5 is the easiest. Not sure? Start at N5.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Text("New kanji per day", style = MaterialTheme.typography.titleMedium)
        ChipRow(listOf(3, 5, 10, 15), perDay.intValue, { "$it" }) { perDay.intValue = it }
        Spacer(Modifier.height(32.dp))
        Button(onClick = { onStart(level.intValue, perDay.intValue) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Start learning")
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "No account. Your progress stays on this phone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Wraps to a new line when chips do not fit (large system font sizes). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRow(values: List<Int>, selected: Int, label: (Int) -> String, onSelect: (Int) -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { v -> FilterChip(selected = v == selected, onClick = { onSelect(v) }, label = { Text(label(v)) }) }
    }
}

@Composable
fun Home(
    current: Kanji,
    stats: Study.Stats,
    lockOn: Boolean,
    onAnswer: (Boolean) -> Unit,
    onReview: () -> Unit,
    onSettings: () -> Unit,
    onLockToggle: (Boolean) -> Unit,
) {
    Page("Kanjilink", action = { TextButton(onClick = onSettings) { Text("Settings") } }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("Streak", "${stats.streak} d", Modifier.weight(1f))
            Stat("Learned in N${stats.level}", "${stats.learned}/${stats.total}", Modifier.weight(1f))
            Stat("Due", "${stats.due}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        KanjiCard(current)
        Spacer(Modifier.height(16.dp))
        AnswerButtons(onAgain = { onAnswer(false) }, onKnow = { onAnswer(true) })
        Spacer(Modifier.height(12.dp))
        if (stats.due > 0) {
            OutlinedButton(onClick = onReview, modifier = Modifier.fillMaxWidth()) { Text("Review ${stats.due} due") }
        }
        Spacer(Modifier.height(20.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Kanji on lock screen", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Replaces your lock-screen wallpaper with the current kanji.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = lockOn, onCheckedChange = onLockToggle)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Tip: long-press your home screen → Widgets → Kanjilink to add the widget.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun Review(k: Kanji?, revealed: Boolean, remaining: Int, onReveal: () -> Unit, onAnswer: (Boolean) -> Unit, onBack: () -> Unit) {
    Page("Review", onBack = onBack) {
        if (k == null) {
            Text("All caught up. Nice work!", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBack) { Text("Done") }
            return@Page
        }
        Text("$remaining left", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        KanjiCard(k, reveal = revealed)
        Spacer(Modifier.height(16.dp))
        if (revealed) {
            AnswerButtons(onAgain = { onAnswer(false) }, onKnow = { onAnswer(true) })
        } else {
            Button(onClick = onReveal, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Show meaning") }
        }
    }
}

data class SettingsState(
    val notificationsOn: Boolean,
    val intervalHours: Int,
    val quietStart: Int,
    val quietEnd: Int,
    val lockOn: Boolean,
    val theme: Int,
    val startLevel: Int,
    val newPerDay: Int,
)

@Composable
fun Settings(
    s: SettingsState,
    onChange: (SettingsState) -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
) {
    Page("Settings", onBack = onBack) {
        Section("Notifications")
        ToggleRow("Show kanji notifications", s.notificationsOn) { onChange(s.copy(notificationsOn = it)) }
        Text("New kanji every", style = MaterialTheme.typography.bodyMedium)
        ChipRow(listOf(1, 2, 3, 4, 6), s.intervalHours, { "${it}h" }) { onChange(s.copy(intervalHours = it)) }
        Spacer(Modifier.height(8.dp))
        Text("Quiet hours: no notifications", style = MaterialTheme.typography.bodyMedium)
        HourStepper("From", s.quietStart) { onChange(s.copy(quietStart = it)) }
        HourStepper("To", s.quietEnd) { onChange(s.copy(quietEnd = it)) }

        Section("Lock screen")
        ToggleRow("Kanji as lock-screen wallpaper", s.lockOn) { onChange(s.copy(lockOn = it)) }
        ChipRow(KanjiRenderer.paletteNames.indices.toList(), s.theme, { KanjiRenderer.paletteNames[it] }) {
            onChange(s.copy(theme = it))
        }
        Text(
            "Some phones (for example with a custom lock screen) may not show it. Notifications still work.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Section("Study")
        Text("Study path starts at", style = MaterialTheme.typography.bodyMedium)
        ChipRow(listOf(5, 4, 3, 2, 1), s.startLevel, { "N$it" }) { onChange(s.copy(startLevel = it)) }
        Text("New kanji per day", style = MaterialTheme.typography.bodyMedium)
        ChipRow(listOf(3, 5, 10, 15), s.newPerDay, { "$it" }) { onChange(s.copy(newPerDay = it)) }

        Section("About")
        TextButton(onClick = onAbout) { Text("Sources & licences") }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun HourStepper(label: String, hour: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.padding(end = 8.dp))
        TextButton(onClick = { onChange((hour + 23) % 24) }) { Text("–") }
        Text("%02d:00".format(hour), fontWeight = FontWeight.SemiBold)
        TextButton(onClick = { onChange((hour + 1) % 24) }) { Text("+") }
    }
}

@Composable
fun About(dataVersion: String, appVersion: String, onBack: () -> Unit) {
    val uri = LocalUriHandler.current
    Page("Sources & licences", onBack = onBack) {
        Text("Kanjilink $appVersion", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Text(
            "This app uses the KANJIDIC2 dictionary file (database version $dataVersion). These files are the " +
                "property of the Electronic Dictionary Research and Development Group, and are used in conformance " +
                "with the Group's licence (Creative Commons Attribution-ShareAlike 4.0).",
        )
        LinkButton("KANJIDIC Project") { uri.openUri("https://www.edrdg.org/wiki/index.php/KANJIDIC_Project") }
        LinkButton("EDRDG licence") { uri.openUri("https://www.edrdg.org/edrdg/licence.html") }
        Spacer(Modifier.height(12.dp))
        Text("JLPT N5–N1 kanji lists by Jonathan Waller (tanos.co.uk), via the kanji-data project by David Gouveia (MIT).")
        LinkButton("Jonathan Waller's JLPT resources") { uri.openUri("http://www.tanos.co.uk/jlpt/") }
        LinkButton("kanji-data") { uri.openUri("https://github.com/davidluzgouveia/kanji-data") }
        Spacer(Modifier.height(12.dp))
        Text(
            "The JLPT has not published official kanji lists since 2010; levels are a study guide only.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text("Privacy: no account, no ads, no tracking. Your progress is stored only on this phone.")
    }
}

@Composable
private fun LinkButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(label) }
}
