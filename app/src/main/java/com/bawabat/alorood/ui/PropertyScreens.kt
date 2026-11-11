package com.bawabat.alorood.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.bawabat.alorood.BuildConfig
import com.bawabat.alorood.data.Demo
import com.bawabat.alorood.data.FloorState
import com.bawabat.alorood.data.Property
import com.bawabat.alorood.data.Repo
import com.bawabat.alorood.data.Role
import com.bawabat.alorood.util.Intents
import com.bawabat.alorood.util.Validation
import kotlinx.coroutines.launch

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 8.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoRows(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(it, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) =
    Text(text, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))

@Composable
private fun MapPlaceholder(onOpen: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(14.dp)).background(Sky), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 36.dp.toPx()
            var x = 0f
            while (x < size.width) { drawLine(Color.White, Offset(x, 0f), Offset(x + step * 0.5f, size.height), 3f); x += step }
            var y = 0f
            while (y < size.height) { drawLine(Color.White, Offset(0f, y), Offset(size.width, y + step * 0.2f), 3f); y += step }
        }
        Box(Modifier.clip(RoundedCornerShape(22.dp)).background(Ink).clickable(onClick = onOpen).padding(horizontal = 22.dp, vertical = 12.dp)) {
            Text(t("فتح الخريطة", "Open map"), color = Cream, fontWeight = FontWeight.Medium)
        }
    }
}

// ---------- تفاصيل العقار ----------
@Composable
fun DetailScreen(nav: NavHostController, id: String) {
    val p = Repo.findProperty(id)
    if (p == null) {
        Screen(t("العقار", "Property"), { nav.popBackStack() }) { EmptyState(t("هذا الإعلان لم يعد متاحًا.", "This listing is no longer available.")) }
        return
    }
    val ctx = LocalContext.current
    val lang = Repo.lang
    var needLogin by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf(false) }
    val mine = p.ownerPhone.isNotEmpty() && p.ownerPhone == Repo.phone
    val showCommission = Repo.role != Role.CLIENT
    val similar = Repo.visibleProperties().filter { it.id != p.id }.take(6)
    val noPhoneMsg = t("رقم التواصل غير متوفر في البيانات التجريبية", "Contact number is not available in demo data")

    Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Box {
                    val n = if (p.photos.isNotEmpty()) p.photos.size else 4
                    val pager = rememberPagerState(pageCount = { n })
                    HorizontalPager(pager, Modifier.fillMaxWidth().height(290.dp)) { page ->
                        if (p.photos.isNotEmpty()) FileImage(p.photos[page], Modifier.fillMaxSize())
                        else BuildingArt(p.seed + page, Modifier.fillMaxSize())
                    }
                    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircleBtn({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, t("رجوع", "Back"), tint = Ink) }
                        Spacer(Modifier.weight(1f))
                        CircleBtn({
                            Intents.share(ctx, "${Demo.typeLabel(p, lang)} — SAR ${Validation.formatPrice(p.price)} — ${p.district}, ${p.city}\nhttps://play.google.com/store/apps/details?id=${ctx.packageName}")
                        }) { Icon(Icons.Default.Share, t("مشاركة", "Share"), tint = Ink) }
                    }
                    Row(Modifier.align(Alignment.BottomCenter).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(n) { i ->
                            Box(Modifier.size(8.dp).clip(CircleShape).background(if (pager.currentPage == i) Gold else Color.White.copy(alpha = 0.7f)))
                        }
                    }
                }
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Demo.typeLabel(p, lang), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("SAR " + Validation.formatPrice(p.price), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    if (showCommission) Text(t("العمولة : ${Validation.formatPrice(p.commission)} ريال", "Commission: SAR ${Validation.formatPrice(p.commission)}"), color = Brown, fontWeight = FontWeight.Medium)
                    VGap(8)
                    Text(p.description, fontSize = 16.sp)
                    VGap(6)
                    Text(t("${p.district} .${p.city}", "${p.district}, ${p.city}"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline)
                    Row(Modifier.fillMaxWidth()) {
                        Stat(t("المساحة", "Area"), "${p.area} m²", Modifier.weight(1f))
                        Stat(t("الغرف", "Rooms"), "${p.rooms}", Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Stat(t("الحمامات", "Bathrooms"), "${p.baths}", Modifier.weight(1f))
                        Stat(t("الواجهة", "Facade"), p.direction, Modifier.weight(1f))
                    }
                    SectionTitle(t("الضمانات", "Warranties"))
                    InfoRows(p.guarantees)
                    SectionTitle(t("الموقع", "Location"))
                    MapPlaceholder { Intents.openMap(ctx, p.lat, p.lng, "${p.district}, ${p.city}") }
                    if (p.amenities.isNotEmpty()) { SectionTitle(t("المرافق", "Amenities")); InfoRows(p.amenities) }
                    if (p.infra.isNotEmpty()) { SectionTitle(t("البنية التحتية", "Infrastructure")); InfoRows(p.infra) }
                    SectionTitle(t("خيارات الدفع", "Payment options"))
                    InfoRows(listOf(t("كاش", "Cash")))
                    SectionTitle(t("إعلانات أخرى قد تعجبك", "Other ads you may like"))
                    PropertyGrid(similar, { nav.navigate(Routes.detail(it.id)) }, { needLogin = true }, showCommission = showCommission)
                    VGap(16)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    VGap(8)
                    Text(t("تم نشر الإعلان بواسطة", "Ad published by"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Text(p.publisher, fontWeight = FontWeight.Medium)
                    TextButton(onClick = { if (Repo.loggedIn) report = true else needLogin = true }) {
                        Text(t("الإبلاغ عن الإعلان", "Report ad"), color = Danger)
                    }
                    VGap(8)
                }
            }
            if (!mine) {
                Row(Modifier.fillMaxWidth().background(Ink).navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionPill(Icons.Default.Email, t("محادثة", "Chat"), Modifier.weight(1f)) {
                        if (!Repo.loggedIn) needLogin = true
                        else nav.navigate(Routes.thread(Repo.openThread(p.publisher, p.id).id))
                    }
                    ActionPill(Icons.Default.Check, t("تفاوض مع المالك", "Negotiate"), Modifier.weight(1.4f)) {
                        if (!Repo.loggedIn) needLogin = true
                        else {
                            Repo.dFloor = ""; Repo.dAmount = 0L
                            if (Repo.role == Role.CLIENT) nav.navigate(Routes.floors(p.id)) else nav.navigate(Routes.clientInfo(p.id))
                        }
                    }
                    ActionPill(Icons.Default.Call, t("الإتصال", "Call"), Modifier.weight(1f)) {
                        if (!Repo.loggedIn) needLogin = true
                        else if (p.contactPhone.isBlank()) Intents.toast(ctx, noPhoneMsg)
                        else Intents.dial(ctx, p.contactPhone)
                    }
                }
            }
        }
    }
    if (needLogin) LoginRequiredDialog({ needLogin = false; nav.navigate(Routes.LOGIN) }, { needLogin = false })
    if (report) ReportDialog(p) { report = false }
}

