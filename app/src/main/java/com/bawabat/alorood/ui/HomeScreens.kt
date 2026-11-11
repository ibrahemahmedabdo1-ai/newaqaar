package com.bawabat.alorood.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.bawabat.alorood.BuildConfig
import com.bawabat.alorood.data.Crit
import com.bawabat.alorood.data.Demo
import com.bawabat.alorood.data.Repo
import com.bawabat.alorood.data.Role
import com.bawabat.alorood.data.SearchPrefs
import com.bawabat.alorood.util.Intents
import com.bawabat.alorood.util.LocationHelper
import com.bawabat.alorood.util.Validation

private fun ordinalAr(i: Int) = listOf("الأولى", "الثانية", "الثالثة", "الرابعة", "الخامسة").getOrElse(i) { "${i + 1}" }
private fun ordinalEn(i: Int) = listOf("first", "second", "third", "fourth", "fifth").getOrElse(i) { "${i + 1}" }

private fun activeFilterCount(p: SearchPrefs): Int {
    var n = 0
    if (p.types.isNotEmpty()) n++
    if (p.minPrice > 0 || p.maxPrice < 20_000_000L) n++
    if (p.facade.isNotEmpty()) n++
    if (p.views.isNotEmpty()) n++
    if (p.maid) n++
    if (p.minBaths > 0) n++
    if (p.districts.isNotEmpty()) n++
    if (p.nearMe) n++
    return n
}

