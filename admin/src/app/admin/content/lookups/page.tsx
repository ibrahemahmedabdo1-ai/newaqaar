import { Badge, Card, PageHeader, Table } from "@/components/ui";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { addDistrict, toggleDistrict, toggleType } from "../actions";

export default async function LookupsPage() {
  await requireAdmin("content.manage");
  const [districts, types] = await Promise.all([db.district.findMany({ orderBy: { name: "asc" } }), db.propertyType.findMany({ orderBy: { key: "asc" } })]);
  return (
    <>
      <PageHeader title="المناطق وأنواع العقار" sub="القوائم التي يستخدمها التطبيق في الفلاتر ونموذج الإعلان" />
      <Card title="إضافة / تعديل منطقة">
        <form action={addDistrict} className="row">
          <input name="name" placeholder="الاسم (عربي)" required />
          <input name="nameEn" placeholder="Name (English)" dir="ltr" />
          <input name="lat" type="number" step="any" placeholder="خط العرض" required />
          <input name="lng" type="number" step="any" placeholder="خط الطول" required />
          <button className="btn sm" type="submit">حفظ</button>
        </form>
      </Card>
      <div className="grid g2">
        <div>
          <h3>المناطق</h3>
          <Table head={["المنطقة", "الإحداثيات", ""]} empty={districts.length === 0}>
            {districts.map((d) => (
              <tr key={d.id}>
                <td>{d.name} {d.active ? null : <Badge>معطّلة</Badge>}</td>
                <td dir="ltr">{d.lat.toFixed(3)}, {d.lng.toFixed(3)}</td>
                <td><form action={toggleDistrict.bind(null, d.id)}><button className="btn alt sm" type="submit">{d.active ? "تعطيل" : "تفعيل"}</button></form></td>
              </tr>
            ))}
          </Table>
        </div>
        <div>
          <h3>أنواع العقار</h3>
          <Table head={["النوع", "المفتاح", ""]} empty={types.length === 0}>
            {types.map((t) => (
              <tr key={t.key}>
                <td>{t.labelAr} {t.active ? null : <Badge>معطّل</Badge>}</td>
                <td dir="ltr">{t.key}</td>
                <td><form action={toggleType.bind(null, t.key)}><button className="btn alt sm" type="submit">{t.active ? "تعطيل" : "تفعيل"}</button></form></td>
              </tr>
            ))}
          </Table>
        </div>
      </div>
    </>
  );
}
