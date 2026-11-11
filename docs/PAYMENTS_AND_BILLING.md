# الدفع و Google Play Billing — ما ينقص وما أحتاجه منك

## الوضع الحالي
- `admin/src/lib/payments.ts`: واجهة `Gateway` وبوابة `demo` فقط (لا تسحب أموالًا، وتُرفض في الإنتاج ما لم تضبط `ALLOW_DEMO_PAYMENTS=true`).
- التطبيق يطلب الدفع من الخادم (`POST /offers` و`POST /subscription`) ولا يعالج بيانات بطاقات.

## 1) مبلغ الحجز (معاملة عقارية حقيقية) — بوابة دفع مرخّصة
لا أستطيع تفعيلها نيابة عنك لأنها تحتاج **حسابًا تجاريًا ومفاتيح** عندك. القرار والمطلوب:
1. اختر مزودًا يدعم السعودية ومدى (مثل Moyasar أو HyperPay أو Tap) وافتح حساب Sandbox.
2. ابعت لي: اسم المزود + مفاتيح **الاختبار فقط** (لا تبعت مفاتيح الإنتاج في المحادثة) + هل تريد دفع بالبطاقة داخل صفحة المزود (Hosted/Invoice، الأسهل والأأمن) أم SDK.
3. أنفّذ: `MoyasarGateway` (إنشاء فاتورة ← فتح الرابط من التطبيق ← Webhook يؤكد الدفع ← الحجز يصير `PAID`)، مع التحقق من توقيع الـ Webhook ومنع التكرار.
4. التدفق يصبح غير متزامن (الدفع خارج التطبيق)، لذلك يتغير تدفق شاشة الحجز قليلًا.

## 2) اشتراك العميل (مزايا رقمية) — Google Play Billing إجباري
- المنتج لازم يُنشأ في **Play Console** (Monetize ← Subscriptions أو In-app products) بمعرّف مثل `client_membership`.
- المطلوب منك: حساب Play Console مفعّل + المنتج + **Service Account** (JSON) مرتبط بـ Play Console بصلاحية Financial data/Orders — يوضع كسر في Vercel (`GOOGLE_PLAY_SERVICE_ACCOUNT`).
- أنفّذ بعدها: مكتبة Billing في التطبيق (شراء) + `POST /api/v1/subscription/verify` يتحقق من `purchaseToken` عبر Google Play Developer API ويفعّل الاشتراك ويعترف بالشراء (acknowledge) خلال 3 أيام وإلا يُسترد تلقائيًا.
- ملاحظة: يلزم رفع نسخة (AAB) على مسار اختبار في Play Console لاختبار الشراء الفعلي، ولا يعمل على APK خارجي.
- حتى ذلك الحين: عطّل الاشتراك من الإنتاج أو اتركه مخفيًا (`SUBSCRIPTION_AMOUNT` = 0 لا يكفي — الأفضل إخفاء الشاشة).
