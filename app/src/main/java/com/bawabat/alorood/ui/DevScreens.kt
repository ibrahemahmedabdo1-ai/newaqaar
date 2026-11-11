package com.bawabat.alorood.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.bawabat.alorood.BuildConfig
import com.bawabat.alorood.data.AdStatus
import com.bawabat.alorood.data.Demo
import com.bawabat.alorood.data.Floor
import com.bawabat.alorood.data.FloorState
import com.bawabat.alorood.data.ImageStore
import com.bawabat.alorood.data.OfferStatus
import com.bawabat.alorood.data.Property
import com.bawabat.alorood.data.Repo
import com.bawabat.alorood.util.Intents
import com.bawabat.alorood.util.Validation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun dateText(ms: Long): String = SimpleDateFormat("yyyy / M / d", Locale.US).format(Date(ms))
private fun floorLabel(i: Int): String = Demo.floorNames.getOrElse(i - 1) { "الدور $i" }

// ---------- إعلاناتي ----------
@Composable
fun MyAdsTab(nav: NavHostController, onLogin: () -> Unit) {
    if (!Repo.loggedIn) {
        Column(Modifier.fillMaxSize()) { Header(t("إعلاناتي", "My ads"), null); GuestPrompt(onLogin) }
        return
    }
    var tab by remember { mutableStateOf(0) }
    var delId by remember { mutableStateOf<String?>(null) }
    var editId by remember { mutableStateOf<String?>(null) }
    var endId by remember { mutableStateOf<String?>(null) }
    var buyer by remember { mutableStateOf("") }
    val ads = Repo.myAds().filter {
        when (tab) { 0 -> it.status == AdStatus.PUBLISHED; 1 -> it.status == AdStatus.PENDING; else -> it.status == AdStatus.ENDED || it.status == AdStatus.REJECTED }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(Ink).statusBarsPadding().padding(16.dp)) {
            Text(t("إعلاناتي", "My ads"), color = Cream, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            VGap(10)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(t("المنشورة", "Published"), tab == 0) { tab = 0 }
                Pill(t("المعلقة", "Pending"), tab == 1) { tab = 1 }
                Pill(t("المنتهية", "Ended"), tab == 2) { tab = 2 }
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (ads.isEmpty()) EmptyState(t("لا توجد إعلانات في هذا القسم.", "No ads in this section."))
            ads.forEach { ad ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)) {
                    Row(Modifier.clickable { nav.navigate(Routes.detail(ad.id)) }, verticalAlignment = Alignment.CenterVertically) {
                        PropertyImage(ad, Modifier.size(76.dp).clip(RoundedCornerShape(10.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("SAR " + Validation.formatPrice(ad.price), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(t("حي ${ad.district} , ${ad.city}", "${ad.district}, ${ad.city}"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                when (ad.status) {
                                    AdStatus.PUBLISHED -> t("نشرت في ${dateText(ad.publishedAt)}", "Published on ${dateText(ad.publishedAt)}")
                                    AdStatus.PENDING -> t("طلب نشرها في ${dateText(ad.createdAt)}", "Publish requested on ${dateText(ad.createdAt)}")
                                    AdStatus.ENDED -> t("انتهى الإعلان", "Ad ended")
                                    AdStatus.REJECTED -> t("تم رفض الإعلان: ${ad.rejectReason}", "Rejected: ${ad.rejectReason}")
                                }, fontSize = 12.sp, color = Brown
                            )
                        }
                    }
                    if (ad.status != AdStatus.ENDED) {
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            TextButton(onClick = { editId = ad.id }) { Text(t("تعديل", "Edit")) }
                            TextButton(onClick = { delId = ad.id }) { Text(t("حذف", "Delete"), color = Danger) }
                            if (ad.status == AdStatus.PUBLISHED) TextButton(onClick = { buyer = ""; endId = ad.id }) { Text(t("إنهاء الإعلان", "End ad")) }
                            if (ad.status == AdStatus.PENDING && BuildConfig.DEMO_MODE) {
                                TextButton(onClick = { Repo.setAdStatus(ad.id, AdStatus.PUBLISHED) }) { Text(t("محاكاة موافقة المنصة (تجريبي)", "Simulate approval (demo)")) }
                            }
                        }
                    } else {
                        TextButton(onClick = { delId = ad.id }) { Text(t("حذف", "Delete"), color = Danger) }
                    }
                }
            }
        }
        Box(Modifier.padding(16.dp)) { PrimaryButton(t("إضافة إعلان", "Add ad"), { nav.navigate(Routes.addAd()) }) }
    }
    delId?.let { id ->
        ConfirmDialog(title = t("هل أنت واثق من حذف الإعلان ؟", "Are you sure you want to delete this ad?"), confirm = t("حذف", "Delete"), danger = true,
            onConfirm = { Repo.deleteAd(id); delId = null }, onDismiss = { delId = null })
    }
    editId?.let { id ->
        ConfirmDialog(
            title = t("هل أنت واثق من تعديل الإعلان ؟", "Are you sure you want to edit this ad?"),
            text = t("سيتم مراجعة الإعلان من قبل المنصة قبل إعادة نشره مرة أخرى", "The platform will review the ad before it is published again"),
            confirm = t("تعديل", "Edit"), onConfirm = { editId = null; nav.navigate(Routes.addAd(id)) }, onDismiss = { editId = null }
        )
    }
    endId?.let { id ->
        AlertDialog(
            onDismissRequest = { endId = null }, containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(t("إنهاء الإعلان", "End ad")) },
            text = {
                Column {
                    Text(t("يرجى إدخال رقم هاتف مشتري العقار.", "Please enter the property buyer's phone number."))
                    VGap(8)
                    AppTextField(buyer, { buyer = Validation.digitsOnly(it).take(14) }, keyboard = KeyboardType.Phone, placeholder = t("رقم الهاتف", "Phone number"))
                }
            },
            confirmButton = {
                TextButton(enabled = Validation.isValidPhone(buyer), onClick = {
                    Repo.ads.firstOrNull { it.id == id }?.let { Repo.updateAd(it.copy(status = AdStatus.ENDED, soldTo = buyer)) }
                    endId = null
                }) { Text(t("إنهاء", "End")) }
            },
            dismissButton = { TextButton(onClick = { endId = null }) { Text(t("إلغاء", "Cancel")) } }
        )
    }
}

