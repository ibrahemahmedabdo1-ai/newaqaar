import { db } from "./db";
import { ApiError } from "./http";

function ipOf(req: Request): string {
  return req.headers.get("x-forwarded-for")?.split(",")[0]?.trim() || req.headers.get("x-real-ip") || "unknown";
}

/**
 * تحديد معدل الطلبات (يعمل على Serverless لأنه مخزَّن في قاعدة البيانات).
 * name: اسم العملية، max: الحد الأقصى داخل النافذة، windowSec: طول النافذة بالثواني.
 * للحماية الأقوى فعّل أيضًا Vercel Firewall / Rate Limiting على مسارات /api/v1.
 */
export async function rateLimit(req: Request, name: string, max: number, windowSec: number, extraKey = ""): Promise<void> {
  const key = `${name}:${ipOf(req)}${extraKey ? ":" + extraKey : ""}`.slice(0, 190);
  const now = new Date();
  const row = await db.rateLimit.findUnique({ where: { key } });
  if (!row || row.resetAt < now) {
    const resetAt = new Date(now.getTime() + windowSec * 1000);
    await db.rateLimit.upsert({ where: { key }, update: { count: 1, resetAt }, create: { key, count: 1, resetAt } });
    return;
  }
  if (row.count >= max) throw new ApiError(429, "RATE_LIMITED", "طلبات كثيرة، حاول بعد قليل");
  await db.rateLimit.update({ where: { key }, data: { count: { increment: 1 } } });
}
