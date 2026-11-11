# إصدار نسخة Release موقّعة

## 1) إنشاء مفتاح الرفع (مرة واحدة)
```bash
keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```
احفظ `upload.jks` وكلمات المرور في مكان آمن (لا ترفعه على GitHub — `.gitignore` يمنعه).

## 2) تحويله إلى Base64
- Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload.jks")) | Set-Clipboard`
- Linux/macOS: `base64 -w0 upload.jks`

## 3) إضافة Secrets في GitHub
Settings ← Secrets and variables ← Actions ← New repository secret:

| الاسم | القيمة |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | ناتج الخطوة 2 |
| `ANDROID_KEYSTORE_PASSWORD` | كلمة مرور الـ keystore |
| `ANDROID_KEY_ALIAS` | `upload` |
| `ANDROID_KEY_PASSWORD` | كلمة مرور المفتاح |

## 4) بناء الإصدار
Actions ← Build Android APK ← Run workflow ← فعّل **build_release**. النتيجة: Artifact `bawabat-alorood-release` فيه `app-release.apk` و`app-release.aab` و`mapping.txt`.

## 5) الرفع على Google Play
- ارفع **الـ AAB** (مش الـ APK) في Play Console، وفعّل **Play App Signing**.
- ارفع `mapping.txt` مع الإصدار لقراءة الأخطاء.
- لو فشل بناء Release بسبب R8، افتح `app/build.gradle.kts` وغيّر `isMinifyEnabled` إلى `false` مؤقتًا وابعت لي الخطأ.

## ملاحظة
لا تبنِ Release قبل ربط الخادم ومراجعة `PLAY_POLICY_CHECKLIST.md`.
