import { z } from "zod";
import { db } from "@/lib/db";
import { requireUser } from "@/lib/api-auth";
import { offerDto } from "@/lib/dto";
import { ApiError, body, json, route } from "@/lib/http";
import { floorsOf, j } from "@/lib/json";
import { gateway } from "@/lib/payments";

/** صاحب العقار يقبل أو يرفض العرض. الرفض يسترد مبلغ الحجز ويعيد إظهار الإعلان. */
export const PATCH = route(async (req: Request, ctx: { params: Promise<{ id: string }> }) => {
  const { id } = await ctx.params;
  const u = await requireUser(req);
  const b = await body(req, z.object({ status: z.enum(["ACTIVE", "REJECTED"]) }));
  const o = await db.offer.findUnique({ where: { id }, include: { property: true, payment: true, byUser: { select: { phone: true } } } });
  if (!o) throw new ApiError(404, "NOT_FOUND");
  if (o.property.ownerId !== u.id) throw new ApiError(403, "FORBIDDEN");
  if (o.status !== "PENDING") throw new ApiError(409, "ALREADY_DECIDED");

  if (b.status === "ACTIVE") {
    const floors = floorsOf(o.property.floors).map((f) => (f.name === o.floor ? { ...f, state: "RESERVED" as const } : f));
    await db.property.update({ where: { id: o.propertyId }, data: { floors: j(floors) } });
  } else {
    if (o.payment?.status === "PAID") {
      await gateway().refund(o.payment.providerRef ?? "");
      await db.payment.update({ where: { id: o.payment.id }, data: { status: "REFUNDED", refundedAt: new Date() } });
    }
    await db.property.update({ where: { id: o.propertyId }, data: { hiddenUntil: null } });
  }
  const updated = await db.offer.update({
    where: { id }, data: { status: b.status, decidedAt: new Date() }, include: { byUser: { select: { phone: true } } },
  });
  return json(offerDto(updated));
});
