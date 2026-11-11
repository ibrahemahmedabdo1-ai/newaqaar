import Link from "next/link";
import type { Prisma, ReportStatus } from "@prisma/client";
import { PageHeader, Pager, StatusBadge, Table, type SP } from "@/components/ui";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { fmtDateTime, one, pageParams } from "@/lib/format";
import { REPORT_STATUS } from "@/lib/labels";
import { handleReport } from "./actions";

export default async function ReportsPage({ searchParams }: { searchParams: SP }) {
  await requireAdmin("reports.manage");
  const sp = await searchParams;
  const status = one(sp.status) || "OPEN";
  const { page, take, skip } = pageParams(sp);
  const where: Prisma.ReportWhereInput = status in REPORT_STATUS ? { status: status as ReportStatus } : {};
  const [total, rows] = await Promise.all([
    db.report.count({ where }),
    db.report.findMany({
      where, orderBy: { createdAt: "desc" }, take, skip,
      include: { property: { select: { id: true, district: true, owner: { select: { name: true } } } }, thread: { select: { id: true, peerName: true } }, reporter: { select: { name: true } } },
    }),
  ]);
  return (
    <>
      <PageHeader title="البلاغات" sub="بلاغات المستخدمين عن الإعلانات والمحادثات (سياسة المحتوى)" />
      <div className="row" style={{ marginBottom: 12 }}>
        {Object.entries(REPORT_STATUS).map(([k, v]) => <Link key={k} className="btn alt sm" href={`/admin/reports?status=${k}`}>{v[0]}</Link>)}
        <Link className="btn alt sm" href="/admin/reports?status=ALL">الكل</Link>
      </div>
      <Table head={["الهدف", "السبب", "المبلّغ", "الحالة", "التاريخ", "الإجراء"]} empty={rows.length === 0}>
        {rows.map((r) => (
          <tr key={r.id}>
            <td>
              {r.property ? <Link href={`/admin/ads/${r.property.id}`}>إعلان — {r.property.district} ({r.property.owner.name})</Link> : null}
              {r.thread ? <Link href={`/admin/chats/${r.thread.id}`}>محادثة — {r.thread.peerName}</Link> : null}
              {!r.property && !r.thread ? <span className="muted">محذوف</span> : null}
            </td>
            <td>{r.reason}{r.details ? <div className="muted">{r.details}</div> : null}</td>
            <td>{r.reporter?.name ?? "—"}</td>
            <td><StatusBadge map={REPORT_STATUS} value={r.status} />{r.resolution ? <div className="muted">{r.resolution}</div> : null}</td>
            <td>{fmtDateTime(r.createdAt)}</td>
            <td>
              {r.status === "OPEN" ? (
                <form action={handleReport.bind(null, r.id)} className="row">
                  <select name="action" defaultValue="resolve">
                    <option value="resolve">تمت المعالجة</option>
                    <option value="dismiss">رفض البلاغ</option>
                    {r.property ? <option value="remove_ad">إزالة الإعلان</option> : null}
                    {r.property ? <option value="suspend_owner">إيقاف صاحب الإعلان</option> : null}
                    {r.thread ? <option value="lock_thread">قفل المحادثة</option> : null}
                  </select>
                  <input name="note" placeholder="ملاحظة" />
                  <button className="btn sm" type="submit">تنفيذ</button>
                </form>
              ) : null}
            </td>
          </tr>
        ))}
      </Table>
      <Pager page={page} total={total} size={take} base="/admin/reports" params={{ status }} />
    </>
  );
}
