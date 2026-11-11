package com.bawabat.alorood.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * بيانات تجريبية للعرض فقط (DEMO_MODE). تُستبدل ببيانات الخادم عند ربط الـ Backend.
 * الإحداثيات تقريبية وتُستخدم فقط لفلتر "القريب مني".
 */
object Demo {
    private val defaultDistricts = listOf(
        District("الصفا", 21.5866, 39.2057), District("العزيزية", 21.5421, 39.2087),
        District("الروضة", 21.5640, 39.1530), District("الزهراء", 21.5800, 39.1250),
        District("الواحة", 21.5900, 39.1450), District("السلامة", 21.5780, 39.1480),
        District("المروة", 21.6100, 39.2150), District("الرحاب", 21.5980, 39.1700),
        District("الربوة", 21.5600, 39.1900), District("النسيم", 21.5000, 39.2300),
        District("النعيم", 21.6000, 39.1500), District("النزهه", 21.5700, 39.1600),
        District("النهضة", 21.6350, 39.1650), District("الحمراء", 21.5300, 39.1700),
        District("الفيصلية", 21.5200, 39.1800)
    )

    private val defaultTypes = listOf(
        TypeOpt("apt3", "apt", 3, false, "شقة 3 غرف", "Apartment 3 rooms"),
        TypeOpt("apt4", "apt", 4, false, "شقة 4 غرف", "Apartment 4 rooms"),
        TypeOpt("apt5", "apt", 5, false, "شقة 5 غرف", "Apartment 5 rooms"),
        TypeOpt("apt5p", "apt", 6, true, "شقة +5 غرف", "Apartment 5+ rooms"),
        TypeOpt("annex2", "annex", 2, false, "ملحق 2 غرف", "Annex 2 rooms"),
        TypeOpt("annex3", "annex", 3, false, "ملحق 3 غرف", "Annex 3 rooms"),
        TypeOpt("annex4", "annex", 4, false, "ملحق 4 غرف", "Annex 4 rooms"),
        TypeOpt("annex4p", "annex", 5, true, "ملحق +4 غرف", "Annex 4+ rooms")
    )

    /** تُستبدل بقوائم الخادم عند الاتصال (من لوحة التحكم). */
    var districts: List<District> by mutableStateOf(defaultDistricts)
    var types: List<TypeOpt> by mutableStateOf(defaultTypes)

    fun kindLabel(kind: String, lang: String): String = when (kind) {
        "annex" -> if (lang == "ar") "ملحق" else "Annex"
        "villa" -> if (lang == "ar") "فيلا" else "Villa"
        "duplex" -> if (lang == "ar") "دوبلكس" else "Duplex"
        "office" -> if (lang == "ar") "مكتب" else "Office"
        "townhouse" -> if (lang == "ar") "تاون هاوس" else "Townhouse"
        else -> if (lang == "ar") "شقة" else "Apartment"
    }

    fun typeLabel(p: Property, lang: String): String =
        kindLabel(p.kind, lang) + " " + p.rooms + (if (lang == "ar") " غرف" else " rooms")

    val floorNames = listOf("الدور الأول", "الدور الثاني", "الدور الثالث", "الدور الرابع", "الدور الخامس", "الدور السادس")

    val properties: List<Property> by lazy { build() }

