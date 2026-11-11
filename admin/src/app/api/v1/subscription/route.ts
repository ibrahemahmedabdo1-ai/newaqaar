import { db } from "@/lib/db";
import { requireUser } from "@/lib/api-auth";
import { ApiError, json, route } from "@/lib/http";
import { gateway } from "@/lib/payments";
import { getSettings } from "@/lib/settings";
import { userDto } from "@/lib/dto";

/**
 * تنبيه Google Play: إن كان الاشتراك يفتح مزايا رقمية داخل التطبيق فيجب استخدام Google Play Billing
 * (والتحقق من الإيصال من الخادم) بدل هذه البوابة.
 */
export const POST = route(async (req: Request) => {
  const u = await requireUser(req);
  if (u.subscribed) return json(userDto(u));
  const s = await getSettings();
  const total = s.subscriptionAmount + s.subscriptionVat;
  const res = await gateway().charge({ amountSar: total, kind: "SUBSCRIPTION", userId: u.id, reference: "subscription" });
  if (!res.ok) throw new ApiError(res.error === "PAYMENTS_NOT_CONFIGURED" ? 503 : 402, res.error, "تعذر إتمام الدفع");
  const [, user] = await db.$transaction([
    db.payment.create({
      data: { userId: u.id, userPhone: u.phone, kind: "SUBSCRIPTION", amount: s.subscriptionAmount, vat: s.subscriptionVat, total, status: "PAID", provider: res.provider, providerRef: res.ref },
    }),
    db.user.update({ where: { id: u.id }, data: { subscribed: true } }),
  ]);
  return json(userDto(user));
});
