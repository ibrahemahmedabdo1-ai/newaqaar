@file:OptIn(ExperimentalMaterial3Api::class)

package com.bawabat.alorood.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import com.bawabat.alorood.BuildConfig
import com.bawabat.alorood.data.Api
import com.bawabat.alorood.data.Demo
import com.bawabat.alorood.data.ImageStore
import com.bawabat.alorood.data.Offer
import com.bawabat.alorood.data.OfferStatus
import com.bawabat.alorood.data.Repo
import com.bawabat.alorood.data.Role
import com.bawabat.alorood.data.SearchPrefs
import com.bawabat.alorood.util.Intents
import com.bawabat.alorood.util.Validation
import androidx.compose.foundation.border

@Composable
fun ProfileAvatar(size: Int = 72) {
    val me = Repo.me
    if (me != null && me.avatar.isNotEmpty()) {
        FileImage(me.avatar, Modifier.size(size.dp).clip(CircleShape))
    } else {
        Box(Modifier.size(size.dp).clip(CircleShape).background(Ink), contentAlignment = Alignment.Center) {
            Text((me?.name ?: "؟").trim().take(1).uppercase(), color = Gold, fontSize = (size / 2.2).sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun roleLabel(r: Role, lang: String): String = when (r) {
    Role.BROKER -> if (lang == "ar") "سمسار" else "Broker"
    Role.DEVELOPER -> if (lang == "ar") "مطور عقاري" else "Developer"
    else -> if (lang == "ar") "مشتري" else "Buyer"
}

// ---------- طلباتي (مشتري / وسيط) ----------
@Composable
fun OffersTab(nav: NavHostController, onLogin: () -> Unit) {
    if (!Repo.loggedIn) {
        Column(Modifier.fillMaxSize()) { Header(t("طلباتي", "My offers"), null); GuestPrompt(onLogin) }
        return
    }
    var tab by remember { mutableStateOf(0) }
    val now = System.currentTimeMillis()
    val offers = Repo.myOffers()
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(Ink).statusBarsPadding().padding(16.dp)) {
            Text(t("طلباتي", "My offers"), color = Cream, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            VGap(10)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(t("المفضلة", "Favorites"), tab == 0) { tab = 0 }
                Pill(t("الحالية", "Current"), tab == 1) { tab = 1 }
                Pill(t("المعلقة", "Pending"), tab == 2) { tab = 2 }
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (tab) {
                0 -> {
                    val favs = Repo.myFavorites()
                    if (favs.isEmpty()) EmptyState(t("لم تضف أي عقار للمفضلة بعد.", "You have not added any favorites yet."))
                    else PropertyGrid(favs, { nav.navigate(Routes.detail(it.id)) }, onLogin, showCommission = Repo.role == Role.BROKER)
                }
                1 -> {
                    val cur = offers.filter { it.status == OfferStatus.ACTIVE || it.status == OfferStatus.REJECTED }
                    if (cur.isEmpty()) EmptyState(t("لا توجد عروض حالية.", "No current offers."))
                    cur.forEach { OfferCard(it, now) { nav.navigate(Routes.detail(it.propertyId)) } }
                }
                else -> {
                    val pend = offers.filter { it.status == OfferStatus.PENDING }
                    if (pend.isEmpty()) EmptyState(t("لا توجد عروض معلقة.", "No pending offers."))
                    pend.forEach { OfferCard(it, now) { nav.navigate(Routes.detail(it.propertyId)) } }
                }
            }
        }
    }
}

@Composable
fun OfferCard(o: Offer, now: Long, onClick: () -> Unit) {
    val p = Repo.findProperty(o.propertyId)
    val left = Validation.daysLeft(o.createdAt, now, Repo.HOLD_DAYS)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (p != null) PropertyImage(p, Modifier.size(76.dp).clip(RoundedCornerShape(10.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("SAR " + Validation.formatPrice(o.amount), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (p != null) Text(t("حي ${p.district} , ${p.city}", "${p.district}, ${p.city}"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(o.floor, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (o.status) {
                OfferStatus.REJECTED -> Text(t("تم رفض العرض — سيُسترد مبلغ الحجز", "Offer rejected — booking amount refunded"), color = Danger, fontSize = 12.sp)
                else -> Text(if (left > 0) t("متبقي $left أيام", "$left days left") else t("انتهت المهلة", "Time elapsed"), color = Brown, fontSize = 12.sp)
            }
        }
    }
}

// ---------- الملف الشخصي ----------
@Composable
fun ProfileTab(nav: NavHostController, onLogin: () -> Unit) {
    val ctx = LocalContext.current
    var logout by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val me = Repo.me
    Column(Modifier.fillMaxSize()) {
        Header(t("الملف الشخصي", "Profile"), null)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (me == null) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(t("سجل الدخول الآن للتواصل مع الشركات مباشرة وإتمام صفقاتك بسهولة.", "Sign in to contact companies directly and complete your deals."), textAlign = TextAlign.Center)
                    VGap(12)
                    PrimaryButton(t("تسجيل الدخول", "Sign in"), onLogin)
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProfileAvatar(72)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(me.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(me.phone, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(roleLabel(me.role, Repo.lang), color = Brown, fontSize = 13.sp)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Row2(t("معلومات الملف الشخصي", "Profile information"), { nav.navigate(Routes.PROFILE_INFO) })
                Row2(t("تغيير رقم الهاتف", "Change phone number"), { nav.navigate(Routes.CHANGE_PHONE) })
                Row2(t("تغيير كلمة المرور", "Change password"), { nav.navigate(Routes.CHANGE_PW) })
                Row2(t("الحسابات المرتبطة", "Linked accounts"), { nav.navigate(Routes.LINKED) })
                if (me.role == Role.DEVELOPER) Row2(t("الحجوزات", "Reservations"), { nav.navigate(Routes.RESERVATIONS) })
            }
            Row2(t("الإعدادات", "Settings"), { nav.navigate(Routes.SETTINGS) })
            Row2(t("تواصل معنا", "Contact us"), { nav.navigate(Routes.CONTACT) })
            Row2(t("الأسئلة الشائعة", "FAQ"), { nav.navigate(Routes.FAQ) })
            Row2(t("الشروط والأحكام", "Terms & Conditions"), { nav.navigate(Routes.legal("terms")) })
            Row2(t("سياسة الخصوصية", "Privacy policy"), { nav.navigate(Routes.legal("privacy")) })
            Row2(t("عن بوابة العروض", "About Bawabat Alorood"), { nav.navigate(Routes.legal("about")) })
            Row2(t("شارك التطبيق", "Share the app"), {
                Intents.share(ctx, t("جرّب تطبيق بوابة العروض: https://play.google.com/store/apps/details?id=${ctx.packageName}", "Try Bawabat Alorood: https://play.google.com/store/apps/details?id=${ctx.packageName}"))
            })
            if (me != null) {
                Row2(t("تسجيل الخروج", "Sign out"), { logout = true })
                Row2(t("حذف الحساب", "Delete account"), { delete = true }, danger = true)
            }
            Text("v${BuildConfig.VERSION_NAME}", Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center, color = Gray2, fontSize = 12.sp)
        }
    }
    if (logout) ConfirmDialog(
        title = t("هل أنت متأكد من تسجيل الخروج؟", "Are you sure you want to sign out?"), confirm = t("تسجيل الخروج", "Sign out"),
        onConfirm = { logout = false; Repo.logout(); nav.clearTo(Routes.WELCOME) }, onDismiss = { logout = false }
    )
    if (delete) {
        var pw by remember { mutableStateOf("") }
        var delErr by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { delete = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(t("حذف الحساب", "Delete account")) },
            text = {
                Column {
                    Text(t("سيتم حذف حسابك وإعلاناتك وطلباتك ومحادثاتك نهائيًا ولا يمكن التراجع.", "Your account, ads, offers and chats will be permanently deleted. This cannot be undone."))
                    if (Api.enabled) {
                        VGap(8)
                        AppTextField(pw, { pw = it; delErr = "" }, password = true, placeholder = t("كلمة المرور للتأكيد", "Password to confirm"))
                    }
                    if (delErr.isNotEmpty()) ErrorText(delErr)
                }
            },
            confirmButton = {
                TextButton(enabled = !busy && (!Api.enabled || pw.isNotEmpty()), onClick = {
                    scope.launch {
                        busy = true
                        val e = Repo.doDeleteAccount(pw)
                        busy = false
                        if (e == null) { delete = false; nav.clearTo(Routes.WELCOME) } else delErr = e
                    }
                }) { Text(t("حذف نهائيًا", "Delete permanently"), color = Danger) }
            },
            dismissButton = { TextButton(onClick = { delete = false }) { Text(t("إلغاء", "Cancel")) } }
        )
    }
}

// ---------- القائمة المركزية ----------
@Composable
fun MenuSheet(nav: NavHostController, onDismiss: () -> Unit, onHome: () -> Unit) {
    val ctx = LocalContext.current
    val lang = Repo.lang
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Box(Modifier.padding(horizontal = 16.dp)) {
                PrimaryButton(t("مستشارك الذكي", "Your smart advisor"), { onDismiss(); nav.navigate(Routes.ADVISOR) })
            }
            VGap(12)
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Demo.types.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { ty ->
                            Box(Modifier.weight(1f)) {
                                Pill(if (lang == "ar") ty.ar else ty.en, false) {
                                    Repo.setPrefs(SearchPrefs(types = setOf(ty.key))); onDismiss(); onHome()
                                }
                            }
                        }
                    }
                }
            }
            VGap(8)
            Row2(t("تواصل معنا", "Contact us"), { onDismiss(); nav.navigate(Routes.CONTACT) })
            Row2(t("الشؤون القانونية", "Legal affairs"), { onDismiss(); nav.navigate(Routes.legal("terms")) })
            Row2(t("أخبار العقارات", "Real-estate news"), { onDismiss(); nav.navigate(Routes.NEWS) })
            Row2(t("البورصة العقارية", "Real-estate exchange"), { onDismiss(); nav.navigate(Routes.soon("exchange")) })
            Row2(t("الأسئلة الشائعة", "FAQ"), { onDismiss(); nav.navigate(Routes.FAQ) })
            Row2(t("الإعدادات", "Settings"), { onDismiss(); nav.navigate(Routes.SETTINGS) })
            Row2(t("شارك التطبيق", "Share the app"), {
                onDismiss()
                Intents.share(ctx, "https://play.google.com/store/apps/details?id=${ctx.packageName}")
            })
        }
    }
}

@Composable
fun SoonScreen(nav: NavHostController, kind: String) {
    Screen(if (kind == "news") t("أخبار العقارات", "Real-estate news") else t("البورصة العقارية", "Real-estate exchange"), { nav.popBackStack() }) {
        EmptyState(t("هذه الخدمة قيد التطوير وستتوفر قريبًا.", "This service is under development and will be available soon."), Modifier.weight(1f))
    }
}

// ---------- معلومات الملف الشخصي ----------
@Composable
fun ProfileInfoScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    val me = Repo.me
    var name by remember { mutableStateOf(me?.name ?: "") }
    val failMsg = t("تعذر حفظ الصورة", "Could not save the photo")
    val saved = t("تم الحفظ", "Saved")
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val path = ImageStore.save(ctx, uri, "avatar_" + Repo.uid())
            if (path != null) {
                Repo.me?.avatar?.takeIf { it.isNotEmpty() }?.let { ImageStore.delete(it) }
                Repo.updateMe { it.copy(avatar = path) }
            } else Intents.toast(ctx, failMsg)
        }
    }
    Screen(t("معلومات الملف الشخصي", "Profile information"), { nav.popBackStack() }, bottom = {
        Column(Modifier.padding(16.dp)) {
            PrimaryButton(t("حفظ", "Save"), { Repo.updateMe { it.copy(name = name.trim()) }; Intents.toast(ctx, saved) }, enabled = name.isNotBlank() && name.trim() != me?.name)
        }
    }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ProfileAvatar(96)
            TextButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text(t("إضافة صورة", "Add photo")) }
            VGap(8)
            Column(Modifier.fillMaxWidth()) {
                Label(t("الإسم", "Name"))
                AppTextField(name, { name = it.take(60) })
                VGap(8)
                Label(t("رقم الهاتف", "Phone number"))
                AppTextField(me?.phone ?: "", {}, enabled = false)
            }
        }
    }
}

