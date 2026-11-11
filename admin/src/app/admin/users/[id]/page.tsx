import Link from "next/link";
import { notFound } from "next/navigation";
import { Alert, Card, Field, PageHeader, StatusBadge, Table } from "@/components/ui";
import ConfirmButton from "@/components/ConfirmButton";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { fmtDateTime, fmtMoney } from "@/lib/format";
import { AD_STATUS, KIND_AR, OFFER_STATUS, ROLE_AR, USER_STATUS } from "@/lib/labels";
import { can } from "@/lib/perms";
import { deleteUser, forceLogout, resetUserPassword, setUserRole, setUserStatus, toggleSubscription } from "../actions";

export default async function UserDetail({ params, searchParams }: { params: Promise<{ id: string }>; searchParams: Promise<{ ok?: string; err?: string }> }) {
  const s = await requireAdmin("users.read");
  const { id } = await params;
  const { ok, err } = await searchParams;
  const u = await db.user.findUnique({
    where: { id },
    include: {
      properties: { orderBy: { createdAt: "desc" }, take: 10 },
      offers: { orderBy: { createdAt: "desc" }, take: 10, include: { property: { select: { district: true } } } },
      payments: { orderBy: { createdAt: "desc" }, take: 10 },
      _count: { select: { properties: true, offers: true, payments: true, reports: true, favorites: true } },
    },
  });
  if (!u) notFound();
  const w = can(s.role, "users.write");
  return (
    <>
      <PageHeader title={u.name} sub={`${ROLE_AR[u.role]} · ${u.phone}`}>
        <Link className="btn alt sm" href="/admin/users">رجوع</Link>
      </PageHeader>
      {ok === "pw" ? <Alert>تم تغيير كلمة المرور وإنهاء جلسات المستخدم.</Alert> : null}
      {err === "weak" ? <Alert bad>كلمة المرور ضعيفة (٨ أحرف على الأقل وتحتوي على حروف وأرقام).</Alert> : null}
      <div className="grid g2">
        <Card title="البيانات">
          <div className="kv">
            <div className="k">الحالة</div><div><StatusBadge map={USER_STATUS} value={u.status} /> {u.statusNote ? <span className="muted">({u.statusNote})</span> : null}</div>
            <div className="k">الاشتراك</div><div>{u.subscribed ? "مشترك" : "غير مشترك"}</div>
            <div className="k">تاريخ التسجيل</div><div>{fmtDateTime(u.createdAt)}</div>
            <div className="k">آخر دخول</div><div>{fmtDateTime(u.lastLoginAt)}</div>
            <div className="k">إعلانات / عروض</div><div>{u._count.properties} / {u._count.offers}</div>
            <div className="k">مدفوعات / بلاغات</div><div>{u._count.payments} / {u._count.reports}</div>
            <div className="k">المفضلة</div><div>{u._count.favorites}</div>
          </div>
        </Card>
        {w ? (
          <Card title="إجراءات">
            <form action={setUserStatus.bind(null, u.id)} className="row" style={{ marginBottom: 10 }}>
              <select name="status" defaultValue={u.status}>
                {Object.entries(USER_STATUS).map(([k, v]) => <option key={k} value={k}>{v[0]}</option>)}
              </select>
              <input name="note" placeholder="سبب/ملاحظة" defaultValue={u.statusNote ?? ""} />
              <button className="btn sm" type="submit">تغيير الحالة</button>
            </form>
            <form action={setUserRole.bind(null, u.id)} className="row" style={{ marginBottom: 10 }}>
              <select name="role" defaultValue={u.role}>
                {Object.entries(ROLE_AR).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
              <button className="btn sm" type="submit">تغيير الدور</button>
            </form>
            <div className="row" style={{ marginBottom: 10 }}>
              <form action={toggleSubscription.bind(null, u.id)}><button className="btn alt sm" type="submit">{u.subscribed ? "إلغاء الاشتراك" : "تفعيل الاشتراك"}</button></form>
              <form action={forceLogout.bind(null, u.id)}><button className="btn alt sm" type="submit">إنهاء كل الجلسات</button></form>
            </div>
            <form action={resetUserPassword.bind(null, u.id)} className="row" style={{ marginBottom: 10 }}>
              <input name="password" type="text" minLength={8} placeholder="كلمة مرور جديدة" required />
              <ConfirmButton message="تغيير كلمة مرور المستخدم؟" className="btn sm">تعيين كلمة المرور</ConfirmButton>
            </form>
            <form action={deleteUser.bind(null, u.id)}>
              <ConfirmButton message="حذف المستخدم وكل بياناته نهائيًا؟ لا يمكن التراجع.">حذف الحساب نهائيًا</ConfirmButton>
            </form>
          </Card>
        ) : null}
      </div>
      <Card title="آخر الإعلانات">
        <Table head={["الإعلان", "السعر", "الحالة", "التاريخ"]} empty={u.properties.length === 0}>
          {u.properties.map((p) => (
            <tr key={p.id}>
              <td><Link href={`/admin/ads/${p.id}`}>{KIND_AR[p.kind] ?? p.kind} {p.rooms} غرف — {p.district}</Link></td>
              <td>{fmtMoney(p.price)}</td>
              <td><StatusBadge map={AD_STATUS} value={p.status} /></td>
              <td>{fmtDateTime(p.createdAt)}</td>
            </tr>
          ))}
        </Table>
      </Card>
      <Card title="آخر العروض">
        <Table head={["المنطقة", "الدور", "المبلغ", "الحالة", "التاريخ"]} empty={u.offers.length === 0}>
          {u.offers.map((o) => (
            <tr key={o.id}>
              <td><Link href={`/admin/ads/${o.propertyId}`}>{o.property.district}</Link></td>
              <td>{o.floor}</td>
              <td>{fmtMoney(o.amount)}</td>
              <td><StatusBadge map={OFFER_STATUS} value={o.status} /></td>
              <td>{fmtDateTime(o.createdAt)}</td>
            </tr>
          ))}
        </Table>
      </Card>
    </>
  );
}
