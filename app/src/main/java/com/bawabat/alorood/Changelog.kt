package com.bawabat.alorood

/** سجل التحديثات داخل التطبيق. حدّثه مع كل إصدار (وحدّث CHANGELOG.md و version.properties). */
object Changelog {
    class Release(val version: String, val ar: List<String>, val en: List<String>)

    val releases = listOf(
        Release(
            "1.1.0",
            ar = listOf(
                "ربط التطبيق بلوحة التحكم (الخادم): حسابات حقيقية ورمز تأكيد ومزامنة",
                "مراجعة الإعلانات والموافقة عليها من الإدارة، وصور الإعلانات على الخادم",
                "إعدادات الرسوم ومدة الحجز والصيانة والتحديث الإجباري من لوحة التحكم",
                "أخبار العقارات والأسئلة الشائعة والصفحات القانونية تُدار من اللوحة",
                "رسائل من الإدارة داخل التطبيق وردود الدعم الفني"
            ),
            en = listOf(
                "App linked to the admin panel (server): real accounts, verification codes and sync",
                "Ad moderation by the admin team, ad photos stored on the server",
                "Fees, hold period, maintenance and forced-update controlled from the panel",
                "Real-estate news, FAQ and legal pages managed from the panel",
                "In-app announcements and support replies"
            )
        ),
        Release(
            "1.0.0",
            ar = listOf(
                "الإصدار الأول من بوابة العروض: مشتري / سمسار / مطور عقاري",
                "بحث وفلاتر وترتيب أولويات، وتفاصيل العقار",
                "التفاوض وتقديم العروض وحجز الدور",
                "المحادثات والإبلاغ والحظر، وإدارة الإعلانات للمطور",
                "عربي RTL وEnglish، وسمة فاتحة وداكنة"
            ),
            en = listOf(
                "First release of Bawabat Alorood: buyer / broker / developer",
                "Search, filters, priority ranking and property details",
                "Negotiation, offers and floor booking",
                "Chats, reporting and blocking, ad management for developers",
                "Arabic RTL and English, light and dark themes"
            )
        )
    )

    fun forVersion(v: String): Release? = releases.firstOrNull { it.version == v }
}