// ---------- الرئيسية ----------
@Composable
fun HomeTab(nav: NavHostController, onNeedLogin: () -> Unit) {
    val prefs = Repo.prefs
    val lang = Repo.lang
    val filtered = Repo.applyPrefs(Repo.visibleProperties())
    val role = Repo.role
    val prios = Repo.priorities.toList().ifEmpty { listOf(Crit.PRICE.name, Crit.AREA.name, Crit.ROOMS.name) }
    val expanded = remember { mutableStateListOf<Int>() }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(Ink).statusBarsPadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("بوابة العروض", "Bawabat Alorood"), color = Cream, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                if (BuildConfig.DEMO_MODE) {
                    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Gold).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text(t("بيانات تجريبية", "Demo data"), color = Ink, fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.padding(top = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(28.dp)).background(Cream).clickable { nav.navigate(Routes.SEARCH) }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = Gray2)
                    Spacer(Modifier.width(8.dp))
                    Text(prefs.query.ifEmpty { t("بحث", "Search") }, color = if (prefs.query.isEmpty()) Gray2 else Ink, fontSize = 15.sp, maxLines = 1)
                }
                Spacer(Modifier.width(10.dp))
                CircleBtn({ nav.navigate(Routes.FILTERS) }) { Icon(Icons.Default.Settings, t("عوامل التصفية", "Filters"), tint = Ink) }
            }
            Spacer(Modifier.padding(top = 10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderChip(
                    if (prefs.districts.isEmpty()) t("كل المناطق", "All areas") else prefs.districts.joinToString("، ").take(24)
                ) { nav.navigate(Routes.AREAS) }
                HeaderChip(
                    if (prefs.types.isEmpty()) t("كل الأنواع", "All types")
                    else Demo.types.filter { it.key in prefs.types }.joinToString(" , ") { if (lang == "ar") it.ar else it.en }.take(26)
                ) { nav.navigate(Routes.FILTERS) }
                HeaderChip(t("ترتيب الأولويات", "Priorities")) { nav.navigate(Routes.PRIORITIES) }
                val n = activeFilterCount(prefs)
                if (n > 0) HeaderChip(t("مسح الفلاتر ($n)", "Clear filters ($n)")) { Repo.setPrefs(SearchPrefs()) }
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (filtered.isEmpty()) {
                EmptyState(t("لا توجد نتائج مطابقة، جرّب تغيير عوامل التصفية.", "No matching results, try changing the filters."))
            } else {
                prios.forEachIndexed { idx, name ->
                    val crit = try { Crit.valueOf(name) } catch (e: Exception) { Crit.PRICE }
                    val sorted = Repo.sortBy(crit, filtered)
                    val showAll = idx in expanded
                    Text(
                        t("نتائج البحث بناء على الأولية ${ordinalAr(idx)} (${crit.ar})", "Results by ${ordinalEn(idx)} priority (${crit.en})"),
                        fontSize = 18.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    PropertyGrid(
                        if (showAll) sorted else sorted.take(6),
                        onOpen = { nav.navigate(Routes.detail(it.id)) }, onNeedLogin = onNeedLogin,
                        showCommission = role == Role.BROKER || crit == Crit.COMMISSION
                    )
                    if (sorted.size > 6) {
                        TextButton(onClick = { if (showAll) expanded.remove(idx) else expanded.add(idx) }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (showAll) t("عرض أقل", "Show less") else t("أنظر المزيد", "See more"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.padding(bottom = 12.dp))
        }
    }
}

@Composable
private fun HeaderChip(text: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(20.dp)).background(Cream.copy(alpha = 0.12f)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)
    ) { Text(text, color = Cream, fontSize = 13.sp, maxLines = 1) }
}

// ---------- البحث ----------
@Composable
fun SearchScreen(nav: NavHostController) {
    var q by remember { mutableStateOf(Repo.prefs.query) }
    val lang = Repo.lang
    fun submit(text: String) {
        Repo.addRecent(text)
        Repo.setPrefs(Repo.prefs.copy(query = text.trim()))
        nav.popBackStack()
    }
    Screen(t("البحث", "Search"), { nav.popBackStack() }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            AppTextField(q, { q = it }, placeholder = t("بحث", "Search"), imeAction = ImeAction.Search, onDone = { submit(q) },
                leading = { Icon(Icons.Default.Search, null, tint = Gray2) },
                trailing = { if (q.isNotEmpty()) TextButton(onClick = { q = "" }) { Text(t("مسح", "Clear"), color = Danger, fontSize = 12.sp) } })
            VGap(8)
            PrimaryButton(t("بحث", "Search"), { submit(q) })
            VGap(16)
            if (Repo.recent.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t("آخر ما تم البحث عنه", "Recent searches"), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    TextButton(onClick = { Repo.clearRecent() }) { Text(t("مسح", "Clear"), color = Danger) }
                }
                Repo.recent.toList().forEach { r ->
                    Row(Modifier.fillMaxWidth().clickable { submit(r) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(r, modifier = Modifier.weight(1f), fontSize = 16.sp)
                        Box(Modifier.clickable { Repo.removeRecent(r) }.padding(6.dp)) { Icon(Icons.Default.Close, t("حذف", "Remove"), tint = Gray2) }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                VGap(12)
            }
            Text(t("الأنواع", "Types"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Demo.types.forEach { ty ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        Repo.setPrefs(Repo.prefs.copy(types = setOf(ty.key), query = ""))
                        nav.popBackStack()
                    }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (lang == "ar") ty.ar else ty.en, modifier = Modifier.weight(1f), fontSize = 16.sp)
                    Icon(
                        if (LocalLayoutDirection.current == LayoutDirection.Rtl) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        null, tint = Gray2
                    )
                }
            }
        }
    }
}

// ---------- المناطق + الموقع ----------
@Composable
fun AreasScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    val selected = remember { mutableStateListOf<String>().apply { addAll(Repo.prefs.districts) } }
    var near by remember { mutableStateOf(Repo.prefs.nearMe && !Repo.userLat.isNaN()) }
    val failMsg = t("تعذر تحديد الموقع", "Could not determine your location")
    val deniedMsg = t("لم يتم منح إذن الموقع", "Location permission was not granted")
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            if (LocationHelper.updateFromLastKnown(ctx)) near = true else Intents.toast(ctx, failMsg)
        } else Intents.toast(ctx, deniedMsg)
    }
    Screen(t("إختر المنطقة", "Choose area"), { nav.popBackStack() }, bottom = {
        Column(Modifier.padding(16.dp)) {
            PrimaryButton(t("تطبيق", "Apply"), {
                Repo.setPrefs(Repo.prefs.copy(districts = selected.toSet(), nearMe = near))
                nav.popBackStack()
            })
        }
    }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(t("نستخدم موقعك التقريبي فقط لإظهار العقارات القريبة منك، ولا نشاركه مع أي جهة.", "We use your approximate location only to show properties near you, and never share it."),
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            VGap(8)
            PrimaryButton(if (near) t("القريب مني: مُفعّل", "Near me: on") else t("تحديد الموقع", "Use my location"), {
                if (near) { near = false } else launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }, outlined = true)
            VGap(12)
            Text(t("إختر المنطقة", "Choose area"), fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Demo.districts.forEach { d ->
                Row(Modifier.fillMaxWidth().clickable { if (d.name in selected) selected.remove(d.name) else selected.add(d.name) }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(d.name in selected, { if (it) selected.add(d.name) else selected.remove(d.name) },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary))
                    Text(d.name, fontSize = 16.sp)
                }
            }
        }
    }
}

