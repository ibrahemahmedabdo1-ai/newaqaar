import { createHash, randomInt } from "crypto";
import { db } from "./db";
import { must, secret } from "./env";
import { ApiError } from "./http";
import { signOtpToken } from "./api-auth";

const hashCode = (code: string, phone: string) =>
  createHash("sha256").update(`${code}:${phone}:${Buffer.from(secret("API_JWT_SECRET")).toString("hex")}`).digest("hex");

async function sendSms(phone: string, text: string): Promise<void> {
  const provider = process.env.OTP_PROVIDER ?? "console";
  if (provider === "twilio") {
    const sid = must("TWILIO_ACCOUNT_SID");
    const token = must("TWILIO_AUTH_TOKEN");
    const from = must("TWILIO_FROM");
    const res = await fetch(`https://api.twilio.com/2010-04-01/Accounts/${sid}/Messages.json`, {
      method: "POST",
      headers: {
        Authorization: "Basic " + Buffer.from(`${sid}:${token}`).toString("base64"),
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: new URLSearchParams({ To: phone, From: from, Body: text }),
    });
    if (!res.ok) throw new ApiError(502, "SMS_FAILED", "تعذر إرسال الرسالة");
    return;
  }
  console.log(`[OTP:console] ${phone}: ${text}`);
}

/** يطلب رمز تأكيد. يعيد debugCode فقط إذا OTP_DEBUG=true (للتجارب). */
export async function requestOtp(phone: string, purpose: string): Promise<{ debugCode?: string }> {
  const now = Date.now();
  const recent = await db.otpCode.findMany({
    where: { phone, createdAt: { gt: new Date(now - 3600_000) } },
    orderBy: { createdAt: "desc" },
    take: 10,
  });
  if (recent.length >= 5) throw new ApiError(429, "OTP_RATE_LIMIT", "تجاوزت عدد المحاولات، حاول بعد ساعة");
  if (recent[0] && now - recent[0].createdAt.getTime() < 30_000) throw new ApiError(429, "OTP_TOO_SOON", "انتظر 30 ثانية قبل إعادة الإرسال");

  const code = String(randomInt(0, 10000)).padStart(4, "0");
  await db.otpCode.create({
    data: { phone, purpose, codeHash: hashCode(code, phone), expiresAt: new Date(now + 5 * 60_000) },
  });
  await sendSms(phone, `بوابة العروض: رمز التأكيد ${code}. لا تشاركه مع أحد.`);
  // لا يُرجَع الرمز في الإنتاج إلا بتفعيل صريح مزدوج (OTP_DEBUG + ALLOW_OTP_DEBUG) — للتجارب فقط.
  const debugOn = process.env.OTP_DEBUG === "true" && (process.env.NODE_ENV !== "production" || process.env.ALLOW_OTP_DEBUG === "true");
  return debugOn ? { debugCode: code } : {};
}

export async function verifyOtp(phone: string, purpose: string, code: string): Promise<string> {
  const row = await db.otpCode.findFirst({
    where: { phone, purpose, usedAt: null, expiresAt: { gt: new Date() } },
    orderBy: { createdAt: "desc" },
  });
  if (!row || row.attempts >= 5) throw new ApiError(400, "OTP_INVALID", "الرمز خاطئ أو منتهي");
  if (row.codeHash !== hashCode(code, phone)) {
    await db.otpCode.update({ where: { id: row.id }, data: { attempts: { increment: 1 } } });
    throw new ApiError(400, "OTP_INVALID", "الرمز خاطئ يرجى المحاولة مرة أخرى");
  }
  await db.otpCode.update({ where: { id: row.id }, data: { usedAt: new Date() } });
  return signOtpToken(phone, purpose);
}
