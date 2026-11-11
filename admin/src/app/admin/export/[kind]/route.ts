import { audit } from "@/lib/audit";
import { getSession } from "@/lib/auth";
import { toCsv } from "@/lib/csv";
import { db } from "@/lib/db";
import { can, type Perm } from "@/lib/perms";

const NEED: Record<string, Perm> = { users: "users.read", ads: "ads.read", offers: "offers.read", payments: "payments.read" };

export const dynamic = "force-dynamic";

export async function GET(_req: Request, ctx: { params: Promise<{ kind: string }> }) {
  const s = await getSession();
  if (!s) return new Response("Unauthorized", { status: 401 });
  const { kind } = await ctx.params;
  const need = NEED[kind];
  if (!need || !can(s.role, "export") || !can(s.role, need)) return new Response("Forbidden", { status: 403 });

  let rows: Record<string, unknown>[] = [];
  if (kind === "users") {
    const r = await db.user.findMany({ orderBy: { createdAt: "desc" }, take: 20000 });
    rows = r.map((u) => ({ id: u.id, name: u.name, phone: u.phone, role: u.role, status: u.status, subscribed: u.subscribed, createdAt: u.createdAt, lastLoginAt: u.lastLoginAt }));
  } else if (kind === "ads") {
    const r = await db.property.findMany({ orderBy: { createdAt: "desc" }, take: 20000, include: { owner: { select: { name: true, phone: true } } } });
    rows = r.map((p) => ({ id: p.id, owner: p.owner.name, ownerPhone: p.owner.phone, kind: p.kind, rooms: p.rooms, district: p.district, price: p.price, minOffer: p.minOffer, commission: p.commission, status: p.status, createdAt: p.createdAt, publishedAt: p.publishedAt }));
  } else if (kind === "offers") {
    const r = await db.offer.findMany({ orderBy: { createdAt: "desc" }, take: 20000, include: { byUser: { select: { name: true } }, property: { select: { district: true } } } });
    rows = r.map((o) => ({ id: o.id, propertyId: o.propertyId, district: o.property.district, by: o.byUser.name, client: o.clientName, clientPhone: o.clientPhone, floor: o.floor, amount: o.amount, status: o.status, createdAt: o.createdAt }));
  } else {
    const r = await db.payment.findMany({ orderBy: { createdAt: "desc" }, take: 20000 });
    rows = r.map((p) => ({ id: p.id, kind: p.kind, amount: p.amount, vat: p.vat, total: p.total, status: p.status, provider: p.provider, userPhone: p.userPhone, createdAt: p.createdAt, refundedAt: p.refundedAt }));
  }
  await audit(s, "export." + kind, "export", undefined, { rows: rows.length });
  return new Response(toCsv(rows), {
    headers: { "Content-Type": "text/csv; charset=utf-8", "Content-Disposition": `attachment; filename="${kind}-${new Date().toISOString().slice(0, 10)}.csv"` },
  });
}