// ---------- عوامل التصفية ----------
@Composable
fun FiltersScreen(nav: NavHostController) {
    val p0 = Repo.prefs
    val types = remember { mutableStateListOf<String>().apply { addAll(p0.types) } }
    var from by remember { mutableStateOf(if (p0.minPrice > 0) p0.minPrice.toString() else "") }
    var to by remember { mutableStateOf(if (p0.maxPrice < 20_000_000L) p0.maxPrice.toString() else "") }
    var facade by remember { mutableStateOf(p0.facade) }
    val views = remember { mutableStateListOf<String>().apply { addAll(p0.views) } }
    var maid by remember { mutableStateOf(p0.maid) }
    var baths by remember { mutableStateOf(p0.minBaths) }
    val lang = Repo.lang
    Screen(t("عوامل التصفية", "Filters"), { nav.popBackStack() }, bottom = {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(t("إعادة تعيين", "Reset"), {
                types.clear(); from = ""; to = ""; facade = ""; views.clear(); maid = false; baths = 0
            }, Modifier.weight(1f), outlined = true)
            PrimaryButton(t("تطبيق", "Apply"), {
                val mn = Validation.parseAmount(from)
                val mx = if (to.isBlank()) 20_000_000L else Validation.parseAmount(to)
                Repo.setPrefs(p0.copy(
                    types = types.toSet(), minPrice = mn, maxPrice = if (mx < mn) 20_000_000L else mx,
                    facade = facade, views = views.toSet(), maid = maid, minBaths = baths
                ))
                nav.popBackStack()
            }, Modifier.weight(1f))
        }
    }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Label(t("نوع العقار", "Property type"))
            FlowPills {
                Demo.types.forEach { ty ->
                    Pill(if (lang == "ar") ty.ar else ty.en, ty.key in types) { if (ty.key in types) types.remove(ty.key) else types.add(ty.key) }
                }
            }
            VGap(8)
            Label(t("السعر (SAR)", "Price (SAR)"))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(to, { to = Validation.digitsOnly(it).take(9) }, Modifier.weight(1f), placeholder = t("إلى", "To"), keyboard = KeyboardType.Number)
                AppTextField(from, { from = Validation.digitsOnly(it).take(9) }, Modifier.weight(1f), placeholder = t("من", "From"), keyboard = KeyboardType.Number)
            }
            VGap(8)
            Label(t("الواجهة", "Facade"))
            FlowPills {
                Pill(t("أمامية", "Front"), facade == "front") { facade = if (facade == "front") "" else "front" }
                Pill(t("خلفية", "Back"), facade == "back") { facade = if (facade == "back") "" else "back" }
            }
            VGap(8)
            Label(t("الإطلالة", "View"))
            FlowPills {
                listOf("city" to t("على المدينة", "City view"), "garden" to t("على حديقة", "Garden view"), "sea" to t("على البحر", "Sea view")).forEach { (k, l) ->
                    Pill(l, k in views) { if (k in views) views.remove(k) else views.add(k) }
                }
            }
            VGap(8)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("غرفة الخادمة", "Maid's room"), Modifier.weight(1f), fontSize = 16.sp)
                Switch(maid, { maid = it }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
            }
            VGap(8)
            Label(t("دورات المياه", "Bathrooms (min)"))
            FlowPills {
                listOf(1, 2, 3, 4, 5).forEach { n ->
                    Pill(if (n == 5) "+5" else "$n", baths == n) { baths = if (baths == n) 0 else n }
                }
            }
        }
    }
}

@Composable
fun FlowPills(content: @Composable () -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

// ---------- ترتيب الأولويات ----------
@Composable
fun PrioritiesScreen(nav: NavHostController) {
    val role = Repo.role
    val count = if (role == Role.CLIENT) 3 else 4
    val options = Crit.values().filter { it != Crit.COMMISSION || role != Role.CLIENT }
    val chosen = remember {
        mutableStateListOf<String>().apply {
            val cur = Repo.priorities.toList()
            for (i in 0 until count) add(cur.getOrNull(i) ?: options[i % options.size].name)
        }
    }
    Screen(t("رتب أولوياتك", "Rank your priorities"), { nav.popBackStack() }, bottom = {
        Column(Modifier.padding(16.dp)) { PrimaryButton(t("تطبيق", "Apply"), { Repo.setPriorities(chosen.toList()); nav.popBackStack() }) }
    }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(t("نساعدك في الوصول إلى العروض التي تناسبك", "We help you reach the offers that suit you"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            VGap(12)
            chosen.forEachIndexed { i, sel ->
                Label(t("الأولية ${ordinalAr(i)}", "Priority ${i + 1}"))
                FlowPills {
                    options.forEach { c -> Pill(if (Repo.lang == "ar") c.ar else c.en, sel == c.name) { chosen[i] = c.name } }
                }
                VGap(8)
            }
        }
    }
}
