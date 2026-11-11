import Link from "next/link";
import type { Prisma, TicketStatus } from "@prisma/client";
import { PageHeader, Pager, StatusBadge, Table, type SP } from "@/components/ui";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { fmtDateTime, one, pageParams } from "@/lib/format";
import { TICKET_STATUS } from "@/lib/labels";

export default async function TicketsPage({ searchParams }: { searchParams: SP }) {
  await requireAdmin("tickets.manage");
  const sp = await searchParams;
  const status = one(sp.status);
  const { page, take, skip } = pageParams(sp);
  const where: Prisma.SupportTicketWhereInput = status in TICKET_STATUS ? { status: status as TicketStatus } : {};
  const [total, rows] = await Promise.all([
    db.supportTicket.count({ where }),
    db.supportTicket.findMany({ where, orderBy: { createdAt: "desc" }, take, skip, include: { user: { select: { name: true } } } }),
  ]);
  return (
    <>
      <PageHeader title="الدعم الفني" sub="رسائل «تواصل معنا» من التطبيق" />
      <form className="filters" method="GET">
        <select name="status" defaultValue={status}>
          <option value="">كل الحالات</option>
          {Object.entries(TICKET_STATUS).map(([k, v]) => <option key={k} value={k}>{v[0]}</option>)}
        </select>
        <button className="btn" type="submit">تصفية</button>
      </form>
      <Table head={["الموضوع", "المستخدم", "الحالة", "التاريخ"]} empty={rows.length === 0}>
        {rows.map((t) => (
          <tr key={t.id}>
            <td><Link href={`/admin/tickets/${t.id}`}>{t.subject}</Link></td>
            <td>{t.user?.name ?? t.contact ?? "—"}</td>
            <td><StatusBadge map={TICKET_STATUS} value={t.status} /></td>
            <td>{fmtDateTime(t.createdAt)}</td>
          </tr>
        ))}
      </Table>
      <Pager page={page} total={total} size={take} base="/admin/tickets" params={{ status }} />
    </>
  );
}