// ---------- تغيير رقم الهاتف ----------
@Composable
fun ChangePhoneScreen(nav: NavHostController) {
    var code by remember { mutableStateOf(Repo.regCode) }
    var number by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var sheet by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Screen(t("تغيير رقم الهاتف", "Change phone number"), { nav.popBackStack() }, bottom = {
        Column(Modifier.padding(16.dp)) {
            PrimaryButton(t("التالي", "Next"), {
                val full = Validation.fullPhone(code, number)
                scope.launch {
                    loading = true
                    val r = Repo.doRequestOtp(full, "change_phone")
                    loading = false
                    if (r.error != null) error = r.error
                    else { Repo.newPhone = full; Repo.otpPurpose = "phone"; nav.navigate(Routes.OTP) }
                }
            }, enabled = Validation.isValidPhone(number), loading = loading)
        }
    }) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Label(t("رقم الهاتف الجديد", "New phone number"))
            PhoneField(code, { sheet = true }, number, { number = it; error = "" }, error.isNotEmpty(), onDark = false)
            if (error.isNotEmpty()) ErrorText(error)
        }
    }
    if (sheet) CountrySheet({ code = it.code; sheet = false }, { sheet = false })
}

// ---------- تغيير كلمة المرور ----------
@Composable
fun ChangePasswordScreen(nav: NavHostController) {
    var cur by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    var weak by remember { mutableStateOf(false) }
    var mismatch by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Screen(t("تغيير كلمة المرور", "Change password"), { nav.popBackStack() }, bottom = {
        Column(Modifier.padding(16.dp)) {
            PrimaryButton(t("تغيير", "Change"), {
                if (Validation.passwordError(pw) != null) {
                    weak = true
                } else if (pw != pw2) {
                    mismatch = true
                } else {
                    scope.launch {
                        loading = true
                        val err = Repo.doChangePassword(cur, pw)
                        loading = false
                        nav.navigate(Routes.result("pw", err == null))
                    }
                }
            }, enabled = cur.isNotEmpty() && pw.isNotEmpty() && pw2.isNotEmpty(), loading = loading)
        }
    }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Label(t("كلمة المرور الحالية", "Current password"))
            AppTextField(cur, { cur = it }, password = true)
            VGap(8)
            Label(t("كلمة المرور", "New password"))
            AppTextField(pw, { pw = it; weak = false; mismatch = false }, password = true, error = weak)
            if (weak) ErrorText(t("٨ أحرف على الأقل وتحتوي على حروف وأرقام", "At least 8 characters with letters and digits"))
            VGap(8)
            Label(t("تأكيد كلمة المرور", "Confirm password"))
            AppTextField(pw2, { pw2 = it; mismatch = false }, password = true, error = mismatch)
            if (mismatch) ErrorText(t("القيم غير متطابقة", "Values do not match"))
        }
    }
}

