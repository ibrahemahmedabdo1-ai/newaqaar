# واجهة API للتطبيق (`/api/v1`)

- كل الردود JSON. الأخطاء: `{ "error": "CODE", "message": "رسالة عربية" }`.
- المصادقة: `Authorization: Bearer <token>` (JWT صلاحيته 30 يومًا، ويُبطَل عند تغيير كلمة المرور/إيقاف الحساب).
- الهاتف بصيغة دولية `+9665...`.

## عام
| الطريقة | المسار | الوصف |
|---|---|---|
| GET | `/api/health` | فحص الخادم وقاعدة البيانات |
| GET | `/api/v1/config` | الإعدادات والمناطق والأنواع والإعلانات الداخلية |
| GET | `/api/v1/sync` | **المزامنة الكاملة**: الإعدادات، المستخدم، الإعلانات، المفضلة، العروض، المحادثات، الرسائل، الدعم، FAQ، الصفحات، الأخبار (التوكن اختياري للزائر) |

## الحساب
| الطريقة | المسار | الجسم |
|---|---|---|
| POST | `/auth/otp/request` | `{phone, purpose: register\|reset\|change_phone}` |
| POST | `/auth/otp/verify` | `{phone, purpose, code}` ← `{otpToken}` |
| POST | `/auth/register` | `{phone, name, role, password, otpToken}` ← `{token, user}` |
| POST | `/auth/login` | `{phone, password}` ← `{token, user}` |
| POST | `/auth/reset` | `{phone, password, otpToken}` |
| GET/PATCH/DELETE | `/me` | عرض / تعديل `{name, avatarUrl}` / حذف الحساب `{password}` |
| POST | `/me/password` | `{current, next}` |
| POST | `/me/phone` | `{newPhone, otpToken}` |

## الإعلانات والعروض
| الطريقة | المسار | ملاحظات |
|---|---|---|
| POST | `/properties` | مطور فقط؛ الحالة `PENDING` إن كانت المراجعة مفعلة |
| PUT/DELETE | `/properties/{id}` | المالك فقط؛ التعديل يعيد الإعلان للمراجعة |
| POST | `/properties/{id}/end` | `{soldTo?}` |
| POST | `/properties/{id}/report` | `{reason, details?}` |
| PUT/DELETE | `/favorites/{id}` | |
| POST | `/offers` | `{id?, propertyId, floor, amount, clientName?, clientPhone?}` — يحجز الإعلان ويدفع الحجز |
| PATCH | `/offers/{id}` | صاحب الإعلان: `{status: ACTIVE\|REJECTED}` (الرفض يسترد المبلغ) |
| POST | `/subscription` | اشتراك العميل |

## المحادثات والرفع والدعم
| الطريقة | المسار | |
|---|---|---|
| POST | `/threads` | `{id?, propertyId?, peerName?}` |
| PATCH | `/threads/{id}` | `{blocked?, deleted?, read?}` |
| POST | `/threads/{id}/messages` | `{id?, text, fileUrl}` |
| PATCH/DELETE | `/messages/{id}` | تعديل/حذف رسالتي |
| POST | `/uploads?folder=ads\|avatars\|chat` | الجسم بايتات صورة، `Content-Type: image/jpeg\|png\|webp` ≤ 6MB ← `{url}` |
| POST | `/support` | `{subject, body}` |

## ملاحظات
- المزامنة (Polling) كل 60 ثانية، وكل 6 ثوانٍ داخل شاشة محادثة. للإنتاج الكبير استبدلها بـ WebSocket/Push (FCM).
- معرّفات العناصر تُولَّد من التطبيق (idempotent)؛ إعادة الإرسال لا تكرر العنصر.
