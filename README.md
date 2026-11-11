# بوابة العروض — Bawabat Alorood

تطبيق أندرويد عقاري (Kotlin + Jetpack Compose) منفّذ من تصميم Figma `Real-state`، بثلاثة مسارات في تطبيق واحد: **مشتري / سمسار / مطور عقاري**، عربي RTL وEnglish، سمة فاتحة وداكنة.

> **مهم وبصراحة:** هذه النسخة **تجريبية (DEMO_MODE)**: لا يوجد خادم، فكل البيانات محلية على الجهاز، ورمز التأكيد تجريبي (`1234`)، والدفع محاكاة ولا يسحب أي مبلغ. الكود كله اتكتب ومتراجع ثابت، لكن **لم يتم بناؤه أو تشغيله على جهاز** في بيئة الكتابة (مفيش Android SDK ولا إنترنت). أول بناء هيتم على GitHub Actions — لو ظهر خطأ في الـ Log ابعتهولي وأصلحه. التفاصيل في `docs/TEST_REPORT.md`.

## الخطوات (من غير ما تنزّل Android Studio)

1. فك الـ ZIP.
2. أنشئ Repository جديد على GitHub (فارغ).
3. ارفع **محتويات** المجلد (مش المجلد نفسه) — لازم يظهر `.github/workflows` في الـ Repo.
   - بالمتصفح: Add file ← Upload files ← اسحب كل الملفات والمجلدات. تأكد إن المجلد المخفي `.github` اترفع (لو مخفي عندك في ويندوز: View ← Hidden items).
   - أو بـ Git:
     ```bash
     git init && git add . && git commit -m "Initial commit"
     git branch -M main
     git remote add origin https://github.com/<user>/<repo>.git
     git push -u origin main
     ```
4. افتح تبويب **Actions** ← **Build Android APK** ← **Run workflow** (أو هيشتغل تلقائيًا مع أول Push).
5. بعد ما يخلص (حوالي 8–15 دقيقة) افتح الـ Run ونزّل الـ Artifact **bawabat-alorood-debug-apk** ← جواه `app-debug.apk` و`BUILD-REPORT.txt`.
6. انقل الـ APK للموبايل وثبّته (فعّل «التثبيت من مصادر غير معروفة» لمرة واحدة).

## نسخة Release لـ Google Play (AAB + APK موقّعين)

اتبع `docs/RELEASE.md` (إنشاء مفتاح التوقيع وإضافة 4 Secrets، ثم Run workflow مع تفعيل `build_release`).

## لوحة التحكم والخادم (جديد)

المجلد `admin/` فيه لوحة تحكم كاملة + API (Next.js + Prisma + Neon + Vercel) والتطبيق يتصل بها عند ضبط المتغير `API_BASE_URL`. الخطوات في **`admin/README.md`**، وتوثيق الـ API في `docs/API.md`.
ملخص: (1) أنشئ قاعدة Neon ← (2) أضف Secrets ← (3) شغّل **Database setup (Neon)** ← (4) انشر `admin/` على Vercel (Root Directory = `admin`) ← (5) ضع `API_BASE_URL` كـ Variable في GitHub ← (6) أعد بناء الـ APK.
> الكود لم يُشغَّل بعد (مفيش بيئة تشغيل عندي)؛ أول بناء على GitHub/Vercel هو الاختبار الفعلي.

## الشعار الرسمي
ضع `branding/logo.png` (تصدير PNG من Figma) وسيتحول تلقائيًا لأيقونة التطبيق وشعار الواجهة عند البناء — التفاصيل في `branding/README.md`.

## ما ينقص قبل الإطلاق (يحتاج حساباتك ومفاتيحك)
- **الدفع وPlay Billing:** `docs/PAYMENTS_AND_BILLING.md`
- **الإشعارات الفورية (Firebase):** `docs/PUSH_NOTIFICATIONS.md`
- **رسائل OTP (Twilio أو غيره)** والتأكد من أن بطاقة «جاهزية الإنتاج» في اللوحة فارغة.
- **واجهة لوحة التحكم بالإنجليزية:** غير منفّذة (عربي فقط حاليًا).

## ملفات التحديثات

| الملف | الغرض |
|---|---|
| `version.properties` | رقم الإصدار (`VERSION_CODE` لازم يزيد مع كل رفع) |
| `CHANGELOG.md` | سجل التحديثات الكامل |
| `app/.../Changelog.kt` | نص «ما الجديد» اللي يظهر داخل التطبيق بعد التحديث |
| `distribution/whatsnew/` | نص «What's new» لصفحة المتجر (عربي/إنجليزي) |
| `docs/UPDATES.md` | طريقة إصدار التحديثات والتحديث داخل التطبيق (Google Play In-App Updates) |

## التوافق مع سياسات Google Play

اقرأ `docs/PLAY_POLICY_CHECKLIST.md` — فيه اللي اتنفّذ في الكود، واللي **لازم تعمله قبل النشر** (أهمه: ربط خادم حقيقي، بوابة دفع/Play Billing، سياسة خصوصية على رابط عام، نموذج Data safety). ومعاه `docs/DATA_SAFETY.md` لتعبئة النموذج.

## القيم اللي تعدّلها قبل النشر

في `gradle.properties`: `SUPPORT_EMAIL`, `SUPPORT_WHATSAPP`, `PRIVACY_URL`, `TERMS_URL`, `ACCOUNT_DELETION_URL`.
في `site/`: استبدل `[ضع هنا ...]` بالبيانات الحقيقية ثم شغّل workflow **Deploy legal pages** لنشر صفحات الخصوصية والشروط وحذف الحساب على GitHub Pages (فعّل Pages: Settings ← Pages ← Source: GitHub Actions).
لتحويل الوضع التجريبي: `buildConfigField("boolean", "DEMO_MODE", "false")` في `app/build.gradle.kts` بعد ربط الخادم.

## هيكل المشروع

```
app/src/main/java/com/bawabat/alorood/
  data/   Models, Repo (تخزين محلي), Demo (بيانات تجريبية), Payments (واجهة الدفع), ImageStore
  ui/     Theme, Components, Auth/Home/Property/Chat/Profile/Dev screens, Nav
  util/   Validation, Intents, LocationHelper, UpdateChecker
docs/     ARCHITECTURE, PLAY_POLICY_CHECKLIST, DATA_SAFETY, RELEASE, UPDATES, TEST_REPORT
site/     صفحات الخصوصية والشروط وحذف الحساب (GitHub Pages)
.github/workflows/  build-apks.yml, pages.yml
```

الحزمة: `com.bawabat.alorood` — النسخة التجريبية (Debug) بلاحقة `.debug` عشان تتثبت جنب نسخة المتجر.