@Composable
private fun ActionPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(24.dp)).border(1.dp, Cream.copy(alpha = 0.5f), RoundedCornerShape(24.dp)).clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Cream, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Cream, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun ReportDialog(p: Property, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val reasons = listOf(
        t("معلومات مضللة", "Misleading information"), t("إعلان مكرر", "Duplicate listing"),
        t("العقار غير متاح", "Property not available"), t("محتوى مسيء", "Offensive content"), t("احتيال", "Fraud")
    )
    var sel by remember { mutableStateOf(0) }
    val done = t("تم استلام بلاغك وسيتم مراجعته", "Your report was received and will be reviewed")
    AlertDialog(
        onDismissRequest = onClose, containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(t("الإبلاغ عن الإعلان", "Report ad")) },
        text = {
            Column {
                reasons.forEachIndexed { i, r ->
                    Row(Modifier.fillMaxWidth().clickable { sel = i }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(sel == i, { sel = i }); Text(r)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { Repo.addReport(p.id, reasons[sel]); Intents.toast(ctx, done); onClose() }) { Text(t("إرسال", "Send")) } },
        dismissButton = { TextButton(onClick = onClose) { Text(t("إلغاء", "Cancel")) } }
    )
}

// ---------- بيانات العميل (وسيط / مطور) ----------
@Composable
fun ClientInfoScreen(nav: NavHostController, id: String) {
    var name by remember { mutableStateOf(Repo.dClientName) }
    var code by remember { mutableStateOf(Repo.regCode) }
    var number by remember { mutableStateOf("") }
    var sheet by remember { mutableStateOf(false) }
    Screen(t("تفاوض مع المالك", "Negotiate with owner"), { nav.popBackStack() }, bottom = {
        Column(Modifier.padding(16.dp)) {
            PrimaryButton(t("التالي", "Next"), {
                Repo.dClientName = name.trim(); Repo.dClientPhone = Validation.fullPhone(code, number)
                nav.navigate(Routes.floors(id))
            }, enabled = name.isNotBlank() && Validation.isValidPhone(number))
        }
    }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(t("يرجى إدخال بيانات العميل لبدء عملية التفاوض.", "Please enter the client's details to start the negotiation."))
            VGap(16)
            Label(t("إسم العميل", "Client name"))
            AppTextField(name, { name = it.take(60) }, placeholder = t("إسم العميل", "Client name"))
            VGap(8)
            Label(t("رقم الهاتف الخاص بالعميل", "Client phone number"))
            PhoneField(code, { sheet = true }, number, { number = it }, onDark = false)
        }
    }
    if (sheet) CountrySheet({ code = it.code; sheet = false }, { sheet = false })
}

