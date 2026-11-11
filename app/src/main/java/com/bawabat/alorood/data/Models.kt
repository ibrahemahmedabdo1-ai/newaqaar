package com.bawabat.alorood.data

import kotlinx.serialization.Serializable

@Serializable enum class Role { CLIENT, BROKER, DEVELOPER }
@Serializable enum class FloorState { AVAILABLE, RESERVED, SOLD }
@Serializable enum class OfferStatus { PENDING, ACTIVE, REJECTED }
@Serializable enum class AdStatus { PUBLISHED, PENDING, ENDED, REJECTED }

@Serializable
data class Account(
    val phone: String,
    val name: String,
    val role: Role,
    val salt: String,
    val pwHash: String,
    val subscribed: Boolean = false,
    val avatar: String = ""
)

@Serializable
data class Floor(val name: String, val state: FloorState)

@Serializable
data class Property(
    val id: String,
    val kind: String,            // apt | annex | villa | duplex | office | townhouse
    val rooms: Int,
    val baths: Int,
    val area: Int,
    val price: Long,
    val minOffer: Long,          // أقل عرض يقبله المالك
    val commission: Long,
    val district: String,
    val city: String,
    val description: String,
    val facade: String,          // front | back
    val direction: String,       // شمالية ...
    val view: String,            // city | garden | sea
    val maid: Boolean,
    val driver: Boolean = false,
    val parking: Boolean = true,
    val floors: List<Floor>,
    val guarantees: List<String>,
    val amenities: List<String>,
    val infra: List<String>,
    val publisher: String,
    val ownerPhone: String = "",
    val contactPhone: String = "",
    val lat: Double,
    val lng: Double,
    val seed: Int,
    val photos: List<String> = emptyList(),
    val status: AdStatus = AdStatus.PUBLISHED,
    val createdAt: Long = 0L,
    val publishedAt: Long = 0L,
    val soldTo: String = "",
    val rejectReason: String = ""
)

@Serializable
data class Offer(
    val id: String,
    val propertyId: String,
    val floor: String,
    val amount: Long,
    val clientName: String,
    val clientPhone: String,
    val byPhone: String,
    val status: OfferStatus,
    val createdAt: Long
)

@Serializable
data class ChatThread(
    val id: String,
    val owner: String,
    val peer: String,
    val propertyId: String = "",
    val unread: Boolean = false,
    val blocked: Boolean = false,
    val deleted: Boolean = false
)

@Serializable
data class Msg(
    val id: String,
    val threadId: String,
    val text: String,
    val mine: Boolean,
    val time: Long,
    val file: String = "",
    val edited: Boolean = false,
    val deleted: Boolean = false
)

@Serializable
data class Report(val propertyId: String, val reason: String, val by: String, val time: Long)

@Serializable
data class SearchPrefs(
    val types: Set<String> = emptySet(),
    val minPrice: Long = 0L,
    val maxPrice: Long = 20_000_000L,
    val facade: String = "",
    val views: Set<String> = emptySet(),
    val maid: Boolean = false,
    val minBaths: Int = 0,
    val districts: Set<String> = emptySet(),
    val nearMe: Boolean = false,
    val query: String = ""
)

enum class Crit(val ar: String, val en: String) {
    PRICE("السعر", "Price"),
    AREA("المساحة", "Area"),
    ROOMS("الغرف", "Rooms"),
    BATHS("الحمامات", "Bathrooms"),
    COMMISSION("العمولة", "Commission")
}

data class TypeOpt(val key: String, val kind: String, val rooms: Int, val plus: Boolean, val ar: String, val en: String) {
    fun matches(p: Property): Boolean =
        p.kind == kind && (if (plus) p.rooms >= rooms else p.rooms == rooms)
}

data class District(val name: String, val lat: Double, val lng: Double)

// ---------- نماذج المزامنة مع الخادم ----------
@Serializable
data class Cfg(
    val bookingAmount: Int = 1000,
    val bookingVat: Int = 20,
    val subscriptionAmount: Int = 300,
    val subscriptionVat: Int = 20,
    val holdDays: Int = 3,
    val maintenance: Boolean = false,
    val maintenanceMessageAr: String = "",
    val maintenanceMessageEn: String = "",
    val minVersionCode: Int = 1,
    val latestVersionCode: Int = 1,
    val supportEmail: String = "",
    val supportWhatsapp: String = ""
)

@Serializable data class Announcement(val id: String = "", val titleAr: String = "", val titleEn: String = "", val bodyAr: String = "", val bodyEn: String = "", val level: String = "INFO")
@Serializable data class FaqDto(val qAr: String = "", val aAr: String = "", val qEn: String = "", val aEn: String = "")
@Serializable data class PageDto(val titleAr: String = "", val titleEn: String = "", val bodyAr: String = "", val bodyEn: String = "")
@Serializable data class NewsDto(val id: String = "", val titleAr: String = "", val titleEn: String = "", val bodyAr: String = "", val bodyEn: String = "", val imageUrl: String = "", val time: Long = 0L)
@Serializable data class TypeDto(val key: String, val kind: String, val rooms: Int, val plus: Boolean = false, val labelAr: String, val labelEn: String)
@Serializable data class DistrictDto(val name: String, val lat: Double, val lng: Double)
@Serializable data class TicketDto(val id: String = "", val subject: String = "", val status: String = "", val reply: String = "", val time: Long = 0L)

@Serializable
data class SyncResp(
    val serverTime: Long = 0L,
    val config: Cfg = Cfg(),
    val announcements: List<Announcement> = emptyList(),
    val me: Account? = null,
    val properties: List<Property> = emptyList(),
    val hidden: Map<String, Long> = emptyMap(),
    val favorites: List<String> = emptyList(),
    val offers: List<Offer> = emptyList(),
    val threads: List<ChatThread> = emptyList(),
    val messages: List<Msg> = emptyList(),
    val tickets: List<TicketDto> = emptyList(),
    val faqs: List<FaqDto> = emptyList(),
    val pages: Map<String, PageDto> = emptyMap(),
    val news: List<NewsDto> = emptyList(),
    val types: List<TypeDto> = emptyList(),
    val districts: List<DistrictDto> = emptyList()
)
