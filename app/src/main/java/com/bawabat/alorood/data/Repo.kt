package com.bawabat.alorood.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bawabat.alorood.util.Validation
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/** مخزن البيانات المحلي (وضع تجريبي بدون خادم). استبدله بطبقة شبكة عند ربط الـ Backend. */
object Repo {
    val BOOKING_AMOUNT: Long get() = cfg.bookingAmount.toLong()
    val VAT_AMOUNT: Long get() = cfg.bookingVat.toLong()
    val SUBSCRIPTION_AMOUNT: Long get() = cfg.subscriptionAmount.toLong()
    val SUBSCRIPTION_VAT: Long get() = cfg.subscriptionVat.toLong()
    const val DEMO_OTP = "1234"
    val HOLD_DAYS: Int get() = cfg.holdDays

    private lateinit var sp: SharedPreferences
    private lateinit var appCtx: Context
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    var lang by mutableStateOf("ar")
    var themeMode by mutableStateOf("system")
    var notifications by mutableStateOf(true)
    var langChosen by mutableStateOf(false)
    var phone by mutableStateOf<String?>(null)
    var guest by mutableStateOf(false)
    var lastVersionSeen by mutableStateOf("")
    var userLat by mutableStateOf(Double.NaN)
    var userLng by mutableStateOf(Double.NaN)
    var prefs by mutableStateOf(SearchPrefs())

    val priorities = mutableStateListOf<String>()
    val accounts = mutableStateListOf<Account>()
    val favorites = mutableStateListOf<String>()
    val offers = mutableStateListOf<Offer>()
    val ads = mutableStateListOf<Property>()
    val threads = mutableStateListOf<ChatThread>()
    val messages = mutableStateListOf<Msg>()
    val recent = mutableStateListOf<String>()
    val reports = mutableStateListOf<Report>()
    val hidden = mutableStateMapOf<String, Long>()

    // ---- حالة الخادم (تُملأ عند ضبط API_BASE_URL) ----
    var cfg by mutableStateOf(Cfg())
    val announcements = mutableStateListOf<Announcement>()
    val remoteProps = mutableStateListOf<Property>()
    val remoteFaqs = mutableStateListOf<FaqDto>()
    var remotePages by mutableStateOf<Map<String, PageDto>>(emptyMap())
    val news = mutableStateListOf<NewsDto>()
    val tickets = mutableStateListOf<TicketDto>()
    var netError by mutableStateOf("")
    var otpToken = ""
    var otpDebug by mutableStateOf("")
    var token: String? = null

    // مسودات مؤقتة بين الشاشات (غير محفوظة)
    var regName = ""
    var regCode = "+966"
    var regPhone = ""
    var regRole: Role = Role.CLIENT
    var otpPurpose = ""
    var newPhone = ""
    var forgotPhone = ""
    var dFloor = ""
    var dClientName = ""
    var dClientPhone = ""
    var dAmount = 0L

    val me: Account? get() = accounts.firstOrNull { it.phone == phone }
    val role: Role get() = me?.role ?: Role.CLIENT
    val loggedIn: Boolean get() = me != null

    fun init(ctx: Context) {
        appCtx = ctx.applicationContext
        sp = appCtx.getSharedPreferences("bawabat", Context.MODE_PRIVATE)
        lang = sp.getString("lang", "ar") ?: "ar"
        themeMode = sp.getString("theme", "system") ?: "system"
        notifications = sp.getBoolean("notifications", true)
        langChosen = sp.getBoolean("langChosen", false)
        phone = sp.getString("phone", null)
        lastVersionSeen = sp.getString("lastVersion", "") ?: ""
        prefs = try { json.decodeFromString<SearchPrefs>(sp.getString("prefs", null) ?: "") } catch (e: Exception) { SearchPrefs() }
        accounts.addAll(readList<Account>("accounts"))
        favorites.addAll(readList<String>("favorites"))
        offers.addAll(readList<Offer>("offers"))
        ads.addAll(readList<Property>("ads"))
        threads.addAll(readList<ChatThread>("threads"))
        messages.addAll(readList<Msg>("messages"))
        recent.addAll(readList<String>("recent"))
        reports.addAll(readList<Report>("reports"))
        priorities.addAll(readList<String>("priorities"))
        try {
            hidden.putAll(json.decodeFromString<Map<String, Long>>(sp.getString("hidden", null) ?: "{}"))
        } catch (e: Exception) { /* ignore */ }
        if (phone != null && me == null) phone = null
        token = sp.getString("token", null)
        Api.token = token
        if (Api.enabled && token == null) phone = null
    }

