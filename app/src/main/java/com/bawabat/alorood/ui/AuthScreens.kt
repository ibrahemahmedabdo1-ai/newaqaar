package com.bawabat.alorood.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.bawabat.alorood.BuildConfig
import com.bawabat.alorood.R
import com.bawabat.alorood.data.Api
import com.bawabat.alorood.data.Repo
import com.bawabat.alorood.data.Role
import com.bawabat.alorood.util.Intents
import com.bawabat.alorood.util.Validation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun Logo(size: Int = 72) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).border(2.dp, Gold, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(painterResource(R.drawable.bw_logo), contentDescription = null, modifier = Modifier.size((size * 0.7f).dp))
    }
}

@Composable
private fun SocialRow() {
    val ctx = LocalContext.current
    val msg = t("تسجيل الدخول الاجتماعي يتطلب إعداد OAuth ولم يُفعَّل في هذه النسخة", "Social sign-in needs OAuth setup and is not enabled in this build")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)) {
        listOf("G", "X", "A").forEach { s ->
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(Color.White).clickable { Intents.toast(ctx, msg) },
                contentAlignment = Alignment.Center
            ) { Text(s, color = Ink, fontWeight = FontWeight.Bold, fontSize = 22.sp) }
        }
    }
}

@Composable
private fun DividerWithText(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.6f))
        Text(text, modifier = Modifier.padding(horizontal = 12.dp), fontSize = 13.sp)
        HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.6f))
    }
}

// ---------- اللغة ----------
@Composable
fun LanguageScreen(nav: NavHostController) {
    AuthScreen(onBack = null) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Logo(88)
            VGap(24)
            Text("اختر اللغة · Choose language", fontSize = 20.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            VGap(24)
            PrimaryButton("اللغة العربية", { Repo.setLanguage("ar"); nav.clearTo(Routes.WELCOME) })
            VGap(12)
            PrimaryButton("English", { Repo.setLanguage("en"); nav.clearTo(Routes.WELCOME) }, outlined = true)
        }
    }
}

// ---------- الترحيب ----------
@Composable
fun WelcomeScreen(nav: NavHostController) {
    AuthScreen(onBack = null) {
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Logo(96)
            VGap(16)
            Text(t("بوابة العروض", "Bawabat Alorood"), color = Peach, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            VGap(12)
            Text(t("جميع العقارات من الملاك مباشرة", "All properties direct from owners"), fontSize = 18.sp, textAlign = TextAlign.Center)
            VGap(6)
            Text(t("من أنت؟ عميل، سمسار، أم مطور عقاري؟ اختر مسارك!", "Who are you? A client, a broker or a developer? Choose your path!"), fontSize = 14.sp, textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.8f))
        }
        PrimaryButton(t("تسجيل الدخول", "Sign in"), { nav.navigate(Routes.LOGIN) })
        VGap(10)
        PrimaryButton(t("إنشاء حساب", "Create account"), { nav.navigate(Routes.ROLE) }, outlined = true)
        VGap(4)
        TextButton(onClick = { Repo.guest = true; nav.clearTo(Routes.MAIN) }, modifier = Modifier.fillMaxWidth()) {
            Text(t("الدخول كضيف", "Continue as guest"), color = Gold, fontSize = 15.sp)
        }
        TextButton(onClick = { nav.navigate(Routes.LANG) }, modifier = Modifier.fillMaxWidth()) {
            Text(t("اللغة: العربية · English", "Language: العربية · English"), color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
        }
    }
}

