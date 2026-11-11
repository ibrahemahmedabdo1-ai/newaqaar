import { Alert, Badge, Card, PageHeader, Table } from "@/components/ui";
import ConfirmButton from "@/components/ConfirmButton";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { fmtDateTime } from "@/lib/format";
import { ROLE_LABELS } from "@/lib/perms";
import { createAdmin, resetAdminPassword, setAdminRole, toggleAdmin } from "./actions";

const ERR: Record<string, string> = {
  invalid: "بيانات غير صحيحة (البريد صحيح، كلمة المرور 12 حرفًا على الأقل).",
  exists: "هذا البريد مسجل بالفعل.",
  self: "لا يمكنك تعديل صلاحياتك أو إيقاف حسابك بنفسك.",
  last: "لا يمكن إزالة آخر مدير عام نشط.",
};

export default async function TeamPage({ searchParams }: { searchParams: Promise<{ err?: string }> }) {
  const me = await requireAdmin("team.manage");
  const { err } = await searchParams;
  const rows = await db.adminUser.findMany({ orderBy: { createdAt: "asc" } });
  return (
    <>
      <PageHeader title="فريق الإدارة" sub="حسابات لوحة التحكم والصلاحيات" />
      {err ? <Alert bad>{ERR[err] ?? "حدث خطأ"}</Alert> : null}
      <Card title="إضافة مسؤول">
        <form action={createAdmin} className="row">
          <input name="name" placeholder="الاسم" required />
          <input name="email" type="email" placeholder="البريد" dir="ltr" required />
          <select name="role" defaultValue="MODERATOR">
            {Object.entries(ROLE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
          <input name="password" type="text" placeholder="كلمة مرور (12+ حرفًا)" minLength={12} required />
          <button className="btn sm" type="submit">إضافة</button>
        </form>
      </Card>
      <Table head={["الاسم", "البريد", "الصلاحية", "الحالة", "آخر دخول", ""]} empty={rows.length === 0}>
        {rows.map((a) => (
          <tr key={a.id}>
            <td>{a.name} {a.id === me.id ? <Badge tone="info">أنت</Badge> : null}</td>
            <td dir="ltr">{a.email}</td>
            <td>
              <form action={setAdminRole.bind(null, a.id)} className="row">
                <select name="role" defaultValue={a.role}>{Object.entries(ROLE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select>
                <button className="btn alt sm" type="submit">تغيير</button>
              </form>
            </td>
            <td>{a.active ? <Badge tone="ok">نشط</Badge> : <Badge tone="bad">موقوف</Badge>}</td>
            <td>{fmtDateTime(a.lastLoginAt)}</td>
            <td>
              <div className="row">
                <form action={toggleAdmin.bind(null, a.id)}><ConfirmButton message={a.active ? "إيقاف هذا الحساب؟" : "تفعيل هذا الحساب؟"} className="btn alt sm">{a.active ? "إيقاف" : "تفعيل"}</ConfirmButton></form>
                <form action={resetAdminPassword.bind(null, a.id)} className="row">
                  <input name="password" placeholder="كلمة مرور جديدة" minLength={12} style={{ width: 150 }} />
                  <button className="btn alt sm" type="submit">تعيين</button>
                </form>
              </div>
            </td>
          </tr>
        ))}
      </Table>
    </>
  );
}
