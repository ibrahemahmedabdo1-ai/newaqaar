@file:OptIn(ExperimentalMaterial3Api::class)

package com.bawabat.alorood.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bawabat.alorood.data.ImageStore
import com.bawabat.alorood.data.Property
import com.bawabat.alorood.data.Repo
import com.bawabat.alorood.data.Role
import com.bawabat.alorood.util.Validation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---------- هيكل الشاشة ----------

@Composable
fun CircleBtn(onClick: () -> Unit, size: Int = 44, content: @Composable () -> Unit) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(Cream).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun Header(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().background(Ink).statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            CircleBtn(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("رجوع", "Back"), tint = Ink) }
            Spacer(Modifier.width(12.dp))
        }
        Text(title, color = Cream, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

/** شاشة عادية: هيدر داكن + محتوى */
@Composable
fun Screen(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottom: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground, modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().imePadding()) {
            Header(title, onBack, actions)
            Column(Modifier.weight(1f).fillMaxWidth(), content = content)
            Box(Modifier.navigationBarsPadding()) { bottom() }
        }
    }
}

/** شاشات الدخول والتسجيل: خلفية داكنة دائمًا كما في التصميم */
@Composable
fun AuthScreen(onBack: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    MaterialTheme(colorScheme = DarkScheme) {
        Surface(color = Ink, contentColor = Color.White, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                if (onBack != null) {
                    CircleBtn(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("رجوع", "Back"), tint = Ink) }
                    Spacer(Modifier.height(12.dp))
                }
                content()
            }
        }
    }
}

@Composable fun VGap(h: Int) = Spacer(Modifier.height(h.dp))

// ---------- أزرار وحقول ----------

@Composable
fun PrimaryButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, outlined: Boolean = false, loading: Boolean = false, danger: Boolean = false
) {
    val shape = RoundedCornerShape(28.dp)
    val m = modifier.fillMaxWidth().height(52.dp)
    if (outlined) {
        OutlinedButton(onClick = onClick, modifier = m, enabled = enabled && !loading, shape = shape,
            border = BorderStroke(1.dp, if (danger) Danger else MaterialTheme.colorScheme.onBackground)) {
            Text(text, fontSize = 16.sp, color = if (danger) Danger else MaterialTheme.colorScheme.onBackground)
        }
    } else {
        Button(
            onClick = onClick, modifier = m, enabled = enabled && !loading, shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (danger) Danger else MaterialTheme.colorScheme.primary,
                contentColor = if (danger) Color.White else MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = LineGray, disabledContentColor = Color.White
            )
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun AppTextField(
    value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier,
    placeholder: String = "", error: Boolean = false, keyboard: KeyboardType = KeyboardType.Text,
    password: Boolean = false, onDark: Boolean = false, singleLine: Boolean = true, minLines: Int = 1,
    leading: (@Composable () -> Unit)? = null, trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true, readOnly: Boolean = false, imeAction: ImeAction = ImeAction.Default,
    onDone: (() -> Unit)? = null
) {
    var show by remember { mutableStateOf(false) }
    val container = if (onDark) Cream else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (onDark) Ink else MaterialTheme.colorScheme.onSurface
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = modifier.fillMaxWidth(),
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder, color = Gray2) },
        isError = error, enabled = enabled, readOnly = readOnly,
        singleLine = singleLine, minLines = minLines,
        shape = RoundedCornerShape(28.dp),
        visualTransformation = if (password && !show) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        leadingIcon = leading,
        trailingIcon = if (password) {
            { TextButton(onClick = { show = !show }) { Text(if (show) t("إخفاء", "Hide") else t("إظهار", "Show"), color = Gray2, fontSize = 12.sp) } }
        } else trailing,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = container, unfocusedContainerColor = container,
            disabledContainerColor = container, errorContainerColor = container,
            focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent, errorBorderColor = Danger,
            focusedTextColor = textColor, unfocusedTextColor = textColor,
            disabledTextColor = textColor, errorTextColor = textColor,
            cursorColor = textColor
        )
    )
}

@Composable
fun PhoneField(code: String, onCodeClick: () -> Unit, number: String, onNumber: (String) -> Unit, error: Boolean = false, onDark: Boolean = true) {
    AppTextField(
        value = number, onChange = { onNumber(Validation.digitsOnly(it).take(12)) },
        keyboard = KeyboardType.Phone, error = error, onDark = onDark, placeholder = t("رقم الهاتف", "Phone number"),
        leading = {
            Box(
                Modifier.padding(start = 6.dp).clip(RoundedCornerShape(20.dp)).background(Peach)
                    .clickable(onClick = onCodeClick).padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(code, color = Ink, fontWeight = FontWeight.Medium)
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Ink, modifier = Modifier.size(18.dp))
                }
            }
        }
    )
}

