import ConfirmButton from "@/components/ConfirmButton";
import { Alert, Badge, Card, PageHeader, Table } from "@/components/ui";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { fmtDateTime } from "@/lib/format";
import { ROLE_AR } from "@/lib/labels";
import { createAnnouncement, deleteAnnouncement, toggleAnnouncement } from "./actions";

export default async function AnnouncementsPage() {
  await requireAdmin("announce.manage");
  const rows = await db.announcement.findMany({ orderBy: { createdAt: "desc" }, take: 50 });
  return (
    <>
      <PageHeader title="الإعلانات داخل التطبيق" sub="رسائل تظهر للمستخدمين عند فتح التطبيق" />
      <Alert>هذه رسائل داخل التطبيق (تُجلب مع كل مزامنة). الإشعارات الفورية (Push) تحتاج ربط Firebase Cloud Messaging لاحقًا.</Alert>
      <Card title="رسالة جديدة">
        <form action={createAnnouncement}>
          <div className="grid g2">
            <div>
              <div className="field"><label>العنوان (عربي)</label><input name="titleAr" required /></div>
              <div className="field"><label>النص (عربي)</label><textarea name="bodyAr" required /></div>
            </div>
            <div>
              <div className="field"><label>Title (English)</label><input name="titleEn" dir="ltr" /></div>
              <div className="field"><label>Body (English)</label><textarea name="bodyEn" dir="ltr" /></div>
            </div>
          </div>
          <div className="row">
            <select name="audience"><option value="ALL">الكل</option>{Object.entries(ROLE_AR).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select>
            <select name="level"><option value="INFO">معلومة</option><option value="WARN">تنبيه</option></select>
            <label>ينتهي في <input name="endsAt" type="datetime-local" /></label>
            <button className="btn sm" type="submit">نشر</button>
          </div>
        </form>
      </Card>
      <Table head={["العنوان", "الجمهور", "المستوى", "الحالة", "التاريخ", ""]} empty={rows.length === 0}>
        {rows.map((a) => (
          <tr key={a.id}>
            <td>{a.titleAr}</td>
            <td>{a.audience === "ALL" ? "الكل" : ROLE_AR[a.audience]}</td>
            <td>{a.level === "WARN" ? <Badge tone="warn">تنبيه</Badge> : <Badge tone="info">معلومة</Badge>}</td>
            <td>{a.active ? <Badge tone="ok">فعّالة</Badge> : <Badge>متوقفة</Badge>}</td>
            <td>{fmtDateTime(a.createdAt)}</td>
            <td>
              <div className="row">
                <form action={toggleAnnouncement.bind(null, a.id)}><button className="btn alt sm" type="submit">{a.active ? "إيقاف" : "تفعيل"}</button></form>
                <form action={deleteAnnouncement.bind(null, a.id)}><ConfirmButton message="حذف الرسالة؟">حذف</ConfirmButton></form>
              </div>
            </td>
          </tr>
        ))}
      </Table>
    </>
  );
}
