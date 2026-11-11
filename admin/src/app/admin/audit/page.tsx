import type { Prisma } from "@prisma/client";
import { PageHeader, Pager, Table, type SP } from "@/components/ui";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { fmtDateTime, one, pageParams } from "@/lib/format";

export default async function AuditPage({ searchParams }: { searchParams: SP }) {
  await requireAdmin("audit.read");
  const sp = await searchParams;
  const entity = one(sp.entity).trim();
  const q = one(sp.q).trim();
  const { page, take, skip } = pageParams(sp, 30);
  const where: Prisma.AuditLogWhereInput = {
    ...(entity ? { entity } : {}),
    ...(q ? { OR: [{ adminName: { contains: q, mode: "insensitive" } }, { action: { contains: q, mode: "insensitive" } }, { entityId: { contains: q } }] } : {}),
  };
  const [total, rows, entities] = await Promise.all([
    db.auditLog.count({ where }),
    db.auditLog.findMany({ where, orderBy: { createdAt: "desc" }, take, skip }),
    db.auditLog.groupBy({ by: ["entity"], _count: true }),
  ]);
  return (
    <>
      <PageHeader title="سجل العمليات" sub="كل إجراء إداري يُسجَّل باسم منفّذه (لا يمكن تعديله من اللوحة)" />
      <form className="filters" method="GET">
        <input name="q" defaultValue={q} placeholder="مسؤول / إجراء / معرّف" />
        <select name="entity" defaultValue={entity}>
          <option value="">كل الكيانات</option>
          {entities.map((e) => <option key={e.entity} value={e.entity}>{e.entity} ({e._count})</option>)}
        </select>
        <button className="btn" type="submit">تصفية</button>
      </form>
      <Table head={["الوقت", "المسؤول", "الإجراء", "الكيان", "المعرّف", "تفاصيل", "IP"]} empty={rows.length === 0}>
        {rows.map((r) => (
          <tr key={r.id}>
            <td>{fmtDateTime(r.createdAt)}</td>
            <td>{r.adminName}</td>
            <td><code>{r.action}</code></td>
            <td>{r.entity}</td>
            <td dir="ltr" style={{ textAlign: "end" }}>{r.entityId?.slice(0, 12) ?? "—"}</td>
            <td style={{ maxWidth: 280, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }} dir="ltr">{r.meta ? JSON.stringify(r.meta) : ""}</td>
            <td dir="ltr">{r.ip ?? ""}</td>
          </tr>
        ))}
      </Table>
      <Pager page={page} total={total} size={take} base="/admin/audit" params={{ q, entity }} />
    </>
  );
}