    private inline fun <reified T> readList(key: String): List<T> =
        try { json.decodeFromString<List<T>>(sp.getString(key, null) ?: "[]") } catch (e: Exception) { emptyList() }

    fun persist() {
        sp.edit()
            .putString("lang", lang).putString("theme", themeMode)
            .putBoolean("notifications", notifications).putBoolean("langChosen", langChosen)
            .putString("phone", phone).putString("lastVersion", lastVersionSeen).putString("token", token)
            .putString("prefs", json.encodeToString(prefs))
            .putString("accounts", json.encodeToString(accounts.toList()))
            .putString("favorites", json.encodeToString(favorites.toList()))
            .putString("offers", json.encodeToString(offers.toList()))
            .putString("ads", json.encodeToString(ads.toList()))
            .putString("threads", json.encodeToString(threads.toList()))
            .putString("messages", json.encodeToString(messages.toList()))
            .putString("recent", json.encodeToString(recent.toList()))
            .putString("reports", json.encodeToString(reports.toList()))
            .putString("priorities", json.encodeToString(priorities.toList()))
            .putString("hidden", json.encodeToString(hidden.toMap()))
            .apply()
    }

    fun uid(): String = UUID.randomUUID().toString().take(12)

    // ---------- الإعدادات ----------
    fun setLanguage(l: String) { lang = l; langChosen = true; persist() }
    fun setTheme(m: String) { themeMode = m; persist() }
    fun setNotif(v: Boolean) { notifications = v; persist() }
    fun markVersionSeen(v: String) { lastVersionSeen = v; persist() }

    // ---------- الحسابات ----------
    fun phoneExists(p: String): Boolean = accounts.any { it.phone == p }

    fun register(phoneFull: String, name: String, role: Role, password: String): Boolean {
        if (phoneExists(phoneFull)) return false
        val salt = uid() + uid()
        accounts.add(Account(phoneFull, name.trim(), role, salt, Validation.sha256(salt + password)))
        phone = phoneFull
        guest = false
        defaultPriorities(role)
        seedThreads(phoneFull)
        persist()
        return true
    }

    fun login(phoneFull: String, password: String): Boolean {
        val a = accounts.firstOrNull { it.phone == phoneFull } ?: return false
        if (a.pwHash != Validation.sha256(a.salt + password)) return false
        phone = phoneFull
        guest = false
        if (priorities.isEmpty()) defaultPriorities(a.role)
        seedThreads(phoneFull)
        persist()
        return true
    }

    fun checkPassword(pw: String): Boolean {
        val a = me ?: return false
        return a.pwHash == Validation.sha256(a.salt + pw)
    }

    fun setPassword(forPhone: String, pw: String) {
        val i = accounts.indexOfFirst { it.phone == forPhone }
        if (i < 0) return
        val salt = uid() + uid()
        accounts[i] = accounts[i].copy(salt = salt, pwHash = Validation.sha256(salt + pw))
        persist()
    }

    fun updateMe(transform: (Account) -> Account) {
        val i = accounts.indexOfFirst { it.phone == phone }
        if (i >= 0) {
            val old = accounts[i]
            val new = transform(old)
            accounts[i] = new
            persist()
            if (Api.enabled) Remote.profile(old, new)
        }
    }

    // ---------- جلسة الخادم ----------
    fun setSession(t: String, user: Account) {
        token = t
        Api.token = t
        val i = accounts.indexOfFirst { it.phone == user.phone }
        if (i >= 0) accounts[i] = user else accounts.add(user)
        phone = user.phone
        guest = false
        if (priorities.isEmpty()) defaultPriorities(user.role)
        persist()
    }

    fun setToken(t: String) { token = t; Api.token = t; persist() }

    fun applyUser(user: Account) {
        val old = phone
        val i = accounts.indexOfFirst { it.phone == old }
        if (i >= 0) accounts[i] = user else accounts.add(user)
        phone = user.phone
        persist()
    }

    fun clearSession() {
        phone = null
        guest = false
        token = null
        Api.token = null
        if (Api.enabled) {
            offers.clear(); threads.clear(); messages.clear(); favorites.clear(); tickets.clear()
        }
        persist()
    }

    fun deleteAccountLocal() {
        deleteAccount()
        clearSession()
    }