// ---------- اختيار المسار ----------
@Composable
fun RoleScreen(nav: NavHostController) {
    AuthScreen(onBack = { nav.popBackStack() }) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(t("ابدأ باختيار مسارك", "Start by choosing your path"), fontSize = 26.sp, fontWeight = FontWeight.Bold)
            VGap(20)
            val items = listOf(
                Triple(Role.CLIENT, t("مشتري", "Buyer"), t("تصفح واعثر على عقارك المثالي بكل سهولة", "Browse and find your ideal property easily")),
                Triple(Role.BROKER, t("سمسار", "Broker"), t("تواصل مع العملاء واستعرض قوائم عقاراتك", "Connect with clients and browse your property lists")),
                Triple(Role.DEVELOPER, t("مطور عقاري", "Real-estate developer"), t("روّج لمشروعاتك واصل لجمهورك المستهدف", "Promote your projects and reach your target audience"))
            )
            items.forEach { (role, title, sub) ->
                Column(
                    Modifier.fillMaxWidth().padding(bottom = 14.dp).clip(RoundedCornerShape(20.dp)).background(Cream)
                        .clickable { Repo.regRole = role; nav.navigate(Routes.REGISTER) }.padding(20.dp)
                ) {
                    Text(title, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    VGap(4)
                    Text(sub, color = Gray2, fontSize = 14.sp)
                }
            }
        }
    }
}

// ---------- تسجيل الدخول ----------
@Composable
fun LoginScreen(nav: NavHostController) {
    var code by remember { mutableStateOf(Repo.regCode) }
    var number by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var errMsg by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var sheet by remember { mutableStateOf(false) }
    AuthScreen(onBack = { nav.popBackStack() }) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Logo(44); Spacer(Modifier.width(10.dp))
                Text(t("بوابة العروض", "Bawabat Alorood"), color = Peach, fontSize = 18.sp)
            }
            VGap(16)
            Text(t("مرحبا بعودتك", "Welcome back"), fontSize = 32.sp, fontWeight = FontWeight.Bold)
            VGap(8)
            if (error) ErrorText(errMsg.ifEmpty { t("خطأ في رقم الهاتف أو كلمة المرور", "Wrong phone number or password") })
            VGap(8)
            Label(t("رقم الهاتف", "Phone number"))
            PhoneField(code, { sheet = true }, number, { number = it; error = false }, error)
            VGap(8)
            Label(t("كلمة المرور", "Password"))
            AppTextField(pw, { pw = it; error = false }, password = true, onDark = true, error = error, placeholder = "••••••••")
            TextButton(onClick = { nav.navigate(Routes.FORGOT) }) { Text(t("هل نسيت كلمة المرور؟", "Forgot password?"), color = Gold, fontSize = 13.sp) }
            VGap(12)
            PrimaryButton(t("تسجيل الدخول", "Sign in"), {
                val full = Validation.fullPhone(code, number)
                if (!Validation.isValidPhone(number)) {
                    error = true; errMsg = ""
                } else {
                    scope.launch {
                        loading = true
                        val err = Repo.doLogin(full, pw)
                        loading = false
                        if (err == null) nav.clearTo(Routes.MAIN) else { error = true; errMsg = err }
                    }
                }
            }, enabled = number.isNotEmpty() && pw.isNotEmpty(), loading = loading)
            VGap(20)
            DividerWithText(t("أو قم بتسجيل الدخول باستخدام", "Or sign in with"))
            VGap(16)
            SocialRow()
            VGap(20)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(t("ليس لديك حساب؟", "Don't have an account?"), fontSize = 14.sp)
                TextButton(onClick = { nav.navigate(Routes.ROLE) }) { Text(t("سجل الآن", "Register now"), color = Gold, fontWeight = FontWeight.Bold) }
            }
        }
    }
    if (sheet) CountrySheet({ code = it.code; Repo.regCode = it.code; sheet = false }, { sheet = false })
}

