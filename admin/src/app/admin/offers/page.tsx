import Link from "next/link";
import type { OfferStatus, Prisma } from "@prisma/client";
import { PageHeader, Pager, StatusBadge, Table, type SP } from "@/components/ui";
import ConfirmButton from "@/components/ConfirmButton";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { fmtDateTime, fmtMoney, one, pageParams } from "@/lib/format";
import { OFFER_STATUS, PAY_STATUS } from "@/lib/labels";
import { can } from "@/lib/perms";
import { adminAcceptOffer, adminRejectOffer } from "./actions";

export default async function OffersPage({ searchParams }: { searchParams: SP }) {
  const s = await requireAdmin("offers.read");
  const sp = await searchParams;
  const q = one(sp.q).trim();
  const status = one(sp.status);
  const { page, take, skip } = pageParams(sp);
  const where: Prisma.OfferWhereInput = {
    ...(q ? { OR: [{ clientName: { contains: q, mode: "insensitive" } }, { clientPhone: { contains: q } }, { byUser: { name: { contains: q, mode: "insensitive" } } }] } : {}),
    ...(status in OFFER_STATUS ? { status: status as OfferStatus } : {}),
  };
  const [total, rows] = await Promise.all([
    db.offer.count({ where }),
    db.offer.findMany({ where, orderBy: { createdAt: "desc" }, take, skip, include: { byUser: { select: { id: true, name: true } }, property: { select: { id: true, district: true, price: true } }, payment: { select: { status: true } } } }),
  ]);
  const w = can(s.role, "offers.write");
  return (
    <>
      <PageHeader title="العروض والحجوزات" sub="متابعة العروض المقدمة ومدفوعات الحجز" >
        {can(s.role, "export") ? <a className="btn alt sm" href="/admin/export/offers">تصدير CSV</a> : null}
      </PageHeader>
      <form className="filters" method="GET">
        <input name="q" defaultValue={q} placeholder="اسم/هاتف العميل أو المقدِّم" />
        <select name="status" defaultValue={status}>
          <option value="">كل الحالات</option>
          {Object.entries(OFFER_STATUS).map(([k, v]) => <option key={k} value={k}>{v[0]}</option>)}
        </select>
        <button className="btn" type="submit">تصفية</button>
      </form>
      <Table head={["الإعلان", "المقدِّم", "العميل", "الدور", "السعر / العرض", "الحالة", "الدفع", "التاريخ", ""]} empty={rows.length === 0}>
        {rows.map((o) => (
          <tr key={o.id}>
            <td><Link href={`/admin/ads/${o.property.id}`}>{o.property.district}</Link></td>
            <td><Link href={`/admin/users/${o.byUser.id}`}>{o.byUser.name}</Link></td>
            <td>{o.clientName}<div className="muted" dir="ltr" style={{ textAlign: "end" }}>{o.clientPhone}</div></td>
            <td>{o.floor}</td>
            <td>{fmtMoney(o.property.price)}<div className="muted">عرض: {fmtMoney(o.amount)}</div></td>
            <td><StatusBadge map={OFFER_STATUS} value={o.status} /></td>
            <td>{o.payment ? <StatusBadge map={PAY_STATUS} value={o.payment.status} /> : "—"}</td>
            <td>{fmtDateTime(o.createdAt)}</td>
            <td>
              {w && o.status === "PENDING" ? (
                <div className="row">
                  <form action={adminAcceptOffer.bind(null, o.id)}><ConfirmButton message="قبول العرض؟" className="btn sm gold">قبول</ConfirmButton></form>
                  <form action={adminRejectOffer.bind(null, o.id)}><ConfirmButton message="رفض العرض واسترداد مبلغ الحجز؟">رفض</ConfirmButton></form>
                </div>
              ) : null}
            </td>
          </tr>
        ))}
      </Table>
      <Pager page={page} total={total} size={take} base="/admin/offers" params={{ q, status }} />
    </>
  );
}