    fun changePhone(newPhoneFull: String) {
        val old = phone ?: return
        val i = accounts.indexOfFirst { it.phone == old }
        if (i < 0) return
        accounts[i] = accounts[i].copy(phone = newPhoneFull)
        for (k in threads.indices) if (threads[k].owner == old) threads[k] = threads[k].copy(owner = newPhoneFull)
        for (k in offers.indices) if (offers[k].byPhone == old) offers[k] = offers[k].copy(byPhone = newPhoneFull)
        for (k in ads.indices) if (ads[k].ownerPhone == old) ads[k] = ads[k].copy(ownerPhone = newPhoneFull)
        for (k in favorites.indices) if (favorites[k].startsWith("$old|")) favorites[k] = "$newPhoneFull|" + favorites[k].substringAfter("|")
        phone = newPhoneFull
        persist()
    }

    fun logout() {
        if (Api.enabled) clearSession() else { phone = null; guest = false; persist() }
    }

    fun deleteAccount() {
        val p = phone ?: return
        me?.avatar?.takeIf { it.isNotEmpty() }?.let { File(it).delete() }
        ads.filter { it.ownerPhone == p }.forEach { ad -> ad.photos.forEach { File(it).delete() } }
        val mine = threads.filter { it.owner == p }.map { it.id }.toSet()
        messages.removeAll { it.threadId in mine }
        threads.removeAll { it.owner == p }
        offers.removeAll { it.byPhone == p }
        ads.removeAll { it.ownerPhone == p }
        favorites.removeAll { it.startsWith("$p|") }
        accounts.removeAll { it.phone == p }
        phone = null
        guest = false
        persist()
    }

    private fun defaultPriorities(role: Role) {
        priorities.clear()
        priorities.addAll(
            if (role == Role.BROKER) listOf(Crit.COMMISSION, Crit.PRICE, Crit.AREA, Crit.ROOMS).map { it.name }
            else listOf(Crit.PRICE, Crit.AREA, Crit.ROOMS).map { it.name }
        )
    }

    fun setPriorities(list: List<String>) { priorities.clear(); priorities.addAll(list); persist() }

    // ---------- المفضلة ----------
    private fun favKey(id: String) = "$phone|$id"
    fun isFav(id: String): Boolean = phone != null && favorites.contains(favKey(id))
    fun toggleFav(id: String) {
        if (!loggedIn) return
        val k = favKey(id)
        if (!favorites.remove(k)) favorites.add(k)
        persist()
        if (Api.enabled) Remote.fav(id, favorites.contains(k))
    }
    fun myFavorites(): List<Property> =
        favorites.filter { it.startsWith("$phone|") }.mapNotNull { findProperty(it.substringAfter("|")) }

    // ---------- العقارات ----------
    fun allProperties(): List<Property> =
        if (Api.enabled) remoteProps.filter { it.status == AdStatus.PUBLISHED }
        else Demo.properties + ads.filter { it.status == AdStatus.PUBLISHED }
    fun findProperty(id: String): Property? =
        if (Api.enabled) remoteProps.firstOrNull { it.id == id }
        else Demo.properties.firstOrNull { it.id == id } ?: ads.firstOrNull { it.id == id }
    fun visibleProperties(now: Long = System.currentTimeMillis()): List<Property> =
        allProperties().filter { (hidden[it.id] ?: 0L) < now }
    fun myAds(): List<Property> =
        if (Api.enabled) remoteProps.filter { it.ownerPhone.isNotEmpty() && it.ownerPhone == phone }
        else ads.filter { it.ownerPhone == phone }

    fun applyPrefs(list: List<Property>, f: SearchPrefs = prefs): List<Property> {
        val typeOpts = Demo.types.filter { it.key in f.types }
        val q = Validation.normalizeDigits(f.query).trim()
        return list.filter { p ->
            (typeOpts.isEmpty() || typeOpts.any { it.matches(p) }) &&
                p.price in f.minPrice..f.maxPrice &&
                (f.facade.isEmpty() || p.facade == f.facade) &&
                (f.views.isEmpty() || p.view in f.views) &&
                (!f.maid || p.maid) &&
                (f.minBaths == 0 || p.baths >= f.minBaths) &&
                (f.districts.isEmpty() || p.district in f.districts) &&
                (!f.nearMe || userLat.isNaN() || Validation.distanceKm(userLat, userLng, p.lat, p.lng) <= 25.0) &&
                (q.isEmpty() || p.district.contains(q) || p.city.contains(q) || p.description.contains(q) ||
                    Demo.typeLabel(p, "ar").contains(q) || Demo.typeLabel(p, "en").contains(q, ignoreCase = true))
        }
    }

    fun sortBy(c: Crit, list: List<Property>): List<Property> = when (c) {
        Crit.PRICE -> list.sortedBy { it.price }
        Crit.AREA -> list.sortedByDescending { it.area }
        Crit.ROOMS -> list.sortedByDescending { it.rooms }
        Crit.BATHS -> list.sortedByDescending { it.baths }
        Crit.COMMISSION -> list.sortedByDescending { it.commission }
    }

