import { db } from "@/lib/db";
import { requireUser } from "@/lib/api-auth";
import { propertyDto, propInclude } from "@/lib/dto";
import { ApiError, body, json, route } from "@/lib/http";
import { j } from "@/lib/json";
import { propertySchema } from "@/lib/schemas";
import { getSettings } from "@/lib/settings";

export const POST = route(async (req: Request) => {
  const u = await requireUser(req);
  if (u.role !== "DEVELOPER") throw new ApiError(403, "FORBIDDEN", "إضافة الإعلانات متاحة للمطور العقاري فقط");
  const b = await body(req, propertySchema);
  if (b.id) {
    const ex = await db.property.findUnique({ where: { id: b.id }, include: propInclude });
    if (ex) {
      if (ex.ownerId !== u.id) throw new ApiError(409, "ID_TAKEN");
      return json(propertyDto(ex, u.id));
    }
  }
  const s = await getSettings();
  const review = s.requireAdReview;
  const p = await db.property.create({
    data: {
      id: b.id, ownerId: u.id, kind: b.kind, rooms: b.rooms, baths: b.baths, area: b.area, price: b.price,
      minOffer: Math.min(b.minOffer, b.price), commission: b.commission, district: b.district, city: b.city,
      description: b.description, facade: b.facade, direction: b.direction, view: b.view,
      maid: b.maid, driver: b.driver, parking: b.parking,
      floors: j(b.floors), guarantees: j(b.guarantees), amenities: j(b.amenities), infra: j(b.infra), photos: j(b.photos),
      lat: b.lat, lng: b.lng,
      status: review ? "PENDING" : "PUBLISHED", publishedAt: review ? null : new Date(),
    },
    include: propInclude,
  });
  return json(propertyDto(p, u.id), 201);
});
