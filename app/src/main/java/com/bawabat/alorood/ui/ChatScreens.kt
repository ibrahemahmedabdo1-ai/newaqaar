@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.bawabat.alorood.ui

import android.content.Context
import android.provider.OpenableColumns
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.bawabat.alorood.data.Api
import com.bawabat.alorood.data.ChatThread
import com.bawabat.alorood.data.Remote
import com.bawabat.alorood.data.Demo
import com.bawabat.alorood.data.ImageStore
import com.bawabat.alorood.data.Msg
import com.bawabat.alorood.data.Repo
import com.bawabat.alorood.data.SearchPrefs
import com.bawabat.alorood.util.Intents
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun isImg(f: String) = f.startsWith("/") || f.startsWith("http")

private fun timeText(ms: Long): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))

private fun displayName(ctx: Context, uri: Uri): String = try {
    ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
        val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (i >= 0 && c.moveToFirst()) c.getString(i) else null
    } ?: "file"
} catch (e: Exception) { "file" }

@Composable
private fun Avatar(name: String, size: Int = 46) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(Ink), contentAlignment = Alignment.Center) {
        Text(name.trim().take(1).uppercase(), color = Gold, fontWeight = FontWeight.Bold, fontSize = (size / 2.4).sp)
    }
}

// ---------- قائمة المحادثات ----------
@Composable
fun MessagesTab(nav: NavHostController, onLogin: () -> Unit) {
    if (!Repo.loggedIn) {
        Column(Modifier.fillMaxSize()) {
            Header(t("الرسائل", "Messages"), null)
            GuestPrompt(onLogin)
        }
        return
    }
    var query by remember { mutableStateOf("") }
    var unreadOnly by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<ChatThread?>(null) }
    val list = Repo.myThreads().filter { !it.blocked }
        .filter { !unreadOnly || it.unread }
        .filter { query.isBlank() || it.peer.contains(query.trim(), ignoreCase = true) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(Ink).statusBarsPadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("الرسائل", "Messages"), color = Cream, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                TextButton(onClick = { nav.navigate(Routes.BLOCKED) }) { Text(t("قائمة الحظر", "Block list"), color = Gold, fontSize = 13.sp) }
            }
            VGap(8)
            AppTextField(query, { query = it }, onDark = true, placeholder = t("بحث", "Search"), leading = { Icon(Icons.Default.Search, null, tint = Gray2) })
            VGap(8)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(t("غير المقروء", "Unread"), unreadOnly) { unreadOnly = !unreadOnly }
            }
        }
        if (list.isEmpty()) {
            EmptyState(
                if (query.isNotBlank() || unreadOnly) t("لم يتم العثور على نتائج، يرجى التحقق من صحة الاسم والمحاولة مرة أخرى.", "No results found. Please check the name and try again.")
                else t("لا توجد محادثات بعد. ابدأ محادثة من صفحة أي عقار.", "No chats yet. Start a chat from any property page.")
            )
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(list, key = { it.id }) { th ->
                    val last = Repo.lastMessage(th.id)
                    Row(
                        Modifier.fillMaxWidth().combinedClickable(onClick = { nav.navigate(Routes.thread(th.id)) }, onLongClick = { menuFor = th })
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(th.peer)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(th.peer, fontWeight = if (th.unread) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                when {
                                    last == null -> t("إبدأ بالمحادثة", "Start the conversation")
                                    last.file.isNotEmpty() && last.text.isEmpty() -> t("📎 مرفق", "📎 Attachment")
                                    else -> last.text
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (last != null) Text(timeText(last.time), fontSize = 11.sp, color = Gray2)
                        if (th.unread) { Spacer(Modifier.width(8.dp)); Box(Modifier.size(10.dp).clip(CircleShape).background(Gold)) }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
    menuFor?.let { th ->
        AlertDialog(
            onDismissRequest = { menuFor = null }, containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(th.peer, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    TextButton(onClick = { Repo.updateThread(th.id) { it.copy(unread = !it.unread) }; menuFor = null }) {
                        Text(if (th.unread) t("تمييز كمقروء", "Mark as read") else t("تمييز كغير مقروء", "Mark as unread"))
                    }
                    TextButton(onClick = { Repo.updateThread(th.id) { it.copy(blocked = true) }; menuFor = null }) { Text(t("حظر", "Block")) }
                    TextButton(onClick = { Repo.updateThread(th.id) { it.copy(deleted = true) }; menuFor = null }) { Text(t("حذف الدردشة", "Delete chat"), color = Danger) }
                }
            },
            confirmButton = {}, dismissButton = { TextButton(onClick = { menuFor = null }) { Text(t("إلغاء", "Cancel")) } }
        )
    }
}

// ---------- قائمة الحظر ----------
@Composable
fun BlockedScreen(nav: NavHostController) {
    val list = Repo.myThreads().filter { it.blocked }
    Screen(t("قائمة الحظر", "Block list"), { nav.popBackStack() }) {
        if (list.isEmpty()) EmptyState(t("لا يوجد مستخدمون محظورون", "No blocked users"))
        list.forEach { th ->
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(th.peer, 40); Spacer(Modifier.width(12.dp))
                Text(th.peer, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                TextButton(onClick = { Repo.updateThread(th.id) { it.copy(blocked = false) } }) { Text(t("إلغاء الحظر", "Unblock")) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}

// ---------- المحادثة ----------
@Composable
fun ThreadScreen(nav: NavHostController, id: String) {
    val th = Repo.threads.firstOrNull { it.id == id }
    if (th == null) { Screen(t("محادثة", "Chat"), { nav.popBackStack() }) { EmptyState(t("المحادثة غير موجودة", "Chat not found")) }; return }
    val ctx = LocalContext.current
    val msgs = Repo.messages.filter { it.threadId == id }
    val listState = rememberLazyListState()
    var text by remember { mutableStateOf("") }
    var attach by remember { mutableStateOf(false) }
    var actionFor by remember { mutableStateOf<Msg?>(null) }
    var editing by remember { mutableStateOf<Msg?>(null) }
    var editText by remember { mutableStateOf("") }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val failMsg = t("تعذر إرفاق الملف", "Could not attach the file")
    val dlMsg = t("التنزيل يتطلب ربط الخادم في النسخة الفعلية", "Download needs the server in the production version")

    LaunchedEffect(Repo.messages.size) { if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.size - 1) }
    LaunchedEffect(id) { if (th.unread) Repo.updateThread(id) { it.copy(unread = false) } }
    LaunchedEffect(id) {
        if (Api.enabled) {
            while (true) { delay(6000); Remote.pull() }
        }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val path = ImageStore.save(ctx, uri, "chat_" + Repo.uid())
            if (path != null) Repo.sendMessage(id, "", path) else Intents.toast(ctx, failMsg)
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) {
            val path = ImageStore.saveBitmap(ctx, bmp, "chat_" + Repo.uid())
            if (path != null) Repo.sendMessage(id, "", path) else Intents.toast(ctx, failMsg)
        }
    }
    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) Repo.sendMessage(id, "", displayName(ctx, uri))
    }

    Screen(th.peer, { nav.popBackStack() }, bottom = {
        if (th.blocked) {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(t("لا يمكنك إرسال أو استقبال الرسائل", "You can't send or receive messages"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { Repo.updateThread(id) { it.copy(blocked = false) } }) { Text(t("إلغاء الحظر", "Unblock")) }
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleBtn({ attach = true }, 44) { Icon(Icons.Default.Add, t("إرفاق", "Attach"), tint = Ink) }
                Spacer(Modifier.width(8.dp))
                AppTextField(text, { text = it.take(1000) }, Modifier.weight(1f), placeholder = t("اكتب رسالة", "Type a message"), singleLine = false)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).clickable(enabled = text.isNotBlank()) {
                        Repo.sendMessage(id, text.trim()); text = ""
                    }, contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.Send, t("إرسال", "Send"), tint = MaterialTheme.colorScheme.onPrimary) }
            }
        }
    }) {
        if (msgs.isEmpty()) EmptyState(t("إبدأ بالمحادثة", "Start the conversation"), Modifier.weight(1f))
        else LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(msgs, key = { it.id }) { m ->
                val align = if (m.mine) (if (rtl) Alignment.Start else Alignment.End) else (if (rtl) Alignment.End else Alignment.Start)
                Column(Modifier.fillMaxWidth(), horizontalAlignment = align) {
                    val bg = if (m.mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    val fg = if (m.mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    Column(
                        Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(16.dp)).background(bg)
                            .combinedClickable(onClick = {
                                if (m.file.isNotEmpty() && !isImg(m.file) && !m.deleted) Intents.toast(ctx, dlMsg)
                            }, onLongClick = { if (m.mine && !m.deleted && !th.blocked) actionFor = m })
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        when {
                            m.deleted -> Text(t("تم حذف هذه الرسالة", "This message was deleted"), color = fg.copy(alpha = 0.7f), fontStyle = FontStyle.Italic, fontSize = 13.sp)
                            isImg(m.file) -> FileImage(m.file, Modifier.size(180.dp).clip(RoundedCornerShape(10.dp)))
                            m.file.isNotEmpty() -> Text("📎 " + m.file + "  ·  " + t("تنزيل", "Download"), color = fg, fontSize = 14.sp)
                        }
                        if (!m.deleted && m.text.isNotEmpty()) Text(m.text, color = fg, fontSize = 15.sp)
                        Text(timeText(m.time) + if (m.edited) "  ·  " + t("معدّلة", "edited") else "", color = fg.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                }
            }
        }
    }

    if (attach) {
        ModalBottomSheet(onDismissRequest = { attach = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Row2(t("الكاميرا", "Camera"), { attach = false; camera.launch(null) })
                Row2(t("الإستوديو", "Gallery"), { attach = false; gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                Row2(t("الملفات", "Files"), { attach = false; files.launch(arrayOf("*/*")) })
            }
        }
    }
    actionFor?.let { m ->
        AlertDialog(
            onDismissRequest = { actionFor = null }, containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(t("الرسالة", "Message")) },
            text = {
                Column {
                    if (m.text.isNotEmpty()) TextButton(onClick = { editing = m; editText = m.text; actionFor = null }) { Text(t("تعديل الرسالة", "Edit message")) }
                    TextButton(onClick = { Repo.deleteMessage(m.id); actionFor = null }) { Text(t("حذف الرسالة", "Delete message"), color = Danger) }
                }
            },
            confirmButton = {}, dismissButton = { TextButton(onClick = { actionFor = null }) { Text(t("إلغاء", "Cancel")) } }
        )
    }
    editing?.let { m ->
        AlertDialog(
            onDismissRequest = { editing = null }, containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(t("تعديل الرسالة", "Edit message")) },
            text = { AppTextField(editText, { editText = it.take(1000) }, singleLine = false) },
            confirmButton = { TextButton(onClick = { if (editText.isNotBlank()) Repo.editMessage(m.id, editText.trim()); editing = null }) { Text(t("حفظ", "Save")) } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(t("إلغاء", "Cancel")) } }
        )
    }
}

// ---------- مساعد البحث ("مستشارك الذكي") ----------
private data class Line(val bot: Boolean, val text: String)

@Composable
fun AdvisorScreen(nav: NavHostController) {
    val lines = remember {
        mutableStateListOf(Line(true, "مرحبًا! 👋 كيف يمكنني مساعدتك في احتياجاتك العقارية اليوم؟"))
    }
    var step by remember { mutableStateOf(0) }
    var picked by remember { mutableStateOf("") }
    val lang = Repo.lang
    val greet = t("مرحبًا! 👋 كيف يمكنني مساعدتك في احتياجاتك العقارية اليوم؟", "Hi! 👋 How can I help with your real-estate needs today?")
    Screen(t("مستشارك الذكي", "Your smart advisor"), { nav.popBackStack() }) {
        Text(
            t("ردود آلية بقواعد بسيطة تساعدك في البحث، وليست مستشارًا بشريًا.", "Automated, rule-based replies to help you search — not a human advisor."),
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Line(true, greet).let { Bubble(it) }
            lines.drop(1).forEach { Bubble(it) }
            when (step) {
                0 -> Pill(t("ابحث عن العقارات المتاحة", "Find available properties"), false) {
                    lines.add(Line(false, if (lang == "ar") "ابحث عن العقارات المتاحة" else "Find available properties"))
                    lines.add(Line(true, if (lang == "ar") "دعني أساعدك في العثور على العقار المثالي! 🏡 من فضلك أخبرني عن نوع العقار الذي تبحث عنه." else "Let me help you find the perfect property! 🏡 Please tell me which type you are looking for."))
                    step = 1
                }
                1 -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Demo.types.forEach { ty ->
                        Pill(if (lang == "ar") ty.ar else ty.en, false) {
                            picked = ty.key
                            lines.add(Line(false, if (lang == "ar") ty.ar else ty.en))
                            lines.add(Line(true, if (lang == "ar") "تمام! اضغط «عرض النتائج» لعرض العقارات المناسبة." else "Great! Tap “Show results” to see matching properties."))
                            step = 2
                        }
                    }
                }
                else -> PrimaryButton(t("عرض النتائج", "Show results"), {
                    Repo.setPrefs(SearchPrefs(types = setOf(picked)))
                    nav.clearTo(Routes.MAIN)
                })
            }
            VGap(8)
            Text(t("كل الي تحتاجه حنا نوفره", "Everything you need, we provide"), color = Brown, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun Bubble(l: Line) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val bg = if (l.bot) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
    val fg = if (l.bot) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
    // الروبوت على الجهة الأولى (يمين في العربية) والمستخدم على الجهة الأخرى
    val align = if (l.bot) (if (rtl) Alignment.Start else Alignment.Start) else Alignment.End
    Column(Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Box(Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(16.dp)).background(bg).padding(12.dp)) { Text(l.text, color = fg) }
    }
}
