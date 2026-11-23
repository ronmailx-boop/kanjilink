package app.vplusstudio.kanjilink.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vplusstudio.kanjilink.R
import app.vplusstudio.kanjilink.data.Kanji
import app.vplusstudio.kanjilink.data.Study
import app.vplusstudio.kanjilink.work.KanjiRenderer

/** Bundled kanji fonts (SIL OFL 1.1): Noto Sans JP (default) and Noto Serif JP. */
private val SansKanji = FontFamily(Font(R.font.noto_sans_jp_bold, FontWeight.Bold))
private val SerifKanji = FontFamily(Font(R.font.noto_serif_jp_bold, FontWeight.Bold))
val KanjiStyleNames = listOf("Sans", "Serif")

/** The chosen kanji style, shared by every screen (set from Prefs in MainActivity). */
object KanjiFontState {
    var style by androidx.compose.runtime.mutableIntStateOf(0)
}

/** Japanese glyph shapes for kana, whatever the phone's language. */
private val JaStyle = TextStyle(localeList = LocaleList("ja"))
/** The app mark always uses the serif 字, like the launcher icon. */
private val MarkStyle = TextStyle(fontFamily = SerifKanji, fontWeight = FontWeight.Bold, localeList = LocaleList("ja"))

private fun kanjiStyle(style: Int = KanjiFontState.style) =
    TextStyle(fontFamily = if (style == 1) SerifKanji else SansKanji, fontWeight = FontWeight.Bold, localeList = LocaleList("ja"))

private val CardShape = RoundedCornerShape(24.dp)

/** The app's mark: the launcher icon (red square, 字) at any size. */
@Composable
fun AppMark(size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text("字", style = MarkStyle.copy(fontSize = (size.value * 0.56f).sp, lineHeight = (size.value * 0.6f).sp), color = Paper)
    }
}

@Composable
fun Page(
    title: String,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_back), contentDescription = "Back") }
                    Spacer(Modifier.width(4.dp))
                } else {
                    AppMark(32.dp)
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                action?.invoke()
            }
            Spacer(Modifier.height(12.dp))
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, tinted: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (tinted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        ),
        border = if (tinted) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) { Column(Modifier.padding(20.dp), content = content) }
}