// ---------- نسيت كلمة المرور ----------
@Composable
fun ForgotScreen(nav: NavHostController) {
    var code by remember { mutableStateOf(Repo.regCode) }
    var number by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var sheet by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val notFound = t("هذا الرقم غير مسجل", "This number is not registered")
    AuthScreen(onBack = { nav.popBackStack() }) {
        Text(t("استعادة كلمة المرور", "Reset password"), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        VGap(8)
        Text(t("أدخل رقم هاتفك وسنرسل لك رمز التأكيد.", "Enter your phone number and we will send you a verification code."), color = Color.White.copy(alpha = 0.8f))
        VGap(20)
        PhoneField(code, { sheet = true }, number, { number = it; error = "" }, error.isNotEmpty())
        if (error.isNotEmpty()) ErrorText(error)
        Spacer(Modifier.weight(1f))
        PrimaryButton(t("التالي", "Next"), {
            val full = Validation.fullPhone(code, number)
            if (!Validation.isValidPhone(number)) {
                error = notFound
            } else {
                scope.launch {
                    loading = true
                    val r = Repo.doRequestOtp(full, "reset")
                    loading = false
                    if (r.error != null) error = r.error
                    else { Repo.forgotPhone = full; Repo.otpPurpose = "forgot"; nav.navigate(Routes.OTP) }
                }
            }
        }, enabled = number.isNotEmpty(), loading = loading)
    }
    if (sheet) CountrySheet({ code = it.code; Repo.regCode = it.code; sheet = false }, { sheet = false })
}

// ---------- إنشاء حساب ----------
@Composable
fun RegisterScreen(nav: NavHostController) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf(Repo.regCode) }
    var number by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var sheet by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val invalid = t("تأكد من الاسم ورقم الهاتف", "Check the name and phone number")
    AuthScreen(onBack = { nav.popBackStack() }) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(t("إنشاء حساب", "Create account"), fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                when (Repo.regRole) { Role.BROKER -> t("سمسار", "Broker"); Role.DEVELOPER -> t("مطور عقاري", "Developer"); else -> t("مشتري", "Buyer") },
                color = Gold, fontSize = 14.sp
            )
            VGap(16)
            Label(t("رقم الهاتف", "Phone number"))
            PhoneField(code, { sheet = true }, number, { number = it; error = "" }, error.isNotEmpty())
            VGap(8)
            Label(t("الإسم", "Name"))
            AppTextField(name, { name = it.take(60); error = "" }, onDark = true, placeholder = t("الإسم", "Name"))
            if (error.isNotEmpty()) ErrorText(error)
            VGap(16)
            PrimaryButton(t("التالي", "Next"), {
                val full = Validation.fullPhone(code, number)
                if (name.isBlank() || !Validation.isValidPhone(number)) {
                    error = invalid
                } else {
                    scope.launch {
                        loading = true
                        val r = Repo.doRequestOtp(full, "register")
                        loading = false
                        if (r.error != null) error = r.error
                        else {
                            Repo.regName = name.trim(); Repo.regCode = code; Repo.regPhone = full
                            Repo.otpPurpose = "register"; nav.navigate(Routes.OTP)
                        }
                    }
                }
            }, enabled = name.isNotBlank() && number.isNotEmpty(), loading = loading)
            VGap(20)
            DividerWithText(t("أو قم بإنشاء حساب باستخدام", "Or create an account with"))
            VGap(16)
            SocialRow()
            VGap(16)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(t("لديك حساب بالفعل؟", "Already have an account?"), fontSize = 14.sp)
                TextButton(onClick = { nav.navigate(Routes.LOGIN) }) { Text(t("تسجيل الدخول", "Sign in"), color = Gold, fontWeight = FontWeight.Bold) }
            }
        }
    }
    if (sheet) CountrySheet({ code = it.code; Repo.regCode = it.code; sheet = false }, { sheet = false })
}