    private fun build(): List<Property> {
        val rooms = intArrayOf(4, 3, 5, 4, 2, 6, 4, 3, 5, 3, 3, 4, 5, 4, 4)
        val prices = longArrayOf(550_000, 720_000, 1_000_000, 650_000, 480_000, 1_500_000, 590_000, 700_000,
            1_100_000, 430_000, 760_000, 640_000, 980_000, 740_000, 1_000_000)
        val descs = listOf(
            "شقة أمامية أربع غرف وثلاث دورات مياه وصالة ومطبخ وخزانات مستقلة وموقف خاص",
            "شقة بتشطيب فاخر، صالة واسعة ومطبخ راقي، قريبة من الخدمات والمدارس",
            "شقة بإطلالة مميزة، غرف نوم ماستر، ومدخل خاص مع موقف سيارتين"
        )
        val dirs = listOf("شمالية", "جنوبية", "شرقية", "غربية")
        val views = listOf("city", "garden", "sea")
        return List(15) { i ->
            val kind = if (i % 5 == 4) "annex" else "apt"
            val r = if (kind == "annex") (i % 4) + 2 else rooms[i]
            val price = prices[i]
            val d = districts[(i * 2) % districts.size]
            val maid = i % 2 == 0
            val driver = i % 4 == 0
            Property(
                id = "p${i + 1}", kind = kind, rooms = r,
                baths = if (r >= 5) 4 else if (r >= 4) 3 else 2,
                area = 90 + r * 10 + (i % 3) * 5,
                price = price, minOffer = ((price * 91 / 100) / 10_000) * 10_000,
                commission = price * 2 / 100,
                district = d.name, city = "جدة", description = descs[i % descs.size],
                facade = if (i % 2 == 0) "front" else "back", direction = dirs[i % 4], view = views[i % 3],
                maid = maid, driver = driver, parking = true,
                floors = floorNames.mapIndexed { idx, n ->
                    Floor(n, when ((idx + i) % 6) { 0 -> FloorState.SOLD; 3 -> FloorState.RESERVED; else -> FloorState.AVAILABLE })
                },
                guarantees = listOf("ضمان الهيكل الإنشائي 25 سنة", "ضمان القواطع والأفياش 20 سنة", "ضمان السباكة والكهرباء 5 سنوات"),
                amenities = buildList {
                    if (maid) add("غرفة خادمة"); if (driver) add("غرفة السائق"); add("موقف خاص"); add("خزانات مستقلة")
                },
                infra = listOf("شبكة المياه", "شبكة الصرف الصحي", "تصريف السيول", "الألياف البصرية"),
                publisher = "شركة بداية للتطوير العقاري (تجريبي)",
                lat = d.lat + (i % 3) * 0.002, lng = d.lng + (i % 4) * 0.002, seed = i + 1
            )
        }
    }

    class Faq(val qAr: String, val aAr: String, val qEn: String, val aEn: String)

    val faqs = listOf(
        Faq("كيف أقدّم عرضًا على عقار؟",
            "افتح العقار ثم اضغط «تفاوض مع المالك»، اختر الدور المتاح، أدخل مبلغ عرضك (لا يقل عن الحد الأدنى الذي حدده المالك) ثم أكمل دفع مبلغ الحجز.",
            "How do I make an offer?",
            "Open the property and tap “Negotiate with owner”, choose an available floor, enter your offer (not below the owner's minimum) and pay the booking amount."),
        Faq("ماذا يحدث لمبلغ الحجز؟",
            "إذا وافق المالك على عرضك يتم تحويل المبلغ لضمان الحجز، وإذا رفض العرض خلال 3 أيام يُسترد المبلغ بالكامل.",
            "What happens to the booking amount?",
            "If the owner accepts, the amount secures your booking. If the offer is rejected within 3 days it is fully refunded."),
        Faq("كيف أنشر إعلانًا؟",
            "من حساب «مطور عقاري» افتح «إعلاناتي» ثم «إضافة إعلان»، عبّئ البيانات وأضف الصور، وسيُراجع الإعلان من المنصة قبل نشره.",
            "How do I publish an ad?",
            "From a developer account open “My ads” then “Add ad”, fill in the details and photos. The platform reviews the ad before it is published."),
        Faq("كيف أبلغ عن إعلان مخالف؟",
            "من صفحة العقار اضغط «الإبلاغ عن الإعلان» واختر السبب. كما يمكنك حظر أي محادثة من قائمة المحادثات.",
            "How do I report a listing?",
            "On the property page tap “Report ad” and choose a reason. You can also block any chat from the chats list."),
        Faq("كيف أحذف حسابي وبياناتي؟",
            "من «الملف الشخصي» ثم «حذف الحساب». يتم حذف حسابك وإعلاناتك وطلباتك ومحادثاتك من الجهاز فورًا.",
            "How do I delete my account and data?",
            "Go to Profile then “Delete account”. Your account, ads, offers and chats are deleted immediately.")
    )

