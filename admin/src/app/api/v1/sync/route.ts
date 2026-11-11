import { db } from "@/lib/db";
import { authUser } from "@/lib/api-auth";
import { messageDto, ms, offerDto, propertyDto, propInclude, userDto } from "@/lib/dto";
import { fail, json, route } from "@/lib/http";
import { getSettings } from "@/lib/settings";
import { threadDto, threadInclude } from "@/lib/threads";

export const dynamic = "force-dynamic";

export const GET = route(async (req: Request) => {
  const user = await authUser(req);
  if (req.headers.get("authorization") && !user) return fail(401, "UNAUTHORIZED");
  if (user && user.status !== "ACTIVE") return fail(403, "ACCOUNT_" + user.status, "تم إيقاف حسابك، تواصل مع الدعم");
  const now = new Date();

  const [settings, published, mine, announcements, faqs, pages, news, types, districts] = await Promise.all([
    getSettings(),
    db.property.findMany({ where: { status: "PUBLISHED" }, include: propInclude, orderBy: [{ featured: "desc" }, { createdAt: "desc" }], take: 500 }),
    user ? db.property.findMany({ where: { ownerId: user.id, status: { not: "PUBLISHED" } }, include: propInclude, orderBy: { createdAt: "desc" } }) : Promise.resolve([]),
    db.announcement.findMany({
      where: { active: true, startsAt: { lte: now }, OR: [{ endsAt: null }, { endsAt: { gt: now } }] },
      orderBy: { createdAt: "desc" }, take: 5,
    }),
    db.faq.findMany({ where: { published: true }, orderBy: { sort: "asc" } }),
    db.page.findMany(),
    db.news.findMany({ where: { published: true }, orderBy: { publishedAt: "desc" }, take: 30 }),
    db.propertyType.findMany({ where: { active: true } }),
    db.district.findMany({ where: { active: true }, orderBy: { name: "asc" } }),
  ]);

  const hidden: Record<string, number> = {};
  for (const p of published) if (p.hiddenUntil && p.hiddenUntil > now) hidden[p.id] = ms(p.hiddenUntil);

  let favorites: string[] = [];
  let offers: ReturnType<typeof offerDto>[] = [];
  let threads: ReturnType<typeof threadDto>[] = [];
  let messages: ReturnType<typeof messageDto>[] = [];
  let tickets: { id: string; subject: string; status: string; reply: string; time: number }[] = [];
  if (user) {
    const [fav, off, thr] = await Promise.all([
      db.favorite.findMany({ where: { userId: user.id }, select: { propertyId: true } }),
      db.offer.findMany({
        where: { OR: [{ byUserId: user.id }, { property: { ownerId: user.id } }] },
        include: { byUser: { select: { phone: true } } }, orderBy: { createdAt: "desc" }, take: 300,
      }),
      db.thread.findMany({
        where: { OR: [{ aId: user.id }, { bId: user.id }], NOT: { deletedBy: { has: user.id } } },
        include: threadInclude, orderBy: { createdAt: "desc" }, take: 100,
      }),
    ]);
    favorites = fav.map((f) => f.propertyId);
    offers = off.map(offerDto);
    threads = thr.map((t) => threadDto(t, user.id, user.phone));
    messages = thr.flatMap((t) => [...t.messages].reverse().map((m) => messageDto(m, user.id)));
    const tk = await db.supportTicket.findMany({ where: { userId: user.id }, orderBy: { createdAt: "desc" }, take: 20 });
    tickets = tk.map((t) => ({ id: t.id, subject: t.subject, status: t.status, reply: t.reply ?? "", time: ms(t.createdAt) }));
  }

  return json({
    serverTime: Date.now(),
    config: settings,
    announcements: announcements.map((a) => ({ id: a.id, titleAr: a.titleAr, titleEn: a.titleEn, bodyAr: a.bodyAr, bodyEn: a.bodyEn, level: a.level })),
    me: user ? userDto(user) : null,
    properties: [...published, ...mine].map((p) => propertyDto(p, user?.id)),
    hidden, favorites, offers, threads, messages, tickets,
    faqs: faqs.map((f) => ({ qAr: f.qAr, aAr: f.aAr, qEn: f.qEn, aEn: f.aEn })),
    pages: Object.fromEntries(pages.map((p) => [p.key, { titleAr: p.titleAr, titleEn: p.titleEn, bodyAr: p.bodyAr, bodyEn: p.bodyEn }])),
    news: news.map((n) => ({ id: n.id, titleAr: n.titleAr, titleEn: n.titleEn, bodyAr: n.bodyAr, bodyEn: n.bodyEn, imageUrl: n.imageUrl ?? "", time: ms(n.publishedAt) })),
    types, districts,
  });
});
