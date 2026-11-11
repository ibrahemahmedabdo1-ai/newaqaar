# الإشعارات الفورية (Push) — الخطة وما أحتاجه منك

## الوضع الحالي
المحادثات والمزامنة بالـ Polling (60 ثانية، و6 ثوانٍ داخل المحادثة). مفيش Push.

## المطلوب منك (لا أستطيع إنشاؤه نيابة عنك)
1. مشروع **Firebase** (console.firebase.google.com) وإضافة تطبيق Android بالحزمة `com.bawabat.alorood` (ومعها `com.bawabat.alorood.debug` للتجربة).
2. ملف `google-services.json` ← ضعه في `app/google-services.json` (لا يُعتبر سرًّا لكن لا تشاركه علنًا إن أمكن).
3. **Service Account** لـ FCM HTTP v1 (Firebase ← Project settings ← Service accounts ← Generate key) ← يوضع كسر في Vercel باسم `FCM_SERVICE_ACCOUNT`.

## ما سأنفّذه بعد توفرها
- التطبيق: `FirebaseMessagingService`، طلب إذن الإشعارات (Android 13+) عند أول سياق مناسب، تسجيل `fcmToken` مع الخادم، وفتح المحادثة/الطلب من الإشعار.
- الخادم: جدول `DeviceToken`، `POST /api/v1/devices`، مُرسِل FCM v1 (JWT بالـ Service Account)، وإرسال إشعار عند: رسالة جديدة، قبول/رفض عرض، موافقة/رفض إعلان، رد دعم.
- لوحة التحكم: إرسال إشعار جماعي حسب الدور، وسجل إرسال.
- شرط البناء: تطبيق `google-services` plugin يكون مشروطًا بوجود الملف حتى لا ينكسر البناء بدونه.