// ---------- إضافة / تعديل إعلان ----------
@Composable
fun AdFormScreen(nav: NavHostController, editId: String?) {
    val ctx = LocalContext.current
    val old = editId?.let { id -> Repo.ads.firstOrNull { it.id == id } }
    val photos = remember { mutableStateListOf<String>().apply { old?.let { addAll(it.photos) } } }
    var district by remember { mutableStateOf(old?.district ?: "") }
    var typeKey by remember {
        mutableStateOf(old?.let { o -> Demo.types.firstOrNull { it.matches(o) }?.key } ?: "")
    }
    var price by remember { mutableStateOf(old?.price?.toString() ?: "") }
    var commission by remember { mutableStateOf(old?.commission?.toString() ?: "") }
    var minOffer by remember { mutableStateOf(old?.minOffer?.toString() ?: "") }
    var area by remember { mutableStateOf(old?.area?.toString() ?: "") }
    var floorsN by remember { mutableStateOf(old?.floors?.size?.toString() ?: "1") }
    var baths by remember { mutableStateOf(old?.baths?.toString() ?: "2") }
    var direction by remember { mutableStateOf(old?.direction ?: "شمالية") }
    var facade by remember { mutableStateOf(old?.facade ?: "front") }
    var view by remember { mutableStateOf(old?.view ?: "city") }
    var maid by remember { mutableStateOf(old?.maid ?: false) }
    var desc by remember { mutableStateOf(old?.description ?: "") }
    val guarantees = remember { mutableStateListOf<String>().apply { old?.let { addAll(it.guarantees) } } }
    val failMsg = t("تعذر حفظ بعض الصور", "Some photos could not be saved")
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(8)) { uris ->
        var failed = false
        for (u in uris) {
            if (photos.size >= 8) break
            val path = ImageStore.save(ctx, u, "ad_" + Repo.uid())
            if (path != null) photos.add(path) else failed = true
        }
        if (failed) Intents.toast(ctx, failMsg)
    }
    val priceV = Validation.parseAmount(price)
    val areaV = Validation.parseAmount(area).toInt()
    val can = photos.isNotEmpty() && district.isNotEmpty() && typeKey.isNotEmpty() && priceV > 0 && areaV > 0 && desc.isNotBlank()

    Screen(if (old == null) t("إضافة إعلان", "Add ad") else t("تعديل الإعلان", "Edit ad"), { nav.popBackStack() }, bottom = {
        Column(Modifier.padding(16.dp)) {
            PrimaryButton(t("نشر الإعلان", "Publish ad"), {
                val opt = Demo.types.first { it.key == typeKey }
                val d = Demo.districts.firstOrNull { it.name == district }
                val n = (floorsN.toIntOrNull() ?: 1).coerceIn(1, 40)
                val minV = Validation.parseAmount(minOffer).let { if (it <= 0L) ((priceV * 91 / 100) / 10_000) * 10_000 else it }
                val prop = Property(
                    id = old?.id ?: ("ad_" + Repo.uid()), kind = opt.kind, rooms = opt.rooms,
                    baths = (baths.toIntOrNull() ?: 1).coerceIn(1, 20), area = areaV, price = priceV,
                    minOffer = minOf(minV, priceV),
                    commission = Validation.parseAmount(commission).let { if (it <= 0L) priceV * 2 / 100 else it },
                    district = district, city = "جدة", description = desc.trim(), facade = facade, direction = direction, view = view,
                    maid = maid, floors = (1..n).map { Floor(floorLabel(it), FloorState.AVAILABLE) },
                    guarantees = guarantees.filter { it.isNotBlank() }, amenities = buildList { if (maid) add("غرفة خادمة"); add("موقف خاص") },
                    infra = emptyList(), publisher = Repo.me?.name ?: "", ownerPhone = Repo.phone ?: "", contactPhone = Repo.phone ?: "",
                    lat = d?.lat ?: 21.54, lng = d?.lng ?: 39.17, seed = old?.seed ?: (1 + (System.currentTimeMillis() % 50).toInt()),
                    photos = photos.toList(), status = AdStatus.PENDING, createdAt = old?.createdAt ?: System.currentTimeMillis()
                )
                if (old == null) Repo.addAd(prop) else Repo.updateAd(prop)
                nav.navigate(Routes.AD_SENT) { popUpTo(Routes.MAIN) }
            }, enabled = can)
        }
    }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Label(t("صور العقار", "Property photos") + " (${photos.size}/8)")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                photos.toList().forEach { path ->
                    Box {
                        FileImage(path, Modifier.size(84.dp).clip(RoundedCornerShape(12.dp)))
                        Box(Modifier.align(Alignment.TopStart).padding(4.dp).size(22.dp).clip(CircleShape).background(Danger).clickable { photos.remove(path); ImageStore.delete(path) }, contentAlignment = Alignment.Center) {
                            Text("×", color = androidx.compose.ui.graphics.Color.White, fontSize = 14.sp)
                        }
                    }
                }
                if (photos.size < 8) {
                    Box(Modifier.size(84.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                        Text(t("إضافة صورة", "Add photo"), fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            VGap(8)
            Label(t("موقع العقار", "Property location"))
            FlowPills { Demo.districts.forEach { d -> Pill(d.name, district == d.name) { district = d.name } } }
            VGap(8)
            Label(t("نوع العقار", "Property type"))
            FlowPills { Demo.types.forEach { ty -> Pill(if (Repo.lang == "ar") ty.ar else ty.en, typeKey == ty.key) { typeKey = ty.key } } }
            VGap(8)
            Label(t("السعر", "Price") + " (SAR)")
            AppTextField(price, { price = Validation.digitsOnly(it).take(10) }, keyboard = KeyboardType.Number, placeholder = "1,500,000")
            VGap(8)
            Label(t("العمولة", "Commission") + " (SAR)")
            AppTextField(commission, { commission = Validation.digitsOnly(it).take(9) }, keyboard = KeyboardType.Number, placeholder = t("اختياري (٢٪ افتراضيًا)", "Optional (2% by default)"))
            VGap(8)
            Label(t("أقل عرض مقبول", "Minimum accepted offer") + " (SAR)")
            AppTextField(minOffer, { minOffer = Validation.digitsOnly(it).take(10) }, keyboard = KeyboardType.Number, placeholder = t("اختياري (٩١٪ من السعر)", "Optional (91% of price)"))
            VGap(8)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) { Label(t("المساحة (م²)", "Area (m²)")); AppTextField(area, { area = Validation.digitsOnly(it).take(5) }, keyboard = KeyboardType.Number) }
                Column(Modifier.weight(1f)) { Label(t("عدد الأدوار", "Floors")); AppTextField(floorsN, { floorsN = Validation.digitsOnly(it).take(2) }, keyboard = KeyboardType.Number) }
                Column(Modifier.weight(1f)) { Label(t("الحمامات", "Bathrooms")); AppTextField(baths, { baths = Validation.digitsOnly(it).take(2) }, keyboard = KeyboardType.Number) }
            }
            VGap(8)
            Label(t("الواجهة", "Facade"))
            FlowPills {
                Pill(t("أمامية", "Front"), facade == "front") { facade = "front" }
                Pill(t("خلفية", "Back"), facade == "back") { facade = "back" }
                listOf("شمالية", "جنوبية", "شرقية", "غربية").forEach { d -> Pill(d, direction == d) { direction = d } }
            }
            VGap(8)
            Label(t("الإطلالة", "View"))
            FlowPills {
                listOf("city" to t("على المدينة", "City view"), "garden" to t("على حديقة", "Garden view"), "sea" to t("على البحر", "Sea view")).forEach { (k, l) ->
                    Pill(l, view == k) { view = k }
                }
            }
            VGap(8)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("غرفة خادمة", "Maid's room"), Modifier.weight(1f), fontSize = 16.sp)
                Switch(maid, { maid = it }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
            }
            VGap(8)
            Label(t("وصف العقار", "Description"))
            AppTextField(desc, { desc = it.take(1500) }, singleLine = false, minLines = 4)
            VGap(8)
            Label(t("الضمانات", "Warranties"))
            guarantees.forEachIndexed { i, g ->
                AppTextField(g, { guarantees[i] = it.take(80) }, placeholder = t("ضمان ${i + 1}", "Warranty ${i + 1}"))
                VGap(6)
            }
            TextButton(onClick = { if (guarantees.size < 8) guarantees.add("") }) { Text(t("إضافة ضمان", "Add warranty")) }
            VGap(8)
            Text(t("سيظهر رقم هاتفك على الإعلان للتواصل، وتتم مراجعة الإعلان من المنصة قبل نشره.", "Your phone number is shown on the ad for contact, and the platform reviews the ad before publishing."),
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AdSentScreen(nav: NavHostController) {
    AuthScreen(onBack = null) {
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(Gold), contentAlignment = Alignment.Center) {
                Text("✓", color = Ink, fontSize = 52.sp, fontWeight = FontWeight.Bold)
            }
            VGap(24)
            Text(t("تم إرسال طلب الإعلان بنجاح!", "Your ad request was sent!"), fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            VGap(8)
            Text(t("شكرًا لك! لقد استلمنا طلبك لرفع الإعلان، وسيتم مراجعته من قِبل فريق المنصة.", "Thank you! We received your request and the platform team will review it."), textAlign = TextAlign.Center)
            VGap(4)
            Text(t("يمكنك متابعة حالة إعلانك من خلال صفحة إعلاناتي", "You can follow the status from “My ads”"), textAlign = TextAlign.Center)
        }
        PrimaryButton(t("الرجوع إلى الصفحة الرئيسية", "Back to home"), { nav.clearTo(Routes.MAIN) })
    }
}

// ---------- الحجوزات ----------
@Composable
fun ReservationsScreen(nav: NavHostController) {
    val list = Repo.offersOnMyAds()
    val now = System.currentTimeMillis()
    Screen(t("الحجوزات", "Reservations"), { nav.popBackStack() }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (list.isEmpty()) EmptyState(t("لا توجد حجوزات على إعلاناتك بعد.", "No reservations on your ads yet."))
            list.forEach { o ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)) {
                    OfferCard(o, now) { nav.navigate(Routes.detail(o.propertyId)) }
                    Text(t("العميل: ${o.clientName} · ${o.clientPhone}", "Client: ${o.clientName} · ${o.clientPhone}"), fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    if (o.status == OfferStatus.PENDING) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            PrimaryButton(t("قبول", "Accept"), { Repo.setOfferStatus(o.id, OfferStatus.ACTIVE) }, Modifier.weight(1f))
                            PrimaryButton(t("رفض", "Reject"), { Repo.setOfferStatus(o.id, OfferStatus.REJECTED) }, Modifier.weight(1f), outlined = true, danger = true)
                        }
                    }
                }
            }
        }
    }
}