// ---------- اختيار الدور ----------
@Composable
fun FloorsScreen(nav: NavHostController, id: String) {
    val p = Repo.findProperty(id)
    var sel by remember { mutableStateOf(Repo.dFloor) }
    Screen(t("تفاوض مع المالك", "Negotiate with owner"), { nav.popBackStack() }, bottom = {
        Column(Modifier.padding(16.dp)) {
            PrimaryButton(t("التالي", "Next"), { Repo.dFloor = sel; nav.navigate(Routes.offer(id)) }, enabled = sel.isNotEmpty())
        }
    }) {
        if (p == null) { EmptyState(t("الإعلان غير متاح", "Listing unavailable")); return@Screen }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(t("يرجى اختيار الدور الذي ترغب في التفاوض عليه.", "Please choose the floor you want to negotiate on."))
            VGap(12)
            Text(t("الأدوار المتاحة", "Available floors"), fontWeight = FontWeight.Medium, fontSize = 16.sp)
            val avail = p.floors.filter { it.state == FloorState.AVAILABLE }
            if (avail.isEmpty()) EmptyState(t("لا توجد أدوار متاحة حاليًا", "No floors available right now"))
            avail.forEach { f ->
                Row(Modifier.fillMaxWidth().clickable { sel = f.name }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(sel == f.name, { sel = f.name }); Text(f.name, fontSize = 16.sp)
                }
            }
            val reserved = p.floors.filter { it.state == FloorState.RESERVED }
            if (reserved.isNotEmpty()) {
                VGap(16)
                Text(t("الأدوار المحجوزة", "Reserved floors"), fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Text(t("الأدوار المحجوزة حالياً، قد تصبح متاحة لاحقًا.", "Currently reserved, they may become available later."), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                reserved.forEach { Text(it.name, Modifier.padding(vertical = 8.dp), color = Gray2) }
            }
            val sold = p.floors.filter { it.state == FloorState.SOLD }
            if (sold.isNotEmpty()) {
                VGap(16)
                Text(t("الأدوار المباعة", "Sold floors"), fontWeight = FontWeight.Medium, fontSize = 16.sp)
                sold.forEach { Text(it.name, Modifier.padding(vertical = 8.dp), color = Gray2) }
            }
        }
    }
}

// ---------- تقديم العرض ----------
@Composable
fun OfferScreen(nav: NavHostController, id: String) {
    val p = Repo.findProperty(id)
    var text by remember { mutableStateOf(if (Repo.dAmount > 0) Repo.dAmount.toString() else "") }
    val amount = Validation.parseAmount(text)
    val min = p?.minOffer ?: 0L
    val valid = p != null && Validation.isOfferValid(amount, min)
    Screen(Repo.dFloor.ifEmpty { t("تقديم عرض", "Make an offer") }, { nav.popBackStack() }, bottom = {
        Column(Modifier.padding(16.dp)) {
            Text(t("بتقديم هذا العرض، سيتم مراجعة طلبك من قِبَل صاحب العقار", "By submitting this offer, your request will be reviewed by the property owner"), fontSize = 13.sp)
            VGap(12)
            PrimaryButton(t("التالي", "Next"), { Repo.dAmount = amount; nav.navigate(Routes.booking(id)) }, enabled = valid)
        }
    }) {
        if (p == null) { EmptyState(t("الإعلان غير متاح", "Listing unavailable")); return@Screen }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(t("يرجى إدخال السعر الذي ترغب في تقديمه.", "Please enter the price you wish to offer."), fontSize = 14.sp)
            VGap(12)
            if (Repo.role != Role.CLIENT) Text(t("إسم العميل: ${Repo.dClientName}", "Client: ${Repo.dClientName}"), fontSize = 16.sp)
            Text(t("السعر الأصلي: SAR ${p.price}", "Original price: SAR ${Validation.formatPrice(p.price)}"), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            Label(t("قدم عرضك", "Your offer"))
            AppTextField(text, { text = Validation.digitsOnly(it).take(9) }, keyboard = KeyboardType.Number, placeholder = t("قدم عرضك", "Your offer"),
                error = text.isNotEmpty() && !valid, trailing = { Text(t("ريال", "SAR"), Modifier.padding(end = 12.dp), color = Gray2) })
            VGap(6)
            Text(
                t("لا يمكن تقديم عرض أقل من ${min} SAR", "Offers below SAR ${Validation.formatPrice(min)} are not allowed"),
                color = if (text.isNotEmpty() && !valid) Danger else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
            )
        }
    }
}

// ---------- الحجز والدفع ----------
@Composable
fun BookingScreen(nav: NavHostController, id: String) {
    val p = Repo.findProperty(id)
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var failMsg by remember { mutableStateOf("") }
    var agree by remember { mutableStateOf(false) }
    val total = Repo.BOOKING_AMOUNT + Repo.VAT_AMOUNT
    Screen(t("الحجز", "Booking"), { nav.popBackStack() }, bottom = {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(agree, { agree = it }, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary))
                Text(t("عند الحجز، فإنك توافق على شروط الشروط والأحكام", "By booking you agree to the Terms & Conditions"), fontSize = 12.sp, modifier = Modifier.weight(1f))
            }
            VGap(8)
            PrimaryButton(t("الدفع الآن", "Pay now"), {
                if (p != null) scope.launch {
                    loading = true; failed = false
                    val err = Repo.doBook(p.id)
                    loading = false
                    if (err == null) nav.navigate(Routes.sent(id)) else { failed = true; failMsg = err }
                }
            }, enabled = agree && p != null, loading = loading)
        }
    }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(20.dp)) {
                Text(t("$total ريال سعودي", "SAR $total"), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(t("مبلغ الحجز : ${Repo.BOOKING_AMOUNT} ريال سعودي", "Booking amount: SAR ${Repo.BOOKING_AMOUNT}"))
                Text(t("الضريبة المضافة : ${Repo.VAT_AMOUNT} ريال سعودي", "VAT: SAR ${Repo.VAT_AMOUNT}"))
            }
            VGap(16)
            Text(t("سيتم إخفاء هذا الإعلان لمدة ${Repo.HOLD_DAYS} أيام بعد الدفع", "This ad will be hidden for ${Repo.HOLD_DAYS} days after payment"), modifier = Modifier.padding(vertical = 4.dp))
            Text(t("إذا وافقت الشركة على عرضك، سيتم تحويل المبلغ إلى الشركة لضمان الحجز.", "If the company accepts your offer, the amount is transferred to the company to secure the booking."), modifier = Modifier.padding(vertical = 4.dp))
            Text(t("إذا رفضت الشركة العرض خلال ${Repo.HOLD_DAYS} أيام، سيتم استرداد المبلغ بالكامل.", "If the company rejects the offer within ${Repo.HOLD_DAYS} days, the amount is fully refunded."), modifier = Modifier.padding(vertical = 4.dp))
            if (BuildConfig.DEMO_MODE) { VGap(8); Text(t("وضع تجريبي — لا يتم سحب أي مبلغ.", "Demo mode — no amount is charged."), color = Brown, fontWeight = FontWeight.Medium) }
            if (failed) ErrorText(failMsg.ifEmpty { t("حدث خطأ برجاء المحاولة مرة أخرى", "Something went wrong, please try again") })
        }
    }
}

// ---------- تم إرسال العرض ----------
@Composable
fun SentScreen(nav: NavHostController, id: String) {
    val similar = Repo.visibleProperties().filter { it.id != id }.take(6)
    Screen(t("تم الإرسال", "Sent"), null, bottom = {
        Column(Modifier.padding(16.dp)) { PrimaryButton(t("الرئيسية", "Home"), { nav.clearTo(Routes.MAIN) }) }
    }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            VGap(16)
            Box(Modifier.size(88.dp).clip(CircleShape).background(Gold), contentAlignment = Alignment.Center) {
                Text("✓", color = Ink, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            }
            VGap(16)
            Text(t("تم إرسال عرض للمالك", "Your offer was sent to the owner"), fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(t("يمكنك متابعة حالة العرض من «طلباتي».", "You can follow the offer status in “My offers”."), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            VGap(20)
            Text(t("إعلانات أخرى قد تعجبك", "Other ads you may like"), fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Start))
            VGap(8)
            PropertyGrid(similar, { nav.navigate(Routes.detail(it.id)) }, {})
        }
    }
}
