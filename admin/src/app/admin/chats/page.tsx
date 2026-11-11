import Link from "next/link";
import type { Prisma } from "@prisma/client";
import { Alert, Badge, PageHeader, Pager, Table, type SP } from "@/components/ui";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { fmtDateTime, one, pageParams } from "@/lib/format";

export default async function ChatsPage({ searchParams }: { searchParams: SP }) {
  await requireAdmin("chats.read");
  const sp = await searchParams;
  const q = one(sp.q).trim();
  const only = one(sp.only);
  const { page, take, skip } = pageParams(sp);
  const where: Prisma.ThreadWhereInput = {
    ...(q ? { OR: [{ a: { name: { contains: q, mode: "insensitive" } } }, { b: { name: { contains: q, mode: "insensitive" } } }] } : {}),
    ...(only === "locked" ? { locked: true } : {}),
    ...(only === "flagged" ? { messages: { some: { flagged: true } } } : {}),
    ...(only === "reported" ? { reports: { some: {} } } : {}),
  };
  const [total, rows] = await Promise.all([
    db.thread.count({ where }),
    db.thread.findMany({
      where, orderBy: { createdAt: "desc" }, take, skip,
      include: { a: { select: { name: true } }, b: { select: { name: true } }, _count: { select: { messages: true, reports: true } }, property: { select: { district: true } } },
    }),
  ]);
  return (
    <>
      <PageHeader title="المحادثات" sub="الإشراف على المحادثات (يُسجَّل كل فتح لمحادثة في سجل العمليات)" />
      <Alert>خصوصية المستخدمين: افتح المحادثات فقط عند وجود بلاغ أو سبب إشرافي واضح. كل اطلاع يُسجَّل باسمك.</Alert>
      <form className="filters" method="GET">
        <input name="q" defaultValue={q} placeholder="اسم أحد الطرفين" />
        <select name="only" defaultValue={only}>
          <option value="">الكل</option>
          <option value="reported">عليها بلاغات</option>
          <option value="flagged">بها رسائل مُعلَّمة</option>
          <option value="locked">المقفلة</option>
        </select>
        <button className="btn" type="submit">تصفية</button>
      </form>
      <Table head={["الطرفان", "الإعلان", "الرسائل", "بلاغات", "الحالة", "التاريخ"]} empty={rows.length === 0}>
        {rows.map((t) => (
          <tr key={t.id}>
            <td><Link href={`/admin/chats/${t.id}`}>{t.a.name} ⇄ {t.b?.name ?? t.peerName}</Link></td>
            <td>{t.property?.district ?? "—"}</td>
            <td>{t._count.messages}</td>
            <td>{t._count.reports > 0 ? <Badge tone="bad">{t._count.reports}</Badge> : "—"}</td>
            <td>{t.locked ? <Badge tone="bad">مقفلة</Badge> : t.blockedBy ? <Badge tone="warn">محظورة</Badge> : <Badge tone="ok">نشطة</Badge>}</td>
            <td>{fmtDateTime(t.createdAt)}</td>
          </tr>
        ))}
      </Table>
      <Pager page={page} total={total} size={take} base="/admin/chats" params={{ q, only }} />
    </>
  );
}
