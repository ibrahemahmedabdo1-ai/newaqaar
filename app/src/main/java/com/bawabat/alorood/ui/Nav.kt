package com.bawabat.alorood.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bawabat.alorood.BuildConfig
import com.bawabat.alorood.Changelog
import com.bawabat.alorood.data.Api
import com.bawabat.alorood.data.Remote
import com.bawabat.alorood.data.Repo
import com.bawabat.alorood.data.Role
import com.bawabat.alorood.util.Intents
import com.bawabat.alorood.util.UpdateState
import kotlinx.coroutines.delay

object Routes {
    const val LANG = "lang"
    const val WELCOME = "welcome"
    const val ROLE = "role"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT = "forgot"
    const val OTP = "otp"
    const val SUBSCRIBE = "subscribe"
    const val PAID = "paid"
    const val CREATED = "created"
    const val MAIN = "main"
    const val SEARCH = "search"
    const val FILTERS = "filters"
    const val PRIORITIES = "priorities"
    const val AREAS = "areas"
    const val ADVISOR = "advisor"
    const val BLOCKED = "blocked"
    const val PROFILE_INFO = "profileinfo"
    const val CHANGE_PHONE = "changephone"
    const val CHANGE_PW = "changepw"
    const val LINKED = "linked"
    const val SETTINGS = "settings"
    const val CONTACT = "contact"
    const val FAQ = "faq"
    const val RESERVATIONS = "reservations"
    const val AD_SENT = "adsent"
    const val NEWS = "news"

    fun createPw(mode: String) = "createpw/$mode"
    fun detail(id: String) = "detail/$id"
    fun floors(id: String) = "floors/$id"
    fun clientInfo(id: String) = "clientinfo/$id"
    fun offer(id: String) = "offer/$id"
    fun booking(id: String) = "booking/$id"
    fun sent(id: String) = "sent/$id"
    fun thread(id: String) = "thread/$id"
    fun legal(kind: String) = "legal/$kind"
    fun soon(kind: String) = "soon/$kind"
    fun result(kind: String, ok: Boolean) = "result/$kind/$ok"
    fun phoneResult(ok: Boolean) = result("phone", ok)
    fun addAd(id: String? = null) = "adform/${id ?: "new"}"
}

