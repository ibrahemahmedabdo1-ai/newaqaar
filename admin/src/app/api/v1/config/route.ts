import { db } from "@/lib/db";
import { getSettings } from "@/lib/settings";
import { json, route } from "@/lib/http";

export const dynamic = "force-dynamic";

export const GET = route(async () => {
  const [s, announcements, types, districts] = await Promise.all([
    getSettings(),
    db.announcement.findMany({
      where: { active: true, startsAt: { lte: new Date() }, OR: [{ endsAt: null }, { endsAt: { gt: new Date() } }] },
      orderBy: { createdAt: "desc" }, take: 5,
    }),
    db.propertyType.findMany({ where: { active: true } }),
    db.district.findMany({ where: { active: true }, orderBy: { name: "asc" } }),
  ]);
  return json({
    config: s,
    announcements: announcements.map((a) => ({ id: a.id, titleAr: a.titleAr, titleEn: a.titleEn, bodyAr: a.bodyAr, bodyEn: a.bodyEn, level: a.level, audience: a.audience })),
    types, districts,
  });
});