@Composable
private fun LevelPill(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
fun KanjiCard(k: Kanji, reveal: Boolean = true) {
    Panel {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            LevelPill("JLPT N${k.level} · ${k.strokes} ${if (k.strokes == 1) "stroke" else "strokes"}")
            Text(
                k.char,
                style = kanjiStyle().copy(fontSize = 132.sp, lineHeight = 150.sp),
                modifier = Modifier.padding(vertical = 8.dp),
            )
            if (!reveal) {
                Text(
                    "What does it mean? How is it read?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            } else {
                Text(
                    k.meanings.take(4).joinToString(", "),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))
                ReadingRow("On", k.on, k.onRomaji)
                ReadingRow("Kun", k.kun, k.kunRomaji)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReadingRow(label: String, kana: List<String>, romaji: List<String>) {
    if (kana.isEmpty()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            label.uppercase(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(48.dp).padding(top = 4.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            kana.take(4).forEachIndexed { i, r ->
                // Non-breaking space keeps the kana and its romaji on one line.
                Text(
                    "$r ${romaji[i]}",
                    style = JaStyle.merge(MaterialTheme.typography.bodyLarge),
                    softWrap = false,
                )
            }
        }
    }
}

/** Each button says when this kanji comes back, so the choice is clear. */
@Composable
fun AnswerButtons(preview: Pair<String, String>, onAgain: () -> Unit, onKnow: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onAgain, modifier = Modifier.weight(1f).heightIn(min = 64.dp)) {
            ButtonLabel("Again", "in ${preview.first}")
        }
        Button(onClick = onKnow, modifier = Modifier.weight(1f).heightIn(min = 64.dp)) {
            ButtonLabel("I know it", "in ${preview.second}")
        }
    }
}

@Composable
private fun ButtonLabel(title: String, caption: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(caption, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

/** For kanji seen before: try to remember first, then reveal and answer. */
@Composable
fun RevealOrAnswer(revealed: Boolean, preview: Pair<String, String>, onReveal: () -> Unit, onAnswer: (Boolean) -> Unit) {
    if (revealed) {
        AnswerButtons(preview, onAgain = { onAnswer(false) }, onKnow = { onAnswer(true) })
    } else {
        Button(onClick = onReveal, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            ButtonLabel("Show meaning", "Do you remember it?")
        }
    }
}

@Composable
fun Onboarding(isPro: Boolean = false, onUpgrade: () -> Unit = {}, onStart: (level: Int, perDay: Int) -> Unit) {
    val level = remember { mutableIntStateOf(5) }
    val perDay = remember { mutableIntStateOf(5) }
    Page("Kanjilink") {
        Spacer(Modifier.height(8.dp))
        AppMark(72.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            "Learn kanji every time you look at your phone.",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "A new kanji appears on your lock screen, in a widget and in a gentle notification. " +
                "Tap “I know it” and it comes back in a day, then in a few days, then weeks. " +
                "Tap “Again” and it comes back in 10 minutes.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Panel {
            Text("Start at", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            ChipRow(listOf(5, 4, 3, 2, 1), level.intValue, { "N$it" }, locked = { !isPro && it != 5 }, onLocked = onUpgrade) {
                level.intValue = it
            }
            Text(
                if (isPro) "N5 is the easiest. Not sure? Start at N5." else "N5 (79 kanji) is free. N4 to N1 come with Kanjilink Pro.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Text("New kanji per day", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            ChipRow(listOf(3, 5, 10, 15), perDay.intValue, { "$it" }) { perDay.intValue = it }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onStart(level.intValue, perDay.intValue) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text("Start learning", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "No account. Your progress stays on this phone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Wraps to a new line when chips do not fit (large system font sizes). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRow(
    values: List<Int>,
    selected: Int,
    label: (Int) -> String,
    locked: (Int) -> Boolean = { false },
    onLocked: () -> Unit = {},
    onSelect: (Int) -> Unit,
) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { v ->
            val isLocked = locked(v)
            FilterChip(
                selected = v == selected,
                onClick = { if (isLocked) onLocked() else onSelect(v) },
                label = {
                    Text(
                        if (isLocked) "${label(v)} · Pro" else label(v),
                        fontWeight = if (v == selected) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
fun Home(
    current: Kanji,
    stats: Study.Stats,
    lockOn: Boolean,
    revealed: Boolean,
    isNew: Boolean,
    preview: Pair<String, String>,
    onReveal: () -> Unit,
    onAnswer: (Boolean) -> Unit,
    onReview: () -> Unit,
    onSettings: () -> Unit,
    onLockToggle: (Boolean) -> Unit,
    isPro: Boolean = true,
    freeLevelDone: Boolean = false,
    onUpgrade: () -> Unit = {},
) {
    Page(
        "Kanjilink",
        action = {
            IconButton(onClick = onSettings) { Icon(painterResource(R.drawable.ic_settings), contentDescription = "Settings") }
        },
    ) {
        Panel(tinted = true) {
            Row(Modifier.fillMaxWidth()) {
                Stat("${stats.streak}", if (stats.streak == 1) "day streak" else "day streak", Modifier.weight(1f))
                Stat("${stats.learned}/${stats.total}", "N${stats.level}", Modifier.weight(1f))
                Stat("${stats.due}", "due", Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { if (stats.total == 0) 0f else stats.learned.toFloat() / stats.total },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                trackColor = MaterialTheme.colorScheme.background,
                drawStopIndicator = {},
            )
        }
        Spacer(Modifier.height(16.dp))
        if (isNew) {
            Text(
                "NEW KANJI",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
        }
        KanjiCard(current, reveal = revealed)
        Spacer(Modifier.height(16.dp))
        RevealOrAnswer(revealed, preview, onReveal, onAnswer)
        if (stats.due > 0) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onReview, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Review ${stats.due} due", fontWeight = FontWeight.Bold)
            }
        }
        if (!isPro) {
            Spacer(Modifier.height(20.dp))
            UpgradeCard(freeLevelDone, onUpgrade)
        }
        Spacer(Modifier.height(20.dp))
        Panel(tinted = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Kanji on lock screen", fontWeight = FontWeight.Bold)
                    Text(
                        "Replaces your lock-screen wallpaper with the current kanji.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = lockOn, onCheckedChange = onLockToggle)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Tip: long-press your home screen → Widgets → Kanjilink to add the widget.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun UpgradeCard(freeLevelDone: Boolean, onUpgrade: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                if (freeLevelDone) "You have met all 79 N5 kanji!" else "Ready for more than N5?",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Kanjilink Pro unlocks N4, N3, N2 and N1: 2,132 more kanji. One payment, no subscription.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onUpgrade, modifier = Modifier.heightIn(min = 48.dp)) { Text("See Kanjilink Pro", fontWeight = FontWeight.Bold) }
        }
    }
}

/** The Pro purchase screen. Google Play shows the real price and handles payment, VAT and receipts. */
@Composable
fun Paywall(
    isPro: Boolean,
    price: String?,
    available: Boolean,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onBack: () -> Unit,
    onReviewCode: (String) -> Boolean = { false },
) {
    Page("Kanjilink Pro", onBack = onBack) {
        Panel {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AppMark(64.dp)
                Spacer(Modifier.height(16.dp))
                Text(
                    if (isPro) "Pro is unlocked. Thank you!" else "Learn every JLPT kanji",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(16.dp))
            Benefit("All 2,211 kanji: JLPT N4, N3, N2 and N1, on top of the free N5")
            Benefit("Start at any level, on the lock screen, widget and notifications")
            Benefit("One payment. No subscription, no ads, no account")
            Benefit("Your progress stays on your phone")
        }
        Spacer(Modifier.height(20.dp))
        if (!isPro) {
            Button(
                onClick = onBuy,
                enabled = available && price != null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text(if (price != null) "Unlock Pro · $price" else "Unlock Pro", fontWeight = FontWeight.Bold)
            }
            if (!available || price == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Purchases work in the version installed from Google Play.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Text("Restore purchase", fontWeight = FontWeight.Bold) }
            ReviewCode(onReviewCode)
        } else {
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Continue", fontWeight = FontWeight.Bold) }
        }
    }
}

/** Small "Have a review code?" link that opens a field; for Google Play's app reviewers. */
@Composable
private fun ReviewCode(onReviewCode: (String) -> Boolean) {
    val open = remember { androidx.compose.runtime.mutableStateOf(false) }
    val code = remember { androidx.compose.runtime.mutableStateOf("") }
    val error = remember { androidx.compose.runtime.mutableStateOf(false) }
    if (!open.value) {
        TextButton(onClick = { open.value = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Have a review code?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Spacer(Modifier.height(8.dp))
    androidx.compose.material3.OutlinedTextField(
        value = code.value,
        onValueChange = { code.value = it; error.value = false },
        label = { Text("Review code") },
        singleLine = true,
        isError = error.value,
        supportingText = { if (error.value) Text("This code is not valid.") },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = { if (!onReviewCode(code.value)) error.value = true },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) { Text("Apply code", fontWeight = FontWeight.Bold) }
}

@Composable
private fun Benefit(text: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
        Text(text)
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun Review(
    k: Kanji?,
    revealed: Boolean,
    remaining: Int,
    preview: Pair<String, String>,
    onReveal: () -> Unit,
    onAnswer: (Boolean) -> Unit,
    onBack: () -> Unit,
    total: Int = remaining,
) {
    Page("Review", onBack = onBack) {
        if (k == null) {
            Panel {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    AppMark(56.dp)
                    Spacer(Modifier.height(16.dp))
                    Text("All caught up. Nice work!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack, modifier = Modifier.heightIn(min = 52.dp)) { Text("Done") }
                }
            }
            return@Page
        }
        val done = (total - remaining).coerceAtLeast(0)
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else done.toFloat() / total },
                modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                drawStopIndicator = {},
            )
            Spacer(Modifier.width(12.dp))
            Text("$remaining left", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(16.dp))
        KanjiCard(k, reveal = revealed)
        Spacer(Modifier.height(16.dp))
        RevealOrAnswer(revealed, preview, onReveal, onAnswer)
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
    val kanjiStyle: Int = 0,
    val isPro: Boolean = true,
)

@Composable
fun Settings(
    s: SettingsState,
    onChange: (SettingsState) -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
    onUpgrade: () -> Unit = {},
) {
    Page("Settings", onBack = onBack) {
        Section("Notifications") {
            ToggleRow("Show kanji notifications", s.notificationsOn) { onChange(s.copy(notificationsOn = it)) }
            Label("New kanji every")
            ChipRow(listOf(1, 2, 3, 4, 6), s.intervalHours, { "${it}h" }) { onChange(s.copy(intervalHours = it)) }
            Label("Quiet hours (no notifications)")
            HourStepper("From", s.quietStart) { onChange(s.copy(quietStart = it)) }
            HourStepper("To", s.quietEnd) { onChange(s.copy(quietEnd = it)) }
        }
        Section("Lock screen") {
            ToggleRow("Kanji as lock-screen wallpaper", s.lockOn) { onChange(s.copy(lockOn = it)) }
            Label("Style")
            ChipRow(KanjiRenderer.paletteNames.indices.toList(), s.theme, { KanjiRenderer.paletteNames[it] }) {
                onChange(s.copy(theme = it))
            }
            Text(
                "Some phones (for example with a custom lock screen) may not show it. Notifications still work.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Section("Kanji style") {
            Text(
                "Sans is clean and closest to handwriting, good for counting strokes. Serif looks like a printed book.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KanjiStyleNames.forEachIndexed { i, name -> StyleOption(name, i, selected = s.kanjiStyle == i, Modifier.weight(1f)) { onChange(s.copy(kanjiStyle = i)) } }
            }
        }
        Section("Study") {
            Label("Study path starts at")
            ChipRow(listOf(5, 4, 3, 2, 1), s.startLevel, { "N$it" }, locked = { !s.isPro && it != 5 }, onLocked = onUpgrade) {
                onChange(s.copy(startLevel = it))
            }
            Label("New kanji per day")
            ChipRow(listOf(3, 5, 10, 15), s.newPerDay, { "$it" }) { onChange(s.copy(newPerDay = it)) }
        }
        Section("About") {
            if (!s.isPro) TextButton(onClick = onUpgrade) { Text("Kanjilink Pro", fontWeight = FontWeight.Bold) }
            TextButton(onClick = onAbout) { Text("Sources & licences", fontWeight = FontWeight.Bold) }
        }
    }
}

/** A tappable sample: 国 in the style, with its name. */
@Composable
private fun StyleOption(name: String, style: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) colors.secondaryContainer else colors.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant),
    ) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("国学", style = kanjiStyle(style).copy(fontSize = 34.sp, lineHeight = 40.sp))
            Text(name, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
    )
    Panel(content = content)
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun Label(text: String) {
    Spacer(Modifier.height(12.dp))
    Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** "From  –  22:00  +". On a narrow screen the buttons move under the label instead of being squeezed. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HourStepper(label: String, hour: Int, onChange: (Int) -> Unit) {
    val pad = androidx.compose.foundation.layout.PaddingValues(0.dp)
    FlowRow(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.widthIn(min = 56.dp), softWrap = false)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { onChange((hour + 23) % 24) }, modifier = Modifier.size(48.dp), contentPadding = pad) {
                Text("–", fontWeight = FontWeight.Bold)
            }
            Text("%02d:00".format(hour), fontWeight = FontWeight.Bold, softWrap = false, modifier = Modifier.padding(horizontal = 10.dp))
            OutlinedButton(onClick = { onChange((hour + 1) % 24) }, modifier = Modifier.size(48.dp), contentPadding = pad) {
                Text("+", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun About(dataVersion: String, appVersion: String, onBack: () -> Unit) {
    val uri = LocalUriHandler.current
    Page("Sources & licences", onBack = onBack) {
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppMark(44.dp)
                Spacer(Modifier.width(12.dp))
                Text("Kanjilink $appVersion", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            Text("Privacy: no account, no ads, no tracking. Your progress is stored only on this phone.")
            LinkButton("Privacy policy") { uri.openUri("https://kanjilink.vplusstudio.app/privacy") }
            LinkButton("Terms of use") { uri.openUri("https://kanjilink.vplusstudio.app/terms") }
        }
        Spacer(Modifier.height(16.dp))
        Panel {
            Text(
                "This app uses the KANJIDIC2 dictionary file (database version $dataVersion). These files are the " +
                    "property of the Electronic Dictionary Research and Development Group, and are used in conformance " +
                    "with the Group's licence (Creative Commons Attribution-ShareAlike 4.0).",
            )
            LinkButton("KANJIDIC Project") { uri.openUri("https://www.edrdg.org/wiki/index.php/KANJIDIC_Project") }
            LinkButton("EDRDG licence") { uri.openUri("https://www.edrdg.org/edrdg/licence.html") }
        }
        Spacer(Modifier.height(16.dp))
        Panel {
            Text("JLPT N5–N1 kanji lists by Jonathan Waller (tanos.co.uk), via the kanji-data project by David Gouveia (MIT).")
            LinkButton("Jonathan Waller's JLPT resources") { uri.openUri("http://www.tanos.co.uk/jlpt/") }
            LinkButton("kanji-data") { uri.openUri("https://github.com/davidluzgouveia/kanji-data") }
            Text(
                "The JLPT has not published official kanji lists since 2010; levels are a study guide only.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
        Panel {
            Text("Kanji are set in Noto Sans JP and Noto Serif JP (SIL Open Font License 1.1). Icons: Material Icons (Apache 2.0).")
            LinkButton("Noto CJK fonts") { uri.openUri("https://github.com/notofonts/noto-cjk") }
        }
    }
}

@Composable
private fun LinkButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(label, fontWeight = FontWeight.Bold) }
}
