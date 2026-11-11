import { db } from "@/lib/db";
import { gateway } from "@/lib/payments";
import { getSettings } from "@/lib/settings";

export const dynamic = "force-dynamic";

/** Vercel Cron: يرفض العروض المعلقة بعد انتهاء مهلة holdDays ويسترد مبلغ الحجز ويعيد إظهار الإعلان. */
export async function GET(req: Request) {
  const secret = process.env.CRON_SECRET;
  if (!secret || req.headers.get("authorization") !== `Bearer ${secret}`) return new Response("Unauthorized", { status: 401 });
  const s = await getSettings();
  const cutoff = new Date(Date.now() - s.holdDays * 86_400_000);
  const stale = await db.offer.findMany({ where: { status: "PENDING", createdAt: { lt: cutoff } }, include: { payment: true }, take: 200 });
  let n = 0;
  for (const o of stale) {
    if (o.payment?.status === "PAID") {
      await gateway().refund(o.payment.providerRef ?? "");
      await db.payment.update({ where: { id: o.payment.id }, data: { status: "REFUNDED", refundedAt: new Date(), note: "auto-refund: offer expired" } });
    }
    await db.property.update({ where: { id: o.propertyId }, data: { hiddenUntil: null } });
    await db.offer.update({ where: { id: o.id }, data: { status: "REJECTED", decidedAt: new Date(), note: "expired" } });
    n++;
  }
  await db.rateLimit.deleteMany({ where: { resetAt: { lt: new Date() } } });
  await db.otpCode.deleteMany({ where: { createdAt: { lt: new Date(Date.now() - 7 * 86_400_000) } } });
  return Response.json({ expired: n });
}
