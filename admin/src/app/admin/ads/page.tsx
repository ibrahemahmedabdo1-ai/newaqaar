import Link from "next/link";
import type { AdStatus, Prisma } from "@prisma/client";
import { Badge, PageHeader, Pager, StatusBadge, Table, type SP } from "@/components/ui";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { fmtDateTime, fmtMoney, one, pageParams } from "@/lib/format";
import { AD_STATUS, KIND_AR } from "@/lib/labels";
import { can } from "@/lib/perms";

export default async function AdsPage({ searchParams }: { searchParams: SP }) {
  const s = await requireAdmin("ads.read");
  const sp = await searchParams;
  const q = one(sp.q).trim();
  const status = one(sp.status);
  const kind = one(sp.kind);
  const { page, take, skip } = pageParams(sp);
  const where: Prisma.PropertyWhereInput = {
    ...(q ? { OR: [{ district: { contains: q, mode: "insensitive" } }, { description: { contains: q, mode: "insensitive" } }, { owner: { name: { contains: q, mode: "insensitive" } } }] } : {}),
    ...(status in AD_STATUS ? { status: status as AdStatus } : {}),
    ...(kind in KIND_AR ? { kind } : {}),
  };
  const [total, rows, counts] = await Promise.all([
    db.property.count({ where }),
    db.property.findMany({ where, orderBy: { createdAt: "desc" }, take, skip, include: { owner: { select: { id: true, name: true } } } }),
    db.property.groupBy({ by: ["status"], _count: true }),
  ]);
  return (
    <>
      <PageHeader title="الإعلانات" sub="مراجعة الإعلانات والموافقة عليها">
        {can(s.role, "export") ? <a className="btn alt sm" href="/admin/export/ads">تصدير CSV</a> : null}
      </PageHeader>
      <div className="row" style={{ marginBottom: 12 }}>
        <Link className="btn alt sm" href="/admin/ads">الكل</Link>
        {Object.entries(AD_STATUS).map(([k, v]) => (
          <Link key={k} className="btn alt sm" href={`/admin/ads?status=${k}`}>{v[0]} ({counts.find((c) => c.status === k)?._count ?? 0})</Link>
        ))}
      </div>
      <form className="filters" method="GET">
        <input name="q" defaultValue={q} placeholder="منطقة / وصف / معلن" />
        <select name="status" defaultValue={status}>
          <option value="">كل الحالات</option>
          {Object.entries(AD_STATUS).map(([k, v]) => <option key={k} value={k}>{v[0]}</option>)}
        </select>
        <select name="kind" defaultValue={kind}>
          <option value="">كل الأنواع</option>
          {Object.entries(KIND_AR).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        <button className="btn" type="submit">تصفية</button>
      </form>
      <Table head={["الإعلان", "المعلن", "السعر", "الحالة", "صور", "التاريخ"]} empty={rows.length === 0}>
        {rows.map((p) => (
          <tr key={p.id}>
            <td>
              <Link href={`/admin/ads/${p.id}`}>{KIND_AR[p.kind] ?? p.kind} {p.rooms} غرف — {p.district}</Link>
              {p.featured ? <> <Badge tone="info">مميز</Badge></> : null}
            </td>
            <td><Link href={`/admin/users/${p.owner.id}`}>{p.owner.name}</Link></td>
            <td>{fmtMoney(p.price)}</td>
            <td><StatusBadge map={AD_STATUS} value={p.status} /></td>
            <td>{Array.isArray(p.photos) ? p.photos.length : 0}</td>
            <td>{fmtDateTime(p.createdAt)}</td>
          </tr>
        ))}
      </Table>
      <Pager page={page} total={total} size={take} base="/admin/ads" params={{ q, status, kind }} />
    </>
  );
}