@Composable
fun OtpField(value: String, onChange: (String) -> Unit, length: Int = 4, error: Boolean = false) {
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { try { fr.requestFocus() } catch (e: Exception) { /* ignore */ } }
    BasicTextField(
        value = value,
        onValueChange = { onChange(Validation.digitsOnly(it).take(length)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().focusRequester(fr),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                    repeat(length) { i ->
                        val shape = RoundedCornerShape(14.dp)
                        Box(
                            Modifier.size(56.dp, 64.dp).clip(shape).background(Cream)
                                .border(if (error) 1.5.dp else 0.dp, Danger, shape),
                            contentAlignment = Alignment.Center
                        ) { Text(value.getOrNull(i)?.toString() ?: "", fontSize = 24.sp, color = Ink, fontWeight = FontWeight.Bold) }
                    }
                }
                Box(Modifier.matchParentSize().alpha(0f)) { inner() }
            }
        }
    )
}

@Composable
fun ErrorText(text: String) = Text(text, color = Danger, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))

@Composable
fun Label(text: String, modifier: Modifier = Modifier) =
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = modifier.padding(vertical = 6.dp))

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(bg).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)) {
        Text(text, color = fg, fontSize = 13.sp)
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun Row2(title: String, onClick: () -> Unit, trailing: @Composable () -> Unit = {}, danger: Boolean = false) {
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, modifier = Modifier.weight(1f), fontSize = 16.sp, color = if (danger) Danger else MaterialTheme.colorScheme.onBackground)
            trailing()
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

// ---------- حوارات ----------

@Composable
fun ConfirmDialog(
    title: String, text: String = "", confirm: String, dismiss: String = t("إلغاء", "Cancel"),
    danger: Boolean = false, onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium) },
        text = if (text.isNotEmpty()) { { Text(text) } } else null,
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = if (danger) Danger else MaterialTheme.colorScheme.primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismiss, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    )
}

@Composable
fun LoginRequiredDialog(onLogin: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = t("سجل الدخول أولًا", "Sign in first"),
        text = t("سجل الدخول الآن للتواصل مع الشركات مباشرة وإتمام صفقاتك بسهولة.", "Sign in to contact companies directly and complete your deals."),
        confirm = t("تسجيل الدخول", "Sign in"), onConfirm = onLogin, onDismiss = onDismiss
    )
}

@Composable
fun GuestPrompt(onLogin: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(t("سجل الدخول الآن للتواصل مع الشركات مباشرة وإتمام صفقاتك بسهولة.", "Sign in to contact companies directly and complete your deals."), textAlign = TextAlign.Center, fontSize = 16.sp)
        VGap(20)
        PrimaryButton(t("تسجيل الدخول", "Sign in"), onLogin)
    }
}

// ---------- اختيار الدولة ----------

data class Country(val ar: String, val en: String, val code: String)

val Countries = listOf(
    Country("مصر", "Egypt", "+20"), Country("السعودية", "Saudi Arabia", "+966"), Country("ليبيا", "Libya", "+218"),
    Country("تونس", "Tunisia", "+216"), Country("قطر", "Qatar", "+974"), Country("البحرين", "Bahrain", "+973"),
    Country("اليمن", "Yemen", "+967"), Country("الإمارات", "UAE", "+971"), Country("الكويت", "Kuwait", "+965"),
    Country("عُمان", "Oman", "+968")
)

@Composable
fun CountrySheet(onPick: (Country) -> Unit, onDismiss: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val list = Countries.filter { q.isBlank() || it.ar.contains(q.trim()) || it.en.contains(q.trim(), ignoreCase = true) || it.code.contains(q.trim()) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(horizontal = 16.dp).heightIn(min = 380.dp)) {
            AppTextField(q, { q = it }, placeholder = t("بحث", "Search"))
            VGap(8)
            if (list.isEmpty()) {
                EmptyState(t("لا توجد نتائج مطابقة، يُرجى التأكد من صحة اسم الدولة.", "No matching results. Please check the country name."))
            } else {
                LazyColumn {
                    items(list) { c ->
                        Row(Modifier.fillMaxWidth().clickable { onPick(c) }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(t(c.ar, c.en), modifier = Modifier.weight(1f), fontSize = 16.sp)
                            Text(c.code, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            VGap(24)
        }
    }
}

// ---------- الصور ----------

@Composable
fun BuildingArt(seed: Int, modifier: Modifier = Modifier) {
    val bodies = listOf(Color(0xFFE8DCC8), Color(0xFFCFC6B8), Color(0xFFD9B99B), Color(0xFFB9C3CC), Color(0xFFE6D5C3))
    val accents = listOf(Color(0xFF8A6A4A), Color(0xFF5F7A8A), Color(0xFFA0522D), Color(0xFF6B8E6B))
    val body = bodies[seed % bodies.size]
    val accent = accents[seed % accents.size]
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFF8FC1E8), Color(0xFFDCEBF7))), Offset(0f, 0f), size)
        val bw = w * 0.62f
        val bx = (w - bw) / 2f
        val top = h * 0.14f
        val bottom = h * 0.92f
        drawRect(body, Offset(bx, top), Size(bw, bottom - top))
        drawRect(accent, Offset(bx, top), Size(bw, h * 0.04f))
        val rows = 5 + seed % 2
        val cols = 4
        val cw = bw / (cols * 2 + 1)
        val rh = (bottom - top - h * 0.18f) / (rows * 2)
        for (r in 0 until rows) for (c in 0 until cols) {
            drawRect(Color(0xCC3B5F7A), Offset(bx + cw * (1 + c * 2), top + h * 0.07f + rh * (r * 2 + 0.5f)), Size(cw, rh))
        }
        drawRect(Color(0xFF4A3B2E), Offset(bx + bw * 0.42f, bottom - h * 0.1f), Size(bw * 0.16f, h * 0.1f))
        drawRect(Color(0xFF9E9E9E), Offset(0f, bottom), Size(w, h - bottom))
    }
}

