import Link from "next/link";
import { notFound } from "next/navigation";
import ConfirmButton from "@/components/ConfirmButton";
import { Badge, Card, PageHeader } from "@/components/ui";
import { audit } from "@/lib/audit";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { fmtDateTime } from "@/lib/format";
import { can } from "@/lib/perms";
import { adminDeleteMessage, setThreadLock, toggleFlag } from "../actions";

export default async function ChatDetail({ params }: { params: Promise<{ id: string }> }) {
  const s = await requireAdmin("chats.read");
  const { id } = await params;
  const t = await db.thread.findUnique({
    where: { id },
    include: { a: { select: { id: true, name: true } }, b: { select: { id: true, name: true } }, messages: { orderBy: { createdAt: "asc" }, take: 500 }, property: { select: { id: true, district: true } } },
  });
  if (!t) notFound();
  await audit(s, "chat.view", "thread", id);
  const mod = can(s.role, "chats.moderate");
  return (
    <>
      <PageHeader title={`${t.a.name} ⇄ ${t.b?.name ?? t.peerName}`} sub={t.property ? `بخصوص إعلان: ${t.property.district}` : undefined}>
        <Link className="btn alt sm" href="/admin/chats">رجوع</Link>
        {mod ? (
          <form action={setThreadLock.bind(null, t.id, !t.locked)}>
            <ConfirmButton message={t.locked ? "فك قفل المحادثة؟" : "قفل المحادثة ومنع الإرسال؟"} className={t.locked ? "btn alt sm" : "btn sm danger"}>
              {t.locked ? "فك القفل" : "قفل المحادثة"}
            </ConfirmButton>
          </form>
        ) : null}
      </PageHeader>
      <Card>
        {t.locked ? <Badge tone="bad">المحادثة مقفلة</Badge> : null}
        {t.messages.length === 0 ? <div className="muted">لا توجد رسائل</div> : null}
        {t.messages.map((m) => (
          <div key={m.id} className={`msg ${m.senderId === t.aId ? "me" : ""} ${m.flagged ? "flag" : ""}`}>
            <div className="muted" style={{ fontSize: 12 }}>
              {m.senderId === t.aId ? t.a.name : t.b?.name ?? t.peerName} · {fmtDateTime(m.createdAt)} {m.edited ? "· معدّلة" : ""}
            </div>
            {m.deleted ? <i className="muted">رسالة محذوفة</i> : <div style={{ whiteSpace: "pre-wrap" }}>{m.text}</div>}
            {!m.deleted && m.fileUrl ? <div><a href={m.fileUrl} target="_blank" rel="noreferrer">مرفق</a></div> : null}
            {mod && !m.deleted ? (
              <div className="row" style={{ marginTop: 4 }}>
                <form action={toggleFlag.bind(null, m.id, t.id)}><button className="btn alt sm" type="submit">{m.flagged ? "إلغاء التعليم" : "تعليم"}</button></form>
                <form action={adminDeleteMessage.bind(null, m.id, t.id)}><ConfirmButton message="حذف الرسالة؟">حذف</ConfirmButton></form>
              </div>
            ) : null}
          </div>
        ))}
      </Card>
    </>
  );
}