@Composable
fun ResultScreen(nav: NavHostController, kind: String, ok: Boolean) {
    val title = when {
        !ok -> t("حدث خطأ برجاء المحاولة مرة أخرى", "Something went wrong, please try again")
        kind == "phone" -> t("تم تغيير رقم الهاتف بنجاح", "Phone number changed successfully")
        else -> t("تم تغيير كلمة المرور بنجاح", "Password changed successfully")
    }
    AuthScreen(onBack = null) {
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(if (ok) Gold else Danger), contentAlignment = Alignment.Center) {
                Text(if (ok) "✓" else "✕", color = if (ok) Ink else Color.White, fontSize = 50.sp, fontWeight = FontWeight.Bold)
            }
            VGap(24)
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        PrimaryButton(if (ok) t("الرجوع", "Back") else t("المحاولة مرة أخرى", "Try again"), {
            if (ok) nav.popBackStack(Routes.MAIN, false) else nav.popBackStack()
        })
    }
}

// ---------- الحسابات المرتبطة ----------
@Composable
fun LinkedScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    val msg = t("ربط الحسابات يتطلب إعداد OAuth ولم يُفعَّل في هذه النسخة", "Linking accounts needs OAuth setup and is not enabled in this build")
    Screen(t("الحسابات المرتبطة", "Linked accounts"), { nav.popBackStack() }) {
        listOf("Google account", "X account", "Apple account").forEach { n ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(n, Modifier.weight(1f), fontSize = 16.sp)
                TextButton(onClick = { Intents.toast(ctx, msg) }) { Text(t("ربط", "Link")) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}

// ---------- الإعدادات ----------
@Composable
fun SettingsScreen(nav: NavHostController) {
    Screen(t("الإعدادات", "Settings"), { nav.popBackStack() }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t("الإشعارات", "Notifications"), fontSize = 16.sp)
                    Text(t("الإشعارات الفورية تتطلب ربط الخادم", "Push notifications require the server"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(Repo.notifications, { Repo.setNotif(it) }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Text(t("اللغة", "Language"), Modifier.padding(20.dp, 16.dp, 20.dp, 4.dp), fontWeight = FontWeight.Medium, fontSize = 16.sp)
            listOf("ar" to "العربية", "en" to "English").forEach { (k, l) ->
                Row(Modifier.fillMaxWidth().clickable { Repo.setLanguage(k) }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(Repo.lang == k, { Repo.setLanguage(k) }); Text(l)
                }
            }
            HorizontalDivider(Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outline)
            Text(t("السمة", "Theme"), Modifier.padding(20.dp, 16.dp, 20.dp, 4.dp), fontWeight = FontWeight.Medium, fontSize = 16.sp)
            listOf("system" to t("سمة الجهاز", "Device theme"), "light" to t("فاتح", "Light"), "dark" to t("داكن", "Dark")).forEach { (k, l) ->
                Row(Modifier.fillMaxWidth().clickable { Repo.setTheme(k) }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(Repo.themeMode == k, { Repo.setTheme(k) }); Text(l)
                }
            }
        }
    }
}

// ---------- تواصل معنا ----------
@Composable
fun ContactScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val supportEmail = Repo.cfg.supportEmail.ifBlank { BuildConfig.SUPPORT_EMAIL }
    val supportWa = Repo.cfg.supportWhatsapp.ifBlank { BuildConfig.SUPPORT_WHATSAPP }
    val sentMsg = t("تم إرسال رسالتك وسنرد عليك قريبًا", "Your message was sent, we will reply soon")
    val noEmail = t("لم يتم ضبط بريد الدعم بعد (SUPPORT_EMAIL)", "Support email is not configured yet (SUPPORT_EMAIL)")
    val noWa = t("لم يتم ضبط رقم واتس آب للدعم بعد (SUPPORT_WHATSAPP)", "Support WhatsApp number is not configured yet (SUPPORT_WHATSAPP)")
    Screen(t("تواصل معنا", "Contact us"), { nav.popBackStack() }, bottom = {
        Column(Modifier.padding(16.dp)) {
            PrimaryButton(t("إرسال", "Send"), {
                if (Api.enabled) {
                    scope.launch {
                        loading = true
                        val err = Repo.doSupport(subject.trim(), body.trim())
                        loading = false
                        if (err == null) { Intents.toast(ctx, sentMsg); subject = ""; body = "" } else Intents.toast(ctx, err)
                    }
                } else if (supportEmail.isBlank()) Intents.toast(ctx, noEmail)
                else Intents.email(ctx, supportEmail, subject.trim(), body.trim())
            }, enabled = subject.isNotBlank() && body.isNotBlank(), loading = loading)
        }
    }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(t("إرسال سؤال", "Send a question"), fontSize = 18.sp, fontWeight = FontWeight.Medium)
            VGap(8)
            Label(t("العنوان", "Subject"))
            AppTextField(subject, { subject = it.take(100) })
            VGap(8)
            Label(t("الرسالة", "Message"))
            AppTextField(body, { body = it.take(2000) }, singleLine = false, minLines = 5)
            VGap(24)
            Text(t("أو يمكنك التواصل معنا مباشرة", "Or contact us directly"), fontSize = 16.sp)
            VGap(8)
            PrimaryButton(t("واتس آب", "WhatsApp"), {
                if (supportWa.isBlank()) Intents.toast(ctx, noWa) else Intents.whatsapp(ctx, supportWa)
            }, outlined = true)
            if (Repo.tickets.isNotEmpty()) {
                VGap(24)
                Text(t("رسائلك السابقة", "Your previous messages"), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Repo.tickets.forEach { tk ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(tk.subject, fontWeight = FontWeight.Bold)
                        if (tk.reply.isNotEmpty()) Text(t("الرد: ", "Reply: ") + tk.reply, color = Brown)
                        else Text(t("قيد المراجعة", "Pending"), color = Gray2, fontSize = 12.sp)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

// ---------- الأسئلة الشائعة ----------
@Composable
fun FaqScreen(nav: NavHostController) {
    var open by remember { mutableStateOf(-1) }
    val ar = Repo.lang == "ar"
    Screen(t("الأسئلة الشائعة", "FAQ"), { nav.popBackStack() }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            val faqs = if (Repo.remoteFaqs.isNotEmpty()) Repo.remoteFaqs.map { Demo.Faq(it.qAr, it.aAr, it.qEn.ifEmpty { it.qAr }, it.aEn.ifEmpty { it.aAr }) } else Demo.faqs
            faqs.forEachIndexed { i, f ->
                Column(Modifier.fillMaxWidth().clickable { open = if (open == i) -1 else i }.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text(if (ar) f.qAr else f.qEn, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    if (open == i) { VGap(8); Text(if (ar) f.aAr else f.aEn, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

// ---------- الشروط / الخصوصية / عن التطبيق ----------
@Composable
fun LegalScreen(nav: NavHostController, kind: String) {
    val ctx = LocalContext.current
    val lang = Repo.lang
    val title = when (kind) {
        "privacy" -> t("سياسة الخصوصية", "Privacy policy")
        "about" -> t("عن بوابة العروض", "About Bawabat Alorood")
        else -> t("الشروط والأحكام", "Terms & Conditions")
    }
    val remote = Repo.remotePages[kind]
    val body = remote?.let { if (lang == "ar") it.bodyAr else it.bodyEn.ifEmpty { it.bodyAr } }?.takeIf { it.isNotBlank() }
        ?: when (kind) { "privacy" -> Demo.privacy(lang); "about" -> Demo.about(lang); else -> Demo.terms(lang) }
    Screen(title, { nav.popBackStack() }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(body, fontSize = 15.sp, lineHeight = 24.sp)
            if (kind == "privacy") {
                VGap(16)
                if (BuildConfig.PRIVACY_URL.isNotBlank()) PrimaryButton(t("فتح السياسة على الويب", "Open policy on the web"), { Intents.openUrl(ctx, BuildConfig.PRIVACY_URL) }, outlined = true)
                if (BuildConfig.ACCOUNT_DELETION_URL.isNotBlank()) { VGap(8); PrimaryButton(t("طلب حذف الحساب عبر الويب", "Request account deletion on the web"), { Intents.openUrl(ctx, BuildConfig.ACCOUNT_DELETION_URL) }, outlined = true) }
            }
            if (kind == "about") { VGap(16); Text("v${BuildConfig.VERSION_NAME}", color = Gray2) }
        }
    }
}

// ---------- أخبار العقارات (من لوحة التحكم) ----------
@Composable
fun NewsScreen(nav: NavHostController) {
    val ar = Repo.lang == "ar"
    Screen(t("أخبار العقارات", "Real-estate news"), { nav.popBackStack() }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (Repo.news.isEmpty()) EmptyState(t("لا توجد أخبار حاليًا.", "No news right now."))
            Repo.news.forEach { n ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp)) {
                    if (n.imageUrl.isNotEmpty()) {
                        FileImage(n.imageUrl, Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(10.dp)))
                        VGap(8)
                    }
                    Text(if (ar) n.titleAr else n.titleEn.ifEmpty { n.titleAr }, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    VGap(4)
                    Text(if (ar) n.bodyAr else n.bodyEn.ifEmpty { n.bodyAr }, fontSize = 14.sp)
                }
            }
        }
    }
}