@Composable
fun FileImage(path: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val bmp by produceState<ImageBitmap?>(null, path) {
        value = if (path.startsWith("http")) ImageStore.loadUrl(path, 1000) else withContext(Dispatchers.IO) { ImageStore.load(path, 1000) }
    }
    val b = bmp
    if (b != null) Image(b, null, modifier, contentScale = contentScale)
    else Box(modifier.background(Peach))
}

@Composable
fun PropertyImage(p: Property, modifier: Modifier = Modifier) {
    if (p.photos.isNotEmpty()) FileImage(p.photos[0], modifier) else BuildingArt(p.seed, modifier)
}

// ---------- بطاقات العقارات ----------

@Composable
fun PropertyCard(p: Property, fav: Boolean, onFav: () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier, extra: String = "") {
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick)) {
        Box {
            PropertyImage(p, Modifier.fillMaxWidth().aspectRatio(1.25f))
            Box(
                Modifier.padding(6.dp).size(24.dp).clip(CircleShape).background(Color.White).clickable(onClick = onFav).align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) { Icon(if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, t("مفضلة", "Favorite"), tint = Danger, modifier = Modifier.size(15.dp)) }
        }
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("SAR " + Validation.formatPrice(p.price), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("${p.area} m²  •  ${p.rooms} ${t("غرف", "rooms")}", fontSize = 10.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(t("حي ${p.district}، ${p.city}", "${p.district}, ${p.city}"), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (extra.isNotEmpty()) Text(extra, fontSize = 10.sp, color = Brown, maxLines = 1)
        }
    }
}

@Composable
fun PropertyGrid(list: List<Property>, onOpen: (Property) -> Unit, onNeedLogin: () -> Unit, cols: Int = 3, showCommission: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        list.chunked(cols).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { p ->
                    PropertyCard(
                        p, Repo.isFav(p.id),
                        onFav = { if (Repo.loggedIn) Repo.toggleFav(p.id) else onNeedLogin() },
                        onClick = { onOpen(p) }, modifier = Modifier.weight(1f),
                        extra = if (showCommission) t("العمولة: ${Validation.formatPrice(p.commission)}", "Commission: ${Validation.formatPrice(p.commission)}") else ""
                    )
                }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ---------- الشريط السفلي ----------

@Composable
fun MainBottomBar(role: Role, tab: Int, onTab: (Int) -> Unit, onCenter: () -> Unit) {
    data class Item(val idx: Int, val icon: ImageVector, val label: String)
    val items = listOf(
        Item(0, Icons.Default.Home, t("الرئيسية", "Home")),
        Item(1, Icons.Default.Email, t("الرسائل", "Messages")),
        Item(2, if (role == Role.DEVELOPER) Icons.Default.List else Icons.Default.Favorite,
            if (role == Role.DEVELOPER) t("إعلاناتي", "My ads") else t("طلباتي", "My offers")),
        Item(3, Icons.Default.Person, t("الملف الشخصي", "Profile"))
    )
    @Composable
    fun RowScope.Tab(i: Item) {
        val sel = tab == i.idx
        Column(
            Modifier.weight(1f).clickable { onTab(i.idx) }.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(i.icon, i.label, tint = if (sel) Gold else Cream.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
            Text(i.label, fontSize = 11.sp, color = if (sel) Gold else Cream.copy(alpha = 0.7f), maxLines = 1)
        }
    }
    Row(
        Modifier.fillMaxWidth().background(Ink).navigationBarsPadding().height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Tab(items[0]); Tab(items[1])
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(Gold).clickable(onClick = onCenter),
                contentAlignment = Alignment.Center
            ) { Text("BD", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        }
        Tab(items[2]); Tab(items[3])
    }
}
