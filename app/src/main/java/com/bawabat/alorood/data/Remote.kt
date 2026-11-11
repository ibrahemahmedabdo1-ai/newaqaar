package com.bawabat.alorood.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File

/** طبقة المزامنة مع الخادم: تحديث محلي فوري (optimistic) ثم إرسال للخادم ثم سحب الحالة الرسمية. */
object Remote {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pullMutex = Mutex()

    class OtpStart(val error: String?, val debugCode: String = "")

    private fun errText(e: Exception): String =
        if (e is Api.ApiException) (e.message ?: e.code) else "تعذر الاتصال بالخادم"

    // ---------- المزامنة ----------
    suspend fun pull() {
        if (!Api.enabled) return
        pullMutex.withLock {
            try {
                val el = Api.call("GET", "/api/v1/sync")
                val r = Api.json.decodeFromJsonElement(SyncResp.serializer(), el)
                withContext(Dispatchers.Main) { apply(r) }
            } catch (e: Api.ApiException) {
                if (e.status == 401 && Api.token != null) withContext(Dispatchers.Main) { Repo.clearSession() }
                if (e.status == 403 && e.code.startsWith("ACCOUNT_")) {
                    withContext(Dispatchers.Main) { Repo.netError = e.message ?: ""; Repo.clearSession() }
                }
            } catch (e: Exception) {
                // بدون اتصال: نبقي النسخة المخزنة
            }
        }
    }

    private fun apply(r: SyncResp) {
        Repo.cfg = r.config
        Repo.announcements.clear(); Repo.announcements.addAll(r.announcements)
        val me = r.me
        if (me != null) {
            val i = Repo.accounts.indexOfFirst { it.phone == me.phone }
            if (i >= 0) Repo.accounts[i] = me else Repo.accounts.add(me)
            Repo.phone = me.phone
        }
        Repo.remoteProps.clear(); Repo.remoteProps.addAll(r.properties)
        Repo.hidden.clear(); Repo.hidden.putAll(r.hidden)
        val p = Repo.phone
        if (p != null) {
            Repo.favorites.removeAll { it.startsWith("$p|") }
            Repo.favorites.addAll(r.favorites.map { "$p|$it" })
        }
        Repo.offers.clear(); Repo.offers.addAll(r.offers)
        Repo.threads.clear(); Repo.threads.addAll(r.threads)
        Repo.messages.clear(); Repo.messages.addAll(r.messages)
        Repo.tickets.clear(); Repo.tickets.addAll(r.tickets)
        Repo.remoteFaqs.clear(); Repo.remoteFaqs.addAll(r.faqs)
        Repo.remotePages = r.pages
        Repo.news.clear(); Repo.news.addAll(r.news)
        if (r.types.isNotEmpty()) Demo.types = r.types.map { TypeOpt(it.key, it.kind, it.rooms, it.plus, it.labelAr, it.labelEn) }
        if (r.districts.isNotEmpty()) Demo.districts = r.districts.map { District(it.name, it.lat, it.lng) }
        Repo.persist()
    }