// ---------- رمز التأكيد ----------
@Composable
fun OtpScreen(nav: NavHostController) {
    var value by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(30) }
    var resendKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val purpose = Repo.otpPurpose
    val phoneFull = when (purpose) { "register" -> Repo.regPhone; "forgot" -> Repo.forgotPhone; else -> Repo.newPhone }
    val apiPurpose = when (purpose) { "forgot" -> "reset"; "phone" -> "change_phone"; else -> "register" }
    LaunchedEffect(resendKey) {
        seconds = 30
        while (seconds > 0) { delay(1000); seconds-- }
    }
    AuthScreen(onBack = { nav.popBackStack() }) {
        Text(t("تأكيد رقم الهاتف", "Confirm phone number"), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        VGap(8)
        Text(t("لقد أرسلنا لك رسالة تحتوي على رمز التأكيد. يرجى إدخاله أدناه.", "We sent you a message with a verification code. Please enter it below."), color = Color.White.copy(alpha = 0.8f))
        if (!Api.enabled) {
            VGap(6)
            Text(t("وضع تجريبي: لا يتم إرسال SMS حقيقي. الرمز هو ${Repo.DEMO_OTP}", "Demo mode: no real SMS is sent. The code is ${Repo.DEMO_OTP}"), color = Gold, fontSize = 13.sp)
        } else if (Repo.otpDebug.isNotEmpty()) {
            VGap(6)
            Text(t("وضع اختبار الخادم (OTP_DEBUG): الرمز ${Repo.otpDebug}", "Server test mode (OTP_DEBUG): code ${Repo.otpDebug}"), color = Gold, fontSize = 13.sp)
        }
        VGap(28)
        OtpField(value, { value = it; errorMsg = "" }, 4, errorMsg.isNotEmpty())
        if (errorMsg.isNotEmpty()) { VGap(8); ErrorText(errorMsg) }
        VGap(16)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(t("لم تتلق الرسالة؟", "Didn't get it?"), fontSize = 14.sp)
            if (seconds > 0) Text("  " + String.format(Locale.US, "0 : %02d", seconds), color = Gold)
            else TextButton(onClick = {
                scope.launch {
                    val r = Repo.doRequestOtp(phoneFull, apiPurpose)
                    if (r.error != null) errorMsg = r.error else { resendKey++; value = ""; errorMsg = "" }
                }
            }) { Text(t("إرسال مرة أخرى", "Resend"), color = Gold) }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(t("التالي", "Next"), {
            scope.launch {
                loading = true
                val err = Repo.doVerifyOtp(phoneFull, apiPurpose, value)
                if (err != null) {
                    errorMsg = err
                } else {
                    when (purpose) {
                        "register" -> nav.navigate(Routes.createPw("register"))
                        "forgot" -> nav.navigate(Routes.createPw("reset"))
                        "phone" -> {
                            val e2 = Repo.doChangePhone(phoneFull)
                            if (e2 == null) nav.navigate(Routes.phoneResult(true)) { popUpTo(Routes.MAIN) }
                            else nav.navigate(Routes.phoneResult(false)) { popUpTo(Routes.MAIN) }
                        }
                    }
                }
                loading = false
            }
        }, enabled = value.length == 4, loading = loading)
    }
}

// ---------- إنشاء كلمة المرور ----------
@Composable
fun CreatePasswordScreen(nav: NavHostController, mode: String) {
    var pw by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    var agree by remember { mutableStateOf(false) }
    var mismatch by remember { mutableStateOf(false) }
    var weak by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var serverErr by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val isReset = mode == "reset"
    val toastDone = t("تم تغيير كلمة المرور بنجاح", "Password changed successfully")
    AuthScreen(onBack = { nav.popBackStack() }) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(t("إنشاء كلمة المرور", "Create password"), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            VGap(20)
            Label(t("كلمة المرور", "Password"))
            AppTextField(pw, { pw = it; weak = false; mismatch = false }, password = true, onDark = true, error = weak, placeholder = "••••••••")
            if (weak) ErrorText(t("٨ أحرف على الأقل وتحتوي على حروف وأرقام", "At least 8 characters with letters and digits"))
            VGap(8)
            Label(t("تأكيد كلمة المرور", "Confirm password"))
            AppTextField(pw2, { pw2 = it; mismatch = false }, password = true, onDark = true, error = mismatch, placeholder = "••••••••")
            if (mismatch) ErrorText(t("القيم غير متطابقة", "Values do not match"))
            if (serverErr.isNotEmpty()) ErrorText(serverErr)
            VGap(12)
            if (!isReset) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(agree, { agree = it }, colors = CheckboxDefaults.colors(checkedColor = Gold, checkmarkColor = Ink, uncheckedColor = Color.White))
                    Text(t("أوافق على ", "I agree to the "), fontSize = 14.sp)
                    TextButton(onClick = { nav.navigate(Routes.legal("terms")) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Text(t("الشروط و الأحكام", "Terms & Conditions"), color = Gold, fontSize = 14.sp)
                    }
                }
            }
            VGap(16)
            PrimaryButton(if (isReset) t("تغيير", "Change") else t("إنشاء حساب", "Create account"), {
                if (Validation.passwordError(pw) != null) {
                    weak = true
                } else if (pw != pw2) {
                    mismatch = true
                } else {
                    scope.launch {
                        loading = true
                        serverErr = ""
                        val err = if (isReset) Repo.doResetPassword(Repo.forgotPhone, pw)
                        else Repo.doRegister(Repo.regPhone, Repo.regName, Repo.regRole, pw)
                        loading = false
                        if (err != null) {
                            serverErr = err
                        } else if (isReset) {
                            Intents.toast(ctx, toastDone)
                            nav.clearTo(Routes.LOGIN)
                        } else if (Repo.regRole == Role.CLIENT) {
                            nav.navigate(Routes.SUBSCRIBE)
                        } else {
                            nav.navigate(Routes.CREATED)
                        }
                    }
                }
            }, enabled = pw.isNotEmpty() && pw2.isNotEmpty() && (isReset || agree), loading = loading)
        }
    }
}