    fun terms(lang: String): String = if (lang == "ar") """
1. القبول: باستخدامك تطبيق «بوابة العروض» فإنك توافق على هذه الشروط.
2. الخدمة: التطبيق منصة تربط المشترين والوسطاء والمطورين العقاريين لتقديم عروض على عقارات معلنة. التطبيق ليس طرفًا في عقد البيع.
3. الحساب: أنت مسؤول عن صحة بياناتك وسرية كلمة المرور.
4. الإعلانات: يلتزم المعلن بصحة بيانات العقار وصوره وملكيته أو تفويضه بالتسويق. يحق للمنصة رفض أو إزالة أي إعلان مخالف.
5. العروض والحجز: مبلغ الحجز يُحوّل للمالك عند قبول العرض، ويُسترد كاملًا إذا رُفض العرض خلال 3 أيام، وفق ما يوضحه التطبيق وقت الدفع.
6. المحتوى الممنوع: يُمنع نشر محتوى مضلل أو مسيء أو مخالف للأنظمة المعمول بها.
7. الإبلاغ والحظر: يمكنك الإبلاغ عن أي إعلان وحظر أي مستخدم من داخل التطبيق.
8. إنهاء الحساب: يمكنك حذف حسابك في أي وقت من الملف الشخصي.
9. التعديل: قد نحدّث هذه الشروط وسيظهر تاريخ آخر تحديث في التطبيق.
""".trimIndent() else """
1. Acceptance: by using Bawabat Alorood you agree to these terms.
2. Service: the app connects buyers, brokers and real-estate developers so offers can be made on listed properties. The app is not a party to any sale contract.
3. Account: you are responsible for the accuracy of your details and for keeping your password secret.
4. Listings: advertisers must provide accurate property details and photos and be the owner or authorised to market it. We may reject or remove any non-compliant listing.
5. Offers and booking: the booking amount is transferred to the owner if the offer is accepted and fully refunded if rejected within 3 days, as shown in the app at payment time.
6. Prohibited content: misleading, abusive or unlawful content is not allowed.
7. Reporting and blocking: you can report any listing and block any user inside the app.
8. Closing your account: you can delete your account at any time from Profile.
9. Changes: we may update these terms and show the last-updated date in the app.
""".trimIndent()

    fun privacy(lang: String): String = if (lang == "ar") """
البيانات التي نعالجها:
• رقم الهاتف والاسم وكلمة المرور (تُخزَّن مشفّرة بصيغة Hash) لإنشاء حسابك وتسجيل دخولك.
• بيانات الإعلانات والصور التي تضيفها (ويظهر رقم هاتفك على إعلاناتك للتواصل)، وبيانات العروض والرسائل التي ترسلها.
• الموقع التقريبي فقط إذا سمحت بذلك، ويُستخدم لإظهار العقارات القريبة منك ولا يُباع ولا يُشارك.
• الصور والملفات التي تختارها بنفسك عبر منتقي الصور/الملفات في النظام، بدون صلاحيات تخزين أو كاميرا.

كيف نستخدمها: لتشغيل الخدمة وعرض النتائج وإتمام العروض والتواصل بين الأطراف، وتحسين الأمان ومنع الإساءة.

المشاركة: لا نبيع بياناتك. تُشارك بيانات العرض فقط مع الطرف الآخر في الصفقة (المالك/المعلن) عند تقديمك عرضًا أو بدء محادثة.

النسخة الحالية (تجريبية): جميع البيانات محفوظة على جهازك فقط ولا تُرسل إلى أي خادم.

حقوقك: يمكنك تعديل بياناتك أو حذف حسابك وكل بياناتك في أي وقت من الملف الشخصي ← حذف الحساب.

التواصل: عبر البريد المذكور في صفحة «تواصل معنا».
""".trimIndent() else """
Data we process:
• Phone number, name and password (stored only as a hash) to create your account and sign you in.
• Listing data and photos you add (your phone number is shown on your ads for contact), and the offers and messages you send.
• Approximate location only if you allow it, used to show nearby properties; it is never sold or shared.
• Photos/files you choose yourself through the system picker — no storage or camera permission is used.

How we use it: to run the service, show results, process offers, let parties communicate, and keep the platform safe.

Sharing: we do not sell your data. Offer details are shared only with the other party (owner/advertiser) when you make an offer or start a chat.

Current build (demo): all data is stored on your device only and is not sent to any server.

Your rights: edit your details or delete your account and all data at any time from Profile → Delete account.

Contact: via the email shown on the “Contact us” page.
""".trimIndent()

    fun about(lang: String): String = if (lang == "ar")
        "«بوابة العروض» منصة عقارية تجمع الملاك والمطورين والوسطاء والمشترين في مكان واحد: جميع العقارات من الملاك مباشرة، وقدّم عرضك على الدور الذي يناسبك وتفاوض بأمان."
    else
        "Bawabat Alorood is a real-estate platform that brings owners, developers, brokers and buyers together: all properties direct from owners — make your offer on the floor you want and negotiate safely."
}