    /** تنفيذ طلب شبكة في الخلفية ثم مزامنة الحالة الرسمية (يُصحح أي تحديث محلي فشل). */
    private fun launchNet(block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Repo.netError = errText(e) }
            }
            pull()
        }
    }

    private suspend fun uploadFile(path: String, folder: String): String {
        val bytes = withContext(Dispatchers.IO) { File(path).readBytes() }
        return Api.upload(folder, bytes, "image/jpeg")
    }

    // ---------- تعديلات تُرسل في الخلفية ----------
    fun fav(id: String, on: Boolean) = launchNet { Api.call(if (on) "PUT" else "DELETE", "/api/v1/favorites/$id") }

    fun report(propertyId: String, reason: String) = launchNet {
        Api.call("POST", "/api/v1/properties/$propertyId/report", buildJsonObject { put("reason", reason) })
    }

    fun openThread(t: ChatThread) = launchNet {
        Api.call("POST", "/api/v1/threads", buildJsonObject {
            put("id", t.id)
            if (t.propertyId.isNotEmpty()) put("propertyId", t.propertyId) else put("peerName", t.peer)
        })
    }

    fun threadChanged(old: ChatThread, new: ChatThread) {
        val body = buildJsonObject {
            if (old.blocked != new.blocked) put("blocked", new.blocked)
            if (!old.deleted && new.deleted) put("deleted", true)
            if (old.unread && !new.unread) put("read", true)
        }
        if (body.isNotEmpty()) launchNet { Api.call("PATCH", "/api/v1/threads/${new.id}", body) }
    }

    fun sendMessage(m: Msg) = launchNet {
        val url = if (m.file.startsWith("/")) uploadFile(m.file, "chat") else m.file
        Api.call("POST", "/api/v1/threads/${m.threadId}/messages", buildJsonObject {
            put("id", m.id); put("text", m.text); put("fileUrl", url)
        })
    }

    fun editMessage(id: String, text: String) = launchNet {
        Api.call("PATCH", "/api/v1/messages/$id", buildJsonObject { put("text", text) })
    }

    fun deleteMessage(id: String) = launchNet { Api.call("DELETE", "/api/v1/messages/$id") }

    fun saveAd(p: Property, create: Boolean) = launchNet {
        val urls = p.photos.map { if (it.startsWith("/")) uploadFile(it, "ads") else it }
        val body = Api.json.encodeToJsonElement(Property.serializer(), p.copy(photos = urls))
        if (create) Api.call("POST", "/api/v1/properties", body) else Api.call("PUT", "/api/v1/properties/${p.id}", body)
        p.photos.filter { it.startsWith("/") }.forEach { f -> try { File(f).delete() } catch (e: Exception) { /* ignore */ } }
    }

    fun updateAd(old: Property, new: Property) {
        if (new.status == AdStatus.ENDED && old.status != AdStatus.ENDED) {
            launchNet {
                Api.call("POST", "/api/v1/properties/${new.id}/end", buildJsonObject { if (new.soldTo.isNotEmpty()) put("soldTo", new.soldTo) })
            }
        } else saveAd(new, false)
    }

    fun deleteAd(id: String) = launchNet { Api.call("DELETE", "/api/v1/properties/$id") }

    fun profile(old: Account, new: Account) = launchNet {
        val body = buildJsonObject {
            if (old.name != new.name) put("name", new.name)
            if (old.avatar != new.avatar && new.avatar.isNotEmpty()) {
                put("avatarUrl", if (new.avatar.startsWith("/")) uploadFile(new.avatar, "avatars") else new.avatar)
            }
        }
        if (body.isNotEmpty()) Api.call("PATCH", "/api/v1/me", body)
    }

    // ---------- المصادقة (تنتظر الخادم) ----------
    suspend fun requestOtp(phone: String, purpose: String): OtpStart = try {
        val el = Api.call("POST", "/api/v1/auth/otp/request", buildJsonObject { put("phone", phone); put("purpose", purpose) })
        OtpStart(null, el.jsonObject["debugCode"]?.jsonPrimitive?.contentOrNull ?: "")
    } catch (e: Exception) { OtpStart(errText(e)) }

    suspend fun verifyOtp(phone: String, purpose: String, code: String): String? = try {
        val el = Api.call("POST", "/api/v1/auth/otp/verify", buildJsonObject { put("phone", phone); put("purpose", purpose); put("code", code) })
        Repo.otpToken = el.jsonObject["otpToken"]?.jsonPrimitive?.contentOrNull ?: ""
        null
    } catch (e: Exception) { errText(e) }

    private suspend fun startSession(el: JsonElement) {
        val o = el.jsonObject
        val token = o["token"]?.jsonPrimitive?.contentOrNull ?: return
        val user = Api.json.decodeFromJsonElement(Account.serializer(), o["user"] ?: JsonObject(emptyMap()))
        withContext(Dispatchers.Main) { Repo.setSession(token, user) }
        pull()
    }

    suspend fun register(phone: String, name: String, role: Role, password: String): String? = try {
        val el = Api.call("POST", "/api/v1/auth/register", buildJsonObject {
            put("phone", phone); put("name", name); put("role", role.name); put("password", password); put("otpToken", Repo.otpToken)
        })
        startSession(el); null
    } catch (e: Exception) { errText(e) }

    suspend fun login(phone: String, password: String): String? = try {
        val el = Api.call("POST", "/api/v1/auth/login", buildJsonObject { put("phone", phone); put("password", password) })
        startSession(el); null
    } catch (e: Exception) { errText(e) }

    suspend fun resetPassword(phone: String, password: String): String? = try {
        Api.call("POST", "/api/v1/auth/reset", buildJsonObject { put("phone", phone); put("password", password); put("otpToken", Repo.otpToken) })
        null
    } catch (e: Exception) { errText(e) }

    suspend fun changePhone(newPhone: String): String? = try {
        val el = Api.call("POST", "/api/v1/me/phone", buildJsonObject { put("newPhone", newPhone); put("otpToken", Repo.otpToken) })
        val user = Api.json.decodeFromJsonElement(Account.serializer(), el)
        withContext(Dispatchers.Main) { Repo.applyUser(user) }
        pull(); null
    } catch (e: Exception) { errText(e) }

    suspend fun changePassword(current: String, next: String): String? = try {
        val el = Api.call("POST", "/api/v1/me/password", buildJsonObject { put("current", current); put("next", next) })
        el.jsonObject["token"]?.jsonPrimitive?.contentOrNull?.let { t -> withContext(Dispatchers.Main) { Repo.setToken(t) } }
        null
    } catch (e: Exception) { errText(e) }

    suspend fun deleteAccount(password: String): String? = try {
        Api.call("DELETE", "/api/v1/me", buildJsonObject { put("password", password) })
        withContext(Dispatchers.Main) { Repo.deleteAccountLocal() }
        null
    } catch (e: Exception) { errText(e) }

    suspend fun subscribe(): String? = try {
        val el = Api.call("POST", "/api/v1/subscription")
        val user = Api.json.decodeFromJsonElement(Account.serializer(), el)
        withContext(Dispatchers.Main) { Repo.applyUser(user) }
        null
    } catch (e: Exception) { errText(e) }

    suspend fun book(propertyId: String): String? = try {
        Api.call("POST", "/api/v1/offers", buildJsonObject {
            put("id", Repo.uid()); put("propertyId", propertyId); put("floor", Repo.dFloor); put("amount", Repo.dAmount)
            if (Repo.dClientName.isNotEmpty()) put("clientName", Repo.dClientName)
            if (Repo.dClientPhone.isNotEmpty()) put("clientPhone", Repo.dClientPhone)
        })
        pull(); null
    } catch (e: Exception) { errText(e) }

    suspend fun support(subject: String, body: String): String? = try {
        Api.call("POST", "/api/v1/support", buildJsonObject { put("subject", subject); put("body", body) })
        pull(); null
    } catch (e: Exception) { errText(e) }
}