fun NavHostController.clearTo(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun MainScreen(nav: NavHostController) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var needLogin by remember { mutableStateOf(false) }
    var hiddenAnn by rememberSaveable { mutableStateOf("") }
    val role = Repo.role
    val toLogin = { nav.navigate(Routes.LOGIN) }
    val ann = Repo.announcements.firstOrNull { it.id != hiddenAnn && (it.titleAr.isNotBlank() || it.bodyAr.isNotBlank()) }
    BackHandler(enabled = tab != 0) { tab = 0 }
    Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> HomeTab(nav) { needLogin = true }
                    1 -> MessagesTab(nav, toLogin)
                    2 -> if (role == Role.DEVELOPER) MyAdsTab(nav, toLogin) else OffersTab(nav, toLogin)
                    else -> ProfileTab(nav, toLogin)
                }
            }
            if (ann != null) {
                Row(
                    Modifier.fillMaxWidth().background(if (ann.level == "WARN") Gold else Peach).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (Repo.lang == "ar") "${ann.titleAr} — ${ann.bodyAr}" else "${ann.titleEn.ifEmpty { ann.titleAr }} — ${ann.bodyEn.ifEmpty { ann.bodyAr }}",
                        color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { hiddenAnn = ann.id }) { Text("×", color = Ink, fontSize = 18.sp) }
                }
            }
            MainBottomBar(role, tab, { tab = it }, { menu = true })
        }
    }
    if (menu) MenuSheet(nav, { menu = false }, { tab = 0 })
    if (needLogin) LoginRequiredDialog({ needLogin = false; nav.navigate(Routes.LOGIN) }, { needLogin = false })
}

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val start = when {
        !Repo.langChosen -> Routes.LANG
        Repo.loggedIn -> Routes.MAIN
        else -> Routes.WELCOME
    }
    LaunchedEffect(Unit) { if (Repo.lastVersionSeen.isEmpty()) Repo.markVersionSeen(BuildConfig.VERSION_NAME) }
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        if (Api.enabled) {
            Remote.pull()
            while (true) { delay(60_000); Remote.pull() }
        }
    }
    LaunchedEffect(Repo.netError) {
        if (Repo.netError.isNotEmpty()) { Intents.toast(ctx, Repo.netError); Repo.netError = "" }
    }

    NavHost(nav, startDestination = start) {
        composable(Routes.LANG) { LanguageScreen(nav) }
        composable(Routes.WELCOME) { WelcomeScreen(nav) }
        composable(Routes.ROLE) { RoleScreen(nav) }
        composable(Routes.LOGIN) { LoginScreen(nav) }
        composable(Routes.REGISTER) { RegisterScreen(nav) }
        composable(Routes.FORGOT) { ForgotScreen(nav) }
        composable(Routes.OTP) { OtpScreen(nav) }
        composable("createpw/{mode}") { CreatePasswordScreen(nav, it.arguments?.getString("mode") ?: "register") }
        composable(Routes.SUBSCRIBE) { SubscribeScreen(nav) }
        composable(Routes.PAID) {
            SuccessScreen(t("تم الدفع بنجاح!", "Payment successful!"), t("يمكنك الآن الاستمتاع بجميع المزايا والخصائص.", "You can now enjoy all the features."),
                t("ابدأ الاستكشاف", "Start exploring")) { nav.clearTo(Routes.MAIN) }
        }
        composable(Routes.CREATED) {
            SuccessScreen(t("تم إنشاء الحساب بنجاح!", "Account created successfully!"), t("مرحبًا بك في بوابة العروض.", "Welcome to Bawabat Alorood."),
                t("ابدأ الاستكشاف", "Start exploring")) { nav.clearTo(Routes.MAIN) }
        }
        composable(Routes.MAIN) { MainScreen(nav) }
        composable(Routes.SEARCH) { SearchScreen(nav) }
        composable(Routes.FILTERS) { FiltersScreen(nav) }
        composable(Routes.PRIORITIES) { PrioritiesScreen(nav) }
        composable(Routes.AREAS) { AreasScreen(nav) }
        composable(Routes.ADVISOR) { AdvisorScreen(nav) }
        composable(Routes.BLOCKED) { BlockedScreen(nav) }
        composable("detail/{id}") { DetailScreen(nav, it.arguments?.getString("id") ?: "") }
        composable("floors/{id}") { FloorsScreen(nav, it.arguments?.getString("id") ?: "") }
        composable("clientinfo/{id}") { ClientInfoScreen(nav, it.arguments?.getString("id") ?: "") }
        composable("offer/{id}") { OfferScreen(nav, it.arguments?.getString("id") ?: "") }
        composable("booking/{id}") { BookingScreen(nav, it.arguments?.getString("id") ?: "") }
        composable("sent/{id}") { SentScreen(nav, it.arguments?.getString("id") ?: "") }
        composable("thread/{id}") { ThreadScreen(nav, it.arguments?.getString("id") ?: "") }
        composable(Routes.PROFILE_INFO) { ProfileInfoScreen(nav) }
        composable(Routes.CHANGE_PHONE) { ChangePhoneScreen(nav) }
        composable(Routes.CHANGE_PW) { ChangePasswordScreen(nav) }
        composable(Routes.LINKED) { LinkedScreen(nav) }
        composable(Routes.SETTINGS) { SettingsScreen(nav) }
        composable(Routes.CONTACT) { ContactScreen(nav) }
        composable(Routes.FAQ) { FaqScreen(nav) }
        composable("legal/{kind}") { LegalScreen(nav, it.arguments?.getString("kind") ?: "terms") }
        composable("soon/{kind}") { SoonScreen(nav, it.arguments?.getString("kind") ?: "news") }
        composable("result/{kind}/{ok}") {
            ResultScreen(nav, it.arguments?.getString("kind") ?: "pw", it.arguments?.getString("ok") == "true")
        }
        composable("adform/{id}") {
            val id = it.arguments?.getString("id") ?: "new"
            AdFormScreen(nav, if (id == "new") null else id)
        }
        composable(Routes.AD_SENT) { AdSentScreen(nav) }
        composable(Routes.RESERVATIONS) { ReservationsScreen(nav) }
        composable(Routes.NEWS) { NewsScreen(nav) }
    }

    // صيانة / تحديث إجباري (يُتحكم بهما من لوحة التحكم)
    val cfg = Repo.cfg
    if (Api.enabled && cfg.maintenance) {
        BlockingScreen(
            t("التطبيق تحت الصيانة", "Under maintenance"),
            (if (Repo.lang == "ar") cfg.maintenanceMessageAr else cfg.maintenanceMessageEn).ifBlank { t("نعود قريبًا.", "We will be back soon.") },
            null
        ) {}
    } else if (Api.enabled && BuildConfig.VERSION_CODE < cfg.minVersionCode) {
        BlockingScreen(
            t("يلزم تحديث التطبيق", "Update required"),
            t("هذا الإصدار لم يعد مدعومًا. حدّث التطبيق من Google Play للمتابعة.", "This version is no longer supported. Please update from Google Play to continue."),
            t("تحديث الآن", "Update now")
        ) { Intents.openUrl(ctx, "https://play.google.com/store/apps/details?id=${ctx.packageName}") }
    }

    // ما الجديد: يظهر مرة واحدة بعد كل تحديث
    val seen = Repo.lastVersionSeen
    val rel = Changelog.forVersion(BuildConfig.VERSION_NAME)
    var dismissedWhatsNew by remember { mutableStateOf(false) }
    if (seen.isNotEmpty() && seen != BuildConfig.VERSION_NAME && rel != null && !dismissedWhatsNew) {
        AlertDialog(
            onDismissRequest = { dismissedWhatsNew = true; Repo.markVersionSeen(BuildConfig.VERSION_NAME) },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(t("ما الجديد في ${rel.version}", "What's new in ${rel.version}")) },
            text = { Text((if (Repo.lang == "ar") rel.ar else rel.en).joinToString("\n") { "• $it" }) },
            confirmButton = {
                TextButton(onClick = { dismissedWhatsNew = true; Repo.markVersionSeen(BuildConfig.VERSION_NAME) }) { Text(t("حسنًا", "OK")) }
            }
        )
    }
    if (UpdateState.downloaded) {
        AlertDialog(
            onDismissRequest = { UpdateState.downloaded = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(t("تم تنزيل تحديث جديد", "An update was downloaded")) },
            text = { Text(t("أعد تشغيل التطبيق لإكمال التحديث.", "Restart the app to finish updating.")) },
            confirmButton = { TextButton(onClick = { UpdateState.complete() }) { Text(t("إعادة التشغيل", "Restart")) } },
            dismissButton = { TextButton(onClick = { UpdateState.downloaded = false }) { Text(t("لاحقًا", "Later")) } }
        )
    }
}

@Composable
fun BlockingScreen(title: String, body: String, button: String?, onButton: () -> Unit) {
    Surface(color = Ink, contentColor = Color.White, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Logo(80)
            VGap(24)
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            VGap(8)
            Text(body, textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.8f))
            if (button != null) {
                VGap(24)
                PrimaryButton(button, onButton)
            }
        }
    }
}
