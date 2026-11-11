import { db } from "@/lib/db";
import { requireUser } from "@/lib/api-auth";
import { propertyDto, propInclude } from "@/lib/dto";
import { ApiError, body, json, route } from "@/lib/http";
import { j } from "@/lib/json";
import { propertySchema } from "@/lib/schemas";
import { getSettings } from "@/lib/settings";

type Ctx = { params: Promise<{ id: string }> };

async function owned(id: string, userId: string) {
  const p = await db.property.findUnique({ where: { id } });
  if (!p) throw new ApiError(404, "NOT_FOUND");
  if (p.ownerId !== userId) throw new ApiError(403, "FORBIDDEN");
  return p;
}

export const PUT = route(async (req: Request, ctx: Ctx) => {
  const { id } = await ctx.params;
  const u = await requireUser(req);
  await owned(id, u.id);
  const b = await body(req, propertySchema);
  const review = (await getSettings()).requireAdReview;
  const p = await db.property.update({
    where: { id },
    data: {
      kind: b.kind, rooms: b.rooms, baths: b.baths, area: b.area, price: b.price, minOffer: Math.min(b.minOffer, b.price),
      commission: b.commission, district: b.district, city: b.city, description: b.description, facade: b.facade,
      direction: b.direction, view: b.view, maid: b.maid, driver: b.driver, parking: b.parking,
      floors: j(b.floors), guarantees: j(b.guarantees), amenities: j(b.amenities), infra: j(b.infra), photos: j(b.photos),
      lat: b.lat, lng: b.lng,
      status: review ? "PENDING" : "PUBLISHED", rejectReason: null, endedAt: null,
    },
    include: propInclude,
  });
  return json(propertyDto(p, u.id));
});

export const DELETE = route(async (req: Request, ctx: Ctx) => {
  const { id } = await ctx.params;
  const u = await requireUser(req);
  await owned(id, u.id);
  await db.property.delete({ where: { id } });
  return json({ ok: true });
});
