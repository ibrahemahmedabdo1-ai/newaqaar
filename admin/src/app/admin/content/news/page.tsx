import ConfirmButton from "@/components/ConfirmButton";
import { Badge, Card, PageHeader, Table } from "@/components/ui";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { fmtDate } from "@/lib/format";
import { deleteNews, saveNews } from "../actions";

type N = { titleAr: string; titleEn: string; bodyAr: string; bodyEn: string; imageUrl: string | null; published: boolean };

function NewsForm({ id, n }: { id: string; n?: N }) {
  return (
    <form action={saveNews.bind(null, id)}>
      <div className="grid g2">
        <div>
          <div className="field"><label>العنوان (عربي)</label><input name="titleAr" defaultValue={n?.titleAr} required /></div>
          <div className="field"><label>النص (عربي)</label><textarea name="bodyAr" defaultValue={n?.bodyAr} required /></div>
        </div>
        <div>
          <div className="field"><label>Title (English)</label><input name="titleEn" defaultValue={n?.titleEn} dir="ltr" /></div>
          <div className="field"><label>Body (English)</label><textarea name="bodyEn" defaultValue={n?.bodyEn} dir="ltr" /></div>
        </div>
      </div>
      <div className="field"><label>رابط الصورة (https)</label><input name="imageUrl" defaultValue={n?.imageUrl ?? ""} dir="ltr" /></div>
      <div className="row">
        <label><input name="published" type="checkbox" defaultChecked={n?.published ?? false} /> نشر</label>
        <button className="btn sm" type="submit">حفظ</button>
      </div>
    </form>
  );
}

export default async function NewsPage() {
  await requireAdmin("content.manage");
  const rows = await db.news.findMany({ orderBy: { createdAt: "desc" }, take: 100 });
  return (
    <>
      <PageHeader title="أخبار العقارات" sub="تظهر في قائمة «أخبار العقارات» داخل التطبيق" />
      <Card title="خبر جديد"><NewsForm id="new" /></Card>
      <Table head={["الخبر", "الحالة", "النشر", ""]} empty={rows.length === 0}>
        {rows.map((r) => (
          <tr key={r.id}>
            <td><details><summary>{r.titleAr}</summary><div style={{ marginTop: 10 }}><NewsForm id={r.id} n={r} /></div></details></td>
            <td>{r.published ? <Badge tone="ok">منشور</Badge> : <Badge>مسودة</Badge>}</td>
            <td>{fmtDate(r.publishedAt)}</td>
            <td><form action={deleteNews.bind(null, r.id)}><ConfirmButton message="حذف الخبر؟">حذف</ConfirmButton></form></td>
          </tr>
        ))}
      </Table>
    </>
  );
}
