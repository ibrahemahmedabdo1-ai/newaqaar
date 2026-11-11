import { Card, PageHeader } from "@/components/ui";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { savePage } from "../actions";

const KEYS: [string, string][] = [["terms", "الشروط والأحكام"], ["privacy", "سياسة الخصوصية"], ["about", "عن بوابة العروض"]];

export default async function PagesPage() {
  await requireAdmin("content.manage");
  const rows = await db.page.findMany();
  return (
    <>
      <PageHeader title="الصفحات القانونية" sub="تظهر داخل التطبيق. تأكد أن سياسة الخصوصية تطابق ما يحدث فعليًا (متطلب Google Play)." />
      {KEYS.map(([key, label]) => {
        const p = rows.find((r) => r.key === key);
        return (
          <Card key={key} title={label}>
            <form action={savePage.bind(null, key)}>
              <div className="grid g2">
                <div>
                  <div className="field"><label>العنوان (عربي)</label><input name="titleAr" defaultValue={p?.titleAr ?? label} /></div>
                  <div className="field"><label>النص (عربي)</label><textarea name="bodyAr" defaultValue={p?.bodyAr ?? ""} style={{ minHeight: 220 }} /></div>
                </div>
                <div>
                  <div className="field"><label>Title (English)</label><input name="titleEn" defaultValue={p?.titleEn ?? ""} dir="ltr" /></div>
                  <div className="field"><label>Body (English)</label><textarea name="bodyEn" defaultValue={p?.bodyEn ?? ""} dir="ltr" style={{ minHeight: 220 }} /></div>
                </div>
              </div>
              <button className="btn sm" type="submit">حفظ</button>
            </form>
          </Card>
        );
      })}
    </>
  );
}
