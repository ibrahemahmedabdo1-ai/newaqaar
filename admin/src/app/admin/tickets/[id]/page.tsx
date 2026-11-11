import Link from "next/link";
import { notFound } from "next/navigation";
import { Alert, Card, PageHeader, StatusBadge } from "@/components/ui";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { fmtDateTime } from "@/lib/format";
import { TICKET_STATUS } from "@/lib/labels";
import { replyTicket } from "../actions";

export default async function TicketDetail({ params, searchParams }: { params: Promise<{ id: string }>; searchParams: Promise<{ ok?: string }> }) {
  await requireAdmin("tickets.manage");
  const { id } = await params;
  const { ok } = await searchParams;
  const t = await db.supportTicket.findUnique({ where: { id }, include: { user: { select: { id: true, name: true, phone: true } } } });
  if (!t) notFound();
  return (
    <>
      <PageHeader title={t.subject} sub={fmtDateTime(t.createdAt)}>
        <Link className="btn alt sm" href="/admin/tickets">رجوع</Link>
      </PageHeader>
      {ok ? <Alert>تم الحفظ. الرد يظهر للمستخدم داخل التطبيق في صفحة «تواصل معنا» بعد التحديث التالي.</Alert> : null}
      <Card>
        <div className="kv">
          <div className="k">المستخدم</div><div>{t.user ? <Link href={`/admin/users/${t.user.id}`}>{t.user.name}</Link> : "—"} <span dir="ltr">{t.user?.phone ?? t.contact ?? ""}</span></div>
          <div className="k">الحالة</div><div><StatusBadge map={TICKET_STATUS} value={t.status} /></div>
        </div>
        <p style={{ whiteSpace: "pre-wrap" }}>{t.body}</p>
      </Card>
      <Card title="الرد">
        {t.reply ? <p style={{ whiteSpace: "pre-wrap" }}><b>{t.repliedBy}</b> ({fmtDateTime(t.repliedAt)}):<br />{t.reply}</p> : null}
        <form action={replyTicket.bind(null, t.id)}>
          <textarea name="reply" placeholder="اكتب الرد" defaultValue={t.reply ?? ""} />
          <div className="row" style={{ marginTop: 8 }}>
            <select name="status" defaultValue={t.status === "OPEN" ? "IN_PROGRESS" : t.status}>
              {Object.entries(TICKET_STATUS).map(([k, v]) => <option key={k} value={k}>{v[0]}</option>)}
            </select>
            <button className="btn" type="submit">حفظ</button>
          </div>
        </form>
      </Card>
    </>
  );
}
