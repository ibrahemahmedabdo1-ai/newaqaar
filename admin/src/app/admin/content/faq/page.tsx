import ConfirmButton from "@/components/ConfirmButton";
import { Badge, Card, PageHeader, Table } from "@/components/ui";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { deleteFaq, saveFaq } from "../actions";

function FaqForm({ id, f }: { id: string; f?: { sort: number; qAr: string; aAr: string; qEn: string; aEn: string; published: boolean } }) {
  return (
    <form action={saveFaq.bind(null, id)}>
      <div className="grid g2">
        <div><div className="field"><label>السؤال (عربي)</label><input name="qAr" defaultValue={f?.qAr} required /></div><div className="field"><label>الإجابة (عربي)</label><textarea name="aAr" defaultValue={f?.aAr} required /></div></div>
        <div><div className="field"><label>Question (English)</label><input name="qEn" defaultValue={f?.qEn} dir="ltr" /></div><div className="field"><label>Answer (English)</label><textarea name="aEn" defaultValue={f?.aEn} dir="ltr" /></div></div>
      </div>
      <div className="row">
        <label>الترتيب <input name="sort" type="number" defaultValue={f?.sort ?? 0} style={{ width: 80 }} /></label>
        <label><input name="published" type="checkbox" defaultChecked={f?.published ?? true} /> منشور</label>
        <button className="btn sm" type="submit">حفظ</button>
      </div>
    </form>
  );
}

export default async function FaqPage() {
  await requireAdmin("content.manage");
  const rows = await db.faq.findMany({ orderBy: { sort: "asc" } });
  return (
    <>
      <PageHeader title="الأسئلة الشائعة" sub="تظهر داخل التطبيق في صفحة الأسئلة الشائعة" />
      <Card title="إضافة سؤال"><FaqForm id="new" /></Card>
      <Table head={["السؤال", "الحالة", ""]} empty={rows.length === 0}>
        {rows.map((r) => (
          <tr key={r.id}>
            <td>
              <details>
                <summary>{r.qAr}</summary>
                <div style={{ marginTop: 10 }}><FaqForm id={r.id} f={r} /></div>
              </details>
            </td>
            <td>{r.published ? <Badge tone="ok">منشور</Badge> : <Badge>مخفي</Badge>}</td>
            <td><form action={deleteFaq.bind(null, r.id)}><ConfirmButton message="حذف السؤال؟">حذف</ConfirmButton></form></td>
          </tr>
        ))}
      </Table>
    </>
  );
}
