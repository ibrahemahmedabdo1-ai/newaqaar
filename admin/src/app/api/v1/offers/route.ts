import { z } from "zod";
import { db } from "@/lib/db";
import { requireUser } from "@/lib/api-auth";
import { offerDto } from "@/lib/dto";
import { rateLimit } from "@/lib/ratelimit";
import { ApiError, body, json, route } from "@/lib/http";
import { floorsOf } from "@/lib/json";
import { gateway } from "@/lib/payments";
import { getSettings } from "@/lib/settings";

const schema = z.object({
  id: z.string().min(6).max(40).optional(),
  propertyId: z.string().min(1),
  floor: z.string().min(1).max(40),
  amount: z.number().int().positive(),
  clientName: z.string().trim().max(60).optional(),
  clientPhone: z.string().max(20).optional(),
});

export const POST = route(async (req: Request) => {
  await rateLimit(req, "offer", 30, 3600);
  const u = await requireUser(req);
  const b = await body(req, schema);

  if (b.id) {
    const ex = await db.offer.findUnique({ where: { id: b.id }, include: { byUser: { select: { phone: true } } } });
    if (ex) {
      if (ex.byUserId !== u.id) throw new ApiError(409, "ID_TAKEN");
      return json(offerDto(ex));
    }
  }

  const p = await db.property.findUnique({ where: { id: b.propertyId } });
  if (!p || p.status !== "PUBLISHED") throw new ApiError(404, "NOT_AVAILABLE", "الإعلان غير متاح");
  if (p.ownerId === u.id) throw new ApiError(400, "OWN_PROPERTY");
  if (!floorsOf(p.floors).some((f) => f.name === b.floor && f.state === "AVAILABLE")) throw new ApiError(409, "FLOOR_UNAVAILABLE", "هذا الدور غير متاح");
  if (b.amount < p.minOffer) throw new ApiError(400, "OFFER_TOO_LOW", `لا يمكن تقديم عرض أقل من ${p.minOffer}`);

  const isClient = u.role === "CLIENT";
  const clientName = isClient ? u.name : b.clientName ?? "";
  const clientPhone = isClient ? u.phone : b.clientPhone ?? "";
  if (!clientName || !clientPhone) throw new ApiError(400, "CLIENT_INFO_REQUIRED", "بيانات العميل مطلوبة");

  const s = await getSettings();
  const total = s.bookingAmount + s.bookingVat;
  const now = new Date();
  const holdUntil = new Date(now.getTime() + s.holdDays * 86_400_000);

  // حجز الإعلان أولًا (يمنع عرضين متزامنين)
  const claimed = await db.property.updateMany({
    where: { id: p.id, OR: [{ hiddenUntil: null }, { hiddenUntil: { lt: now } }] },
    data: { hiddenUntil: holdUntil },
  });
  if (claimed.count === 0) throw new ApiError(409, "ON_HOLD", "هذا الإعلان محجوز حاليًا");

  const res = await gateway().charge({ amountSar: total, kind: "BOOKING", userId: u.id, reference: p.id });
  if (!res.ok) {
    await db.property.update({ where: { id: p.id }, data: { hiddenUntil: null } });
    throw new ApiError(res.error === "PAYMENTS_NOT_CONFIGURED" ? 503 : 402, res.error, "تعذر إتمام الدفع");
  }

  const offer = await db.offer.create({
    data: {
      id: b.id, propertyId: p.id, byUserId: u.id, clientName, clientPhone, floor: b.floor, amount: b.amount,
      payment: {
        create: {
          userId: u.id, userPhone: u.phone, kind: "BOOKING", amount: s.bookingAmount, vat: s.bookingVat, total,
          status: "PAID", provider: res.provider, providerRef: res.ref,
        },
      },
    },
    include: { byUser: { select: { phone: true } } },
  });
  return json(offerDto(offer), 201);
});
