package app.vplusstudio.kanjilink

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import app.vplusstudio.kanjilink.data.Kanji
import app.vplusstudio.kanjilink.data.KanjiRepository
import app.vplusstudio.kanjilink.data.Prefs
import app.vplusstudio.kanjilink.data.Study
import app.vplusstudio.kanjilink.ui.About
import app.vplusstudio.kanjilink.ui.Home
import app.vplusstudio.kanjilink.ui.KanjilinkTheme
import app.vplusstudio.kanjilink.ui.Onboarding
import app.vplusstudio.kanjilink.ui.Review
import app.vplusstudio.kanjilink.ui.Settings
import app.vplusstudio.kanjilink.ui.SettingsState
import app.vplusstudio.kanjilink.work.Outputs
import app.vplusstudio.kanjilink.work.RotationWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private enum class Screen { Onboarding, Home, Review, Settings, About, Paywall }

    private lateinit var prefs: Prefs
    private var screen by mutableStateOf(Screen.Home)
    private var current by mutableStateOf<Kanji?>(null)
    private var stats by mutableStateOf(Study.Stats(5, 0, 0, 0, 0, 0))
    private var reviewQueue by mutableStateOf<List<Kanji>>(emptyList())
    private var reviewTotal by mutableStateOf(0)
    private var revealed by mutableStateOf(false)
    private var homeRevealed by mutableStateOf(true)
    private var currentIsNew by mutableStateOf(true)
    private var homePreview by mutableStateOf("10 min" to "1 day")
    private var reviewPreview by mutableStateOf("10 min" to "1 day")
    private var settings by mutableStateOf<SettingsState?>(null)
    private var billingState by mutableStateOf(app.vplusstudio.kanjilink.billing.Billing.State(isPro = false))
    private var freeLevelDone by mutableStateOf(false)
    private var paywallReturn = Screen.Home
    private lateinit var billing: app.vplusstudio.kanjilink.billing.Billing

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                prefs.notificationsOn = false
                loadSettings()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = Prefs(this)
        app.vplusstudio.kanjilink.ui.KanjiFontState.style = prefs.kanjiStyle
        billing = app.vplusstudio.kanjilink.billing.Billing(this) { st -> onBilling(st) }
        billingState = billing.state
        billing.start()
        screen = if (prefs.onboarded) Screen.Home else Screen.Onboarding
        loadSettings()
        refresh()

        setContent {
            KanjilinkTheme {
                BackHandler(enabled = screen != Screen.Home && screen != Screen.Onboarding) {
                    screen = when (screen) {
                        Screen.About -> Screen.Settings
                        Screen.Paywall -> paywallReturn
                        else -> Screen.Home
                    }
                    refresh()
                }
                when (screen) {
                    Screen.Onboarding -> Onboarding(
                        isPro = billingState.isPro,
                        onUpgrade = { openPaywall() },
                    ) { level, perDay -> finishOnboarding(level, perDay) }
                    Screen.Paywall -> app.vplusstudio.kanjilink.ui.Paywall(
                        isPro = billingState.isPro,
                        price = billingState.price,
                        available = billingState.available,
                        onBuy = { if (!billing.buy(this)) toast("Google Play is not ready. Please try again in a moment.") },
                        onRestore = { billing.refreshPurchases(); toast("Checking your purchases…") },
                        onBack = { screen = paywallReturn; refresh() },
                    )
                    Screen.Home -> current?.let { k ->
                        Home(
                            current = k,
                            stats = stats,
                            lockOn = settings?.lockOn == true,
                            revealed = homeRevealed,
                            isNew = currentIsNew,
                            preview = homePreview,
                            onReveal = { homeRevealed = true },
                            onAnswer = { known -> answer(k, known, advance = true) },
                            onReview = { startReview() },
                            onSettings = { screen = Screen.Settings },
                            onLockToggle = { on -> settings?.let { applySettings(it.copy(lockOn = on)) } },
                            isPro = billingState.isPro,
                            freeLevelDone = freeLevelDone,
                            onUpgrade = { openPaywall() },
                        )
                    }
                    Screen.Review -> Review(
                        k = reviewQueue.firstOrNull(),
                        revealed = revealed,
                        remaining = reviewQueue.size,
                        preview = reviewPreview,
                        onReveal = { revealed = true },
                        onAnswer = { known -> reviewQueue.firstOrNull()?.let { answer(it, known, advance = false) } },
                        onBack = { screen = Screen.Home; refresh() },
                        total = reviewTotal,
                    )
                    Screen.Settings -> settings?.let { s ->
                        Settings(
                            s = s,
                            onChange = { applySettings(it) },
                            onAbout = { screen = Screen.About },
                            onBack = { screen = Screen.Home; refresh() },
                            onUpgrade = { openPaywall() },
                        )
                    }
                    Screen.About -> About(
                        dataVersion = KanjiRepository.dataVersion,
                        appVersion = BuildConfigVersion.name(this),
                        onBack = { screen = Screen.Settings },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        billing.end()
        super.onDestroy()
    }

    private fun openPaywall() {
        paywallReturn = if (screen == Screen.Paywall) paywallReturn else screen
        screen = Screen.Paywall
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    /** Google Play reported a change: Pro unlocked/restored/refunded, a price, or a message. */
    private fun onBilling(st: app.vplusstudio.kanjilink.billing.Billing.State) {
        val proChanged = st.isPro != billingState.isPro
        billingState = st
        st.message?.let { toast(it); billing.clearMessage() }
        if (proChanged) {
            prefs.currentKanji = null
            if (!st.isPro) prefs.startLevel = Study.FREE_LEVEL
            loadSettings()
            lifecycleScope.launch(Dispatchers.IO) {
                val k = Study.current(this@MainActivity)
                Outputs.refreshAll(this@MainActivity, k, notify = false)
                withContext(Dispatchers.Main) { refresh() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        lifecycleScope.launch {
            val ctx = this@MainActivity
            val (k, s) = withContext(Dispatchers.IO) { Study.current(ctx) to Study.stats(ctx) }
            val (isNew, preview) = withContext(Dispatchers.IO) { Study.isNew(ctx, k.char) to Study.preview(ctx, k.char) }
            freeLevelDone = withContext(Dispatchers.IO) { Study.freeLevelComplete(ctx) }
            // A new kanji is shown in full; one seen before starts hidden as a small self-test.
            if (k.char != current?.char) homeRevealed = isNew
            current = k
            currentIsNew = isNew
            homePreview = preview
            stats = s
            reviewQueue.firstOrNull()?.let { r -> reviewPreview = withContext(Dispatchers.IO) { Study.preview(ctx, r.char) } }
        }
    }

    private fun finishOnboarding(level: Int, perDay: Int) {
        prefs.startLevel = level
        prefs.newPerDay = perDay
        prefs.onboarded = true
        prefs.currentKanji = null
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        RotationWorker.schedule(this)
        loadSettings()
        screen = Screen.Home
        lifecycleScope.launch(Dispatchers.IO) {
            val k = Study.current(this@MainActivity)
            Outputs.refreshAll(this@MainActivity, k, notify = false)
            withContext(Dispatchers.Main) { refresh() }
        }
    }

    private fun answer(k: Kanji, known: Boolean, advance: Boolean) {
        lifecycleScope.launch {
            val back = withContext(Dispatchers.IO) {
                val back = Study.answer(this@MainActivity, k.char, known)
                if (advance) {
                    val next = Study.advance(this@MainActivity)
                    Outputs.cancelNotification(this@MainActivity)
                    Outputs.refreshAll(this@MainActivity, next, notify = false)
                }
                back
            }
            Toast.makeText(this@MainActivity, "${k.char} · back in $back", Toast.LENGTH_SHORT).show()
            if (!advance) {
                reviewQueue = reviewQueue.drop(1)
                revealed = false
            }
            refresh()
        }
    }

    private fun startReview() {
        lifecycleScope.launch {
            reviewQueue = withContext(Dispatchers.IO) { Study.dueList(this@MainActivity) }
            reviewTotal = reviewQueue.size
            reviewQueue.firstOrNull()?.let { reviewPreview = withContext(Dispatchers.IO) { Study.preview(this@MainActivity, it.char) } }
            revealed = false
            screen = Screen.Review
        }
    }

    private fun loadSettings() {
        settings = SettingsState(
            notificationsOn = prefs.notificationsOn,
            intervalHours = prefs.intervalHours,
            quietStart = prefs.quietStart,
            quietEnd = prefs.quietEnd,
            lockOn = prefs.lockWallpaperOn,
            theme = prefs.wallpaperTheme,
            startLevel = prefs.startLevel,
            newPerDay = prefs.newPerDay,
            kanjiStyle = prefs.kanjiStyle,
            isPro = prefs.isPro,
        )
    }

    private fun applySettings(new: SettingsState) {
        val old = settings ?: return
        prefs.notificationsOn = new.notificationsOn
        prefs.intervalHours = new.intervalHours
        prefs.quietStart = new.quietStart
        prefs.quietEnd = new.quietEnd
        prefs.lockWallpaperOn = new.lockOn
        prefs.wallpaperTheme = new.theme
        prefs.startLevel = new.startLevel
        prefs.newPerDay = new.newPerDay
        prefs.kanjiStyle = new.kanjiStyle
        app.vplusstudio.kanjilink.ui.KanjiFontState.style = new.kanjiStyle
        settings = new

        if (new.notificationsOn && !old.notificationsOn && Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!new.notificationsOn) Outputs.cancelNotification(this)
        if (new.intervalHours != old.intervalHours) RotationWorker.schedule(this, replace = true)
        if (new.startLevel != old.startLevel) prefs.currentKanji = null

        val wallpaperChanged = (new.lockOn && !old.lockOn) || (new.lockOn && new.theme != old.theme) ||
            new.startLevel != old.startLevel || new.kanjiStyle != old.kanjiStyle
        if (wallpaperChanged) {
            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    val k = Study.current(this@MainActivity)
                    Outputs.updateWidgets(this@MainActivity)
                    if (new.lockOn) Outputs.setLockWallpaper(this@MainActivity, k) else true
                }
                if (!ok) {
                    Toast.makeText(this@MainActivity, R.string.wallpaper_failed, Toast.LENGTH_LONG).show()
                }
                refresh()
            }
        }
    }
}

/** Avoids enabling BuildConfig generation just for the version name. */
object BuildConfigVersion {
    fun name(activity: ComponentActivity): String = try {
        activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: ""
    } catch (e: Exception) {
        ""
    }
}
