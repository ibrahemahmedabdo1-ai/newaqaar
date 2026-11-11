export type Check = { level: "ok" | "warn" | "bad"; text: string };

/** فحص جاهزية الإنتاج: ما الذي ما زال في وضع تجريبي. */
export function readiness(opts: { supportEmail: string; supportWhatsapp: string; requireAdReview: boolean }): Check[] {
  const prod = process.env.NODE_ENV === "production";
  const c: Check[] = [];
  const add = (level: Check["level"], text: string) => c.push({ level, text });

  const otp = process.env.OTP_PROVIDER ?? "console";
  if (otp === "console") add(prod ? "bad" : "warn", "رمز التأكيد (OTP) يُطبع في السجلات فقط — لن يصل المستخدمين. اضبط OTP_PROVIDER=twilio (أو مزود SMS آخر).");
  else add("ok", `مزود الرسائل: ${otp}`);

  if (process.env.OTP_DEBUG === "true") add(prod ? "bad" : "warn", "OTP_DEBUG مفعّل — يرجّع رمز التأكيد للتطبيق (تجارب فقط). عطّله قبل الإطلاق.");
  if (process.env.ALLOW_OTP_DEBUG === "true") add("bad", "ALLOW_OTP_DEBUG=true — احذفه قبل الإطلاق.");

  const pay = process.env.PAYMENT_PROVIDER ?? "demo";
  if (pay === "demo") add(prod ? "bad" : "warn", "بوابة الدفع تجريبية (لا تسحب أموالًا). اربط بوابة حقيقية في src/lib/payments.ts.");
  else add("ok", `بوابة الدفع: ${pay}`);
  if (process.env.ALLOW_DEMO_PAYMENTS === "true") add("bad", "ALLOW_DEMO_PAYMENTS=true — المدفوعات الوهمية مسموحة. عطّلها قبل الإطلاق.");

  if (!process.env.BLOB_READ_WRITE_TOKEN) add("bad", "رفع الصور غير مفعّل (BLOB_READ_WRITE_TOKEN).");
  else add("ok", "تخزين الصور مفعّل");
  if (!process.env.CRON_SECRET) add("warn", "CRON_SECRET غير مضبوط — مهمة رفض العروض المنتهية واسترداد الحجوزات لن تعمل.");
  if (!opts.supportEmail && !opts.supportWhatsapp) add("warn", "بيانات الدعم (بريد/واتس آب) غير مضبوطة في الإعدادات.");
  if (!opts.requireAdReview) add("warn", "مراجعة الإعلانات قبل النشر معطّلة (موصى بتفعيلها لسياسة المحتوى).");
  if ((process.env.ADMIN_JWT_SECRET ?? "").length < 32 || (process.env.API_JWT_SECRET ?? "").length < 32) add("bad", "أسرار JWT قصيرة (أقل من 32 حرفًا).");
  return c;
}
