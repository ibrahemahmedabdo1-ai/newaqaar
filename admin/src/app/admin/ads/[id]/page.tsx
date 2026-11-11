import Link from "next/link";
import { notFound } from "next/navigation";
import ConfirmButton from "@/components/ConfirmButton";
import { Alert, Card, PageHeader, StatusBadge, Table } from "@/components/ui";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { fmtDateTime, fmtMoney } from "@/lib/format";
import { floorsOf } from "@/lib/json";
import { AD_STATUS, KIND_AR, OFFER_STATUS, ROLE_AR } from "@/lib/labels";
import { can } from "@/lib/perms";
import { approveAd, clearHold, deleteAd, endAd, rejectAd, toggleFeatured, updateAdFields } from "../actions";

const FLOOR_AR: Record<string, string> = { AVAILABLE: "متاح", RESERVED: "محجوز", SOLD: "مباع" };

export default async function AdDetail({ params, searchParams }: { params: Promise<{ id: string }>; searchParams: Promise<{ ok?: string; err?: string }> }) {
  const s = await requireAdmin("ads.read");
  const { id } = await params;
  const { ok, err } = await searchParams;
  const p = await db.property.findUnique({
    where: { id },
    include: { owner: true, offers: { orderBy: { createdAt: "desc" }, take: 10, include: { byUser: { select: { name: true } } } }, reports: { orderBy: { createdAt: "desc" }, take: 10 }, _count: { select: { favorites: true } } },
  });
  if (!p) notFound();
  const photos = Array.isArray(p.photos) ? (p.photos as string[]) : [];
  const mod = can(s.role, "ads.moderate");
  return (
    <>
      <PageHeader title={`${KIND_AR[p.kind] ?? p.kind} ${p.rooms} غرف — ${p.district}`} sub={`${fmtMoney(p.price)} · ${p.area} م²`}>
        <Link className="btn alt sm" href="/admin/ads">رجوع</Link>
      </PageHeader>
      {ok ? <Alert>تم الحفظ.</Alert> : null}
      {err === "reason" ? <Alert bad>سبب الرفض مطلوب.</Alert> : null}
      {err === "fields" ? <Alert bad>تحقق من الأسعار (أقل عرض ≤ السعر) والوصف.</Alert> : null}
      <div className="grid g2">
        <Card title="التفاصيل">
          <div className="kv">
            <div className="k">الحالة</div><div><StatusBadge map={AD_STATUS} value={p.status} /> {p.rejectReason ? <span className="muted">({p.rejectReason})</span> : null}</div>
            <div className="k">المعلن</div><div><Link href={`/admin/users/${p.owner.id}`}>{p.owner.name}</Link> · {ROLE_AR[p.owner.role]}</div>
            <div className="k">الهاتف</div><div dir="ltr" style={{ textAlign: "end" }}>{p.owner.phone}</div>
            <div className="k">السعر / أقل عرض</div><div>{fmtMoney(p.price)} / {fmtMoney(p.minOffer)}</div>
            <div className="k">العمولة</div><div>{fmtMoney(p.commission)}</div>
            <div className="k">الحمامات / الواجهة</div><div>{p.baths} / {p.direction || "—"}</div>
            <div className="k">محجوز حتى</div><div>{p.hiddenUntil && p.hiddenUntil > new Date() ? fmtDateTime(p.hiddenUntil) : "—"}</div>
            <div className="k">أُنشئ / نُشر</div><div>{fmtDateTime(p.createdAt)} / {fmtDateTime(p.publishedAt)}</div>
            <div className="k">المفضلة</div><div>{p._count.favorites}</div>
          </div>
          <p style={{ whiteSpace: "pre-wrap" }}>{p.description}</p>
          <div className="row">
            {floorsOf(p.floors).map((f) => <span key={f.name} className={`badge ${f.state === "AVAILABLE" ? "ok" : f.state === "RESERVED" ? "warn" : ""}`}>{f.name}: {FLOOR_AR[f.state]}</span>)}
          </div>
        </Card>
        <Card title="الصور">
          {photos.length === 0 ? <div className="muted">لا توجد صور</div> : (
            <div className="photos">{photos.map((u) => <a key={u} href={u} target="_blank" rel="noreferrer"><img src={u} alt="" loading="lazy" /></a>)}</div>
          )}
        </Card>
      </div>
      {mod ? (
        <Card title="إجراءات الإشراف">
          <div className="row" style={{ marginBottom: 12 }}>
            {p.status !== "PUBLISHED" ? <form action={approveAd.bind(null, p.id)}><button className="btn gold sm" type="submit">موافقة ونشر</button></form> : null}
            {p.status === "PUBLISHED" ? <form action={endAd.bind(null, p.id)}><ConfirmButton message="إنهاء الإعلان؟" className="btn alt sm">إنهاء</ConfirmButton></form> : null}
            <form action={toggleFeatured.bind(null, p.id)}><button className="btn alt sm" type="submit">{p.featured ? "إلغاء التمييز" : "تمييز الإعلان"}</button></form>
            {p.hiddenUntil ? <form action={clearHold.bind(null, p.id)}><button className="btn alt sm" type="submit">إلغاء الحجز المؤقت</button></form> : null}
            <form action={deleteAd.bind(null, p.id)}><ConfirmButton message="حذف الإعلان نهائيًا؟">حذف</ConfirmButton></form>
          </div>
          <form action={rejectAd.bind(null, p.id)} className="row" style={{ marginBottom: 12 }}>
            <input name="reason" placeholder="سبب الرفض (يظهر للمعلن)" style={{ minWidth: 280 }} />
            <button className="btn danger sm" type="submit">رفض الإعلان</button>
          </form>
          <form action={updateAdFields.bind(null, p.id)}>
            <div className="row">
              <input name="price" type="number" defaultValue={p.price} placeholder="السعر" />
              <input name="minOffer" type="number" defaultValue={p.minOffer} placeholder="أقل عرض" />
              <input name="commission" type="number" defaultValue={p.commission} placeholder="العمولة" />
            </div>
            <textarea name="description" defaultValue={p.description} style={{ marginTop: 8 }} />
            <button className="btn sm" type="submit" style={{ marginTop: 8 }}>حفظ التعديلات</button>
          </form>
        </Card>
      ) : null}
      <Card title="العروض على هذا الإعلان">
        <Table head={["المقدِّم", "العميل", "الدور", "المبلغ", "الحالة", "التاريخ"]} empty={p.offers.length === 0}>
          {p.offers.map((o) => (
            <tr key={o.id}>
              <td>{o.byUser.name}</td><td>{o.clientName}</td><td>{o.floor}</td><td>{fmtMoney(o.amount)}</td>
              <td><StatusBadge map={OFFER_STATUS} value={o.status} /></td><td>{fmtDateTime(o.createdAt)}</td>
            </tr>
          ))}
        </Table>
      </Card>
      <Card title="البلاغات">
        <Table head={["السبب", "الحالة", "التاريخ"]} empty={p.reports.length === 0}>
          {p.reports.map((r) => <tr key={r.id}><td>{r.reason}</td><td>{r.status}</td><td>{fmtDateTime(r.createdAt)}</td></tr>)}
        </Table>
      </Card>
    </>
  );
}