    fun setPrefs(p: SearchPrefs) { prefs = p; persist() }

    fun addRecent(q: String) {
        val s = q.trim()
        if (s.isEmpty()) return
        recent.remove(s)
        recent.add(0, s)
        while (recent.size > 8) recent.removeAt(recent.size - 1)
        persist()
    }
    fun removeRecent(q: String) { recent.remove(q); persist() }
    fun clearRecent() { recent.clear(); persist() }

    // ---------- العروض ----------
    fun addOffer(o: Offer) {
        offers.add(0, o)
        hidden[o.propertyId] = o.createdAt + HOLD_DAYS * 86_400_000L
        persist()
    }
    fun myOffers(): List<Offer> = offers.filter { it.byPhone == phone }
    fun offersOnMyAds(): List<Offer> {
        val ids = myAds().map { it.id }.toSet()
        return offers.filter { it.propertyId in ids }
    }
    fun setOfferStatus(id: String, s: OfferStatus) {
        val i = offers.indexOfFirst { it.id == id }
        if (i >= 0) {
            offers[i] = offers[i].copy(status = s)
            if (s == OfferStatus.REJECTED) hidden.remove(offers[i].propertyId)
            persist()
        }
    }

    // ---------- الإعلانات ----------
    fun addAd(p: Property) {
        if (Api.enabled) { remoteProps.add(0, p); persist(); Remote.saveAd(p, true) } else { ads.add(0, p); persist() }
    }
    fun updateAd(p: Property) {
        if (Api.enabled) {
            val j = remoteProps.indexOfFirst { it.id == p.id }
            if (j >= 0) { val old = remoteProps[j]; remoteProps[j] = p; persist(); Remote.updateAd(old, p) }
            return
        }
        val i = ads.indexOfFirst { it.id == p.id }
        if (i >= 0) { ads[i] = p; persist() }
    }
    fun deleteAd(id: String) {
        if (Api.enabled) { remoteProps.removeAll { it.id == id }; persist(); Remote.deleteAd(id); return }
        ads.firstOrNull { it.id == id }?.photos?.forEach { File(it).delete() }
        ads.removeAll { it.id == id }
        persist()
    }
    fun setAdStatus(id: String, s: AdStatus) {
        val i = ads.indexOfFirst { it.id == id }
        if (i >= 0) {
            ads[i] = ads[i].copy(status = s, publishedAt = if (s == AdStatus.PUBLISHED) System.currentTimeMillis() else ads[i].publishedAt)
            persist()
        }
    }
    fun addReport(propertyId: String, reason: String) {
        reports.add(Report(propertyId, reason, phone ?: "guest", System.currentTimeMillis()))
        persist()
        if (Api.enabled) Remote.report(propertyId, reason)
    }

    // ---------- المحادثات ----------
    fun myThreads(): List<ChatThread> = threads.filter { it.owner == phone && !it.deleted }
    fun threadMessages(id: String): List<Msg> = messages.filter { it.threadId == id }
    fun lastMessage(id: String): Msg? = messages.lastOrNull { it.threadId == id && !it.deleted }

    fun openThread(peer: String, propertyId: String): ChatThread {
        val p = phone ?: ""
        threads.firstOrNull { it.owner == p && it.peer == peer && it.propertyId == propertyId && !it.deleted }?.let { return it }
        val t = ChatThread(uid(), p, peer, propertyId)
        threads.add(0, t)
        persist()
        if (Api.enabled) Remote.openThread(t)
        return t
    }
    fun updateThread(id: String, f: (ChatThread) -> ChatThread) {
        val i = threads.indexOfFirst { it.id == id }
        if (i >= 0) {
            val old = threads[i]
            val new = f(old)
            threads[i] = new
            persist()
            if (Api.enabled) Remote.threadChanged(old, new)
        }
    }
    fun sendMessage(threadId: String, text: String, file: String = "") {
        val m = Msg(uid(), threadId, text, true, System.currentTimeMillis(), file)
        messages.add(m)
        persist()
        if (Api.enabled) Remote.sendMessage(m)
    }
    fun editMessage(id: String, text: String) {
        val i = messages.indexOfFirst { it.id == id }
        if (i >= 0) { messages[i] = messages[i].copy(text = text, edited = true); persist(); if (Api.enabled) Remote.editMessage(id, text) }
    }
    fun deleteMessage(id: String) {
        val i = messages.indexOfFirst { it.id == id }
        if (i >= 0) { messages[i] = messages[i].copy(deleted = true, text = "", file = ""); persist(); if (Api.enabled) Remote.deleteMessage(id) }
    }

