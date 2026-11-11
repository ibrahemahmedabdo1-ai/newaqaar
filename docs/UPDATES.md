# التحديثات

## إصدار تحديث جديد (خطوات ثابتة)
1. عدّل `version.properties`: ارفع `VERSION_CODE` (+1 دائمًا) وغيّر `VERSION_NAME` (مثل 1.0.1).
2. أضف قسمًا جديدًا في `CHANGELOG.md`.
3. أضف `Release(...)` جديد في `Changelog.kt` (عشان تظهر شاشة «ما الجديد» بعد التحديث).
4. حدّث `distribution/whatsnew/whatsnew-ar` و`whatsnew-en-US` (500 حرف كحد أقصى لكل لغة في Play).
5. Push ← شغّل workflow Release ← ارفع الـ AAB في Play Console (يفضّل Staged rollout 10% ثم 50% ثم 100%).

## كيف يصل التحديث للمستخدم
- **المتجر:** التحديث التلقائي المعتاد من Google Play.
- **داخل التطبيق:** `UpdateChecker` يتحقق عند فتح التطبيق من Play In-App Updates، فإن وُجد إصدار أحدث يبدأ **تحديثًا مرنًا (Flexible)** يُنزَّل في الخلفية، ثم يظهر حوار «أعد التشغيل لإكمال التحديث». يعمل فقط للنسخة المثبّتة من Play، ويفشل بصمت غير ذلك.
- لإجبار تحديث حرج: غيّر `AppUpdateType.FLEXIBLE` إلى `IMMEDIATE` في `UpdateChecker.kt` (واستخدمه بحذر).
- **لا تضع آلية تنزيل APK ذاتية** من خارج المتجر: مخالفة لسياسة Google Play.

## ما الجديد داخل التطبيق
يظهر مرة واحدة بعد التحديث، ومصدره `Changelog.kt` (يطابق `VERSION_NAME`). في أول تثبيت لا يظهر.

## ترقية البيانات المحلية
`Repo` يقرأ JSON بـ `ignoreUnknownKeys` وقيم افتراضية، فإضافة حقول جديدة للموديلات آمنة. لتغيير جذري: أضف رقم Schema وخطوة ترحيل في `Repo.init`.