// ---------- نجاح عام ----------
@Composable
fun SuccessScreen(title: String, subtitle: String, button: String, onClick: () -> Unit) {
    AuthScreen(onBack = null) {
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(Gold), contentAlignment = Alignment.Center) {
                Text("✓", color = Ink, fontSize = 52.sp, fontWeight = FontWeight.Bold)
            }
            VGap(24)
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            VGap(8)
            Text(subtitle, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
        }
        PrimaryButton(button, onClick)
    }
}

// ---------- الاشتراك (عميل) ----------
@Composable
fun SubscribeScreen(nav: NavHostController) {
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var failMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val total = Repo.SUBSCRIPTION_AMOUNT + Repo.SUBSCRIPTION_VAT
    AuthScreen(onBack = null) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(t("أكمل تسجيلك", "Complete your registration"), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            VGap(16)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Cream).padding(20.dp)) {
                Text(t("$total ريال سعودي", "SAR $total"), color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(t("مبلغ الإشتراك : ${Repo.SUBSCRIPTION_AMOUNT} ريال سعودي", "Subscription: SAR ${Repo.SUBSCRIPTION_AMOUNT}"), color = Gray1)
                Text(t("الضريبة المضافة : ${Repo.SUBSCRIPTION_VAT} ريال سعودي", "VAT: SAR ${Repo.SUBSCRIPTION_VAT}"), color = Gray1)
                Text(t("دفع لمرة واحدة", "One-time payment"), color = Brown, fontWeight = FontWeight.Medium)
            }
            VGap(16)
            listOf(
                t("احصل على تركيب المكيفات بدون أي تكلفة إضافية", "Get air-conditioner installation at no extra cost"),
                t("فريق الدعم لدينا متوفر دائمًا لمساعدتك في أي وقت وأي مكان", "Our support team is always available to help you anytime, anywhere"),
                t("استمتع بخصومات على خدمات مثل التصميم الداخلي وتجهيز العقارات", "Enjoy discounts on services like interior design and property preparation")
            ).forEach { Text("•  $it", modifier = Modifier.padding(vertical = 6.dp), fontSize = 15.sp) }
            VGap(8)
            if (BuildConfig.DEMO_MODE) Text(t("وضع تجريبي — لا يتم سحب أي مبلغ.", "Demo mode — no amount is charged."), color = Gold, fontSize = 13.sp)
            if (failed) ErrorText(failMsg.ifEmpty { t("حدث خطأ برجاء المحاولة مرة أخرى", "Something went wrong, please try again") })
            Text(t("هذا دفع لمرة واحدة. غير قابل للاسترداد.", "This is a one-time payment. Non-refundable."), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        PrimaryButton(t("الدفع الآن", "Pay now"), {
            scope.launch {
                loading = true; failed = false
                val err = Repo.doSubscribe()
                loading = false
                if (err == null) nav.navigate(Routes.PAID) else { failed = true; failMsg = err }
            }
        }, loading = loading)
        TextButton(onClick = { nav.clearTo(Routes.MAIN) }, modifier = Modifier.fillMaxWidth()) {
            Text(t("الإستمرار دون إشتراك", "Continue without subscription"), color = Gold)
        }
    }
}