    // ---------- عمليات تعمل في الوضعين (محلي / خادم) ----------
    suspend fun doRequestOtp(phoneFull: String, purpose: String): Remote.OtpStart {
        otpDebug = ""
        if (Api.enabled) {
            val r = Remote.requestOtp(phoneFull, purpose)
            otpDebug = r.debugCode
            return r
        }
        return when (purpose) {
            "register", "change_phone" -> if (phoneExists(phoneFull)) Remote.OtpStart("هذا الرقم لديه حساب بالفعل") else Remote.OtpStart(null)
            "reset" -> if (!phoneExists(phoneFull)) Remote.OtpStart("هذا الرقم غير مسجل") else Remote.OtpStart(null)
            else -> Remote.OtpStart(null)
        }
    }

    suspend fun doVerifyOtp(phoneFull: String, purpose: String, code: String): String? =
        if (Api.enabled) Remote.verifyOtp(phoneFull, purpose, code)
        else if (code == DEMO_OTP) null else "الرمز خاطئ يرجى المحاولة مرة أخرى"

    suspend fun doRegister(phoneFull: String, name: String, role: Role, password: String): String? =
        if (Api.enabled) Remote.register(phoneFull, name, role, password)
        else if (register(phoneFull, name, role, password)) null else "هذا الرقم لديه حساب بالفعل"

    /** null = نجاح، "" = خطأ عام، غير ذلك = رسالة الخادم */
    suspend fun doLogin(phoneFull: String, password: String): String? =
        if (Api.enabled) Remote.login(phoneFull, password)
        else if (login(phoneFull, password)) null else ""

    suspend fun doResetPassword(phoneFull: String, password: String): String? =
        if (Api.enabled) Remote.resetPassword(phoneFull, password) else { setPassword(phoneFull, password); null }

    suspend fun doChangePhone(newPhoneFull: String): String? =
        if (Api.enabled) Remote.changePhone(newPhoneFull) else { changePhone(newPhoneFull); null }

    suspend fun doChangePassword(current: String, next: String): String? =
        if (Api.enabled) Remote.changePassword(current, next)
        else if (!checkPassword(current)) "كلمة المرور الحالية غير صحيحة" else { setPassword(phone ?: "", next); null }

    suspend fun doDeleteAccount(password: String): String? =
        if (Api.enabled) Remote.deleteAccount(password) else { deleteAccount(); null }

    suspend fun doSubscribe(): String? {
        if (Api.enabled) return Remote.subscribe()
        val total = SUBSCRIPTION_AMOUNT + SUBSCRIPTION_VAT
        val ok = try { Payments.gateway.charge(total, "subscription") } catch (e: Exception) { false }
        if (!ok) return "حدث خطأ برجاء المحاولة مرة أخرى"
        updateMe { it.copy(subscribed = true) }
        return null
    }

    suspend fun doBook(propertyId: String): String? {
        if (Api.enabled) return Remote.book(propertyId)
        val p = findProperty(propertyId) ?: return "الإعلان غير متاح"
        val total = BOOKING_AMOUNT + VAT_AMOUNT
        val ok = try { Payments.gateway.charge(total, "booking:$propertyId") } catch (e: Exception) { false }
        if (!ok) return "حدث خطأ برجاء المحاولة مرة أخرى"
        val m = me
        val isClient = role == Role.CLIENT
        addOffer(
            Offer(
                id = uid(), propertyId = p.id, floor = dFloor, amount = dAmount,
                clientName = if (isClient) (m?.name ?: "") else dClientName,
                clientPhone = if (isClient) (m?.phone ?: "") else dClientPhone,
                byPhone = phone ?: "", status = OfferStatus.PENDING, createdAt = System.currentTimeMillis()
            )
        )
        return null
    }

    suspend fun doSupport(subject: String, body: String): String? =
        if (Api.enabled) Remote.support(subject, body) else null

    private fun seedThreads(p: String) {
        if (threads.any { it.owner == p }) return
        val t = ChatThread(uid(), p, "شركة بداية للتطوير العقاري (تجريبي)", "p1", unread = true)
        threads.add(t)
        messages.add(
            Msg(uid(), t.id, "مرحبًا بك في بوابة العروض. هذه محادثة تجريبية، المحادثات الحقيقية تتطلب ربط الخادم.",
                false, System.currentTimeMillis() - 3_600_000L)
        )
    }
}
