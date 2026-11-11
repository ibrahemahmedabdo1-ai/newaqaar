import { Alert, Card, Field, PageHeader } from "@/components/ui";
import { requireAdmin } from "@/lib/auth";
import { can } from "@/lib/perms";
import { getSettings } from "@/lib/settings";
import { saveSettingsAction } from "./actions";

export default async function SettingsPage({ searchParams }: { searchParams: Promise<{ ok?: string }> }) {
  const s = await requireAdmin("settings.read");
  const { ok } = await searchParams;
  const v = await getSettings();
  const w = can(s.role, "settings.write");
  return (
    <>
      <PageHeader title="الإعدادات" sub="تنعكس على التطبيق عند المزامنة التالية" />
      {ok ? <Alert>تم حفظ الإعدادات.</Alert> : null}
      {!w ? <Alert>صلاحيتك للعرض فقط.</Alert> : null}
      <form action={saveSettingsAction}>
        <fieldset disabled={!w} style={{ border: 0, padding: 0, margin: 0 }}>
          <Card title="الرسوم والمدد">
            <div className="grid g4">
              <Field label="مبلغ الحجز (ر.س)"><input name="bookingAmount" type="number" defaultValue={v.bookingAmount} /></Field>
              <Field label="ضريبة الحجز (ر.س)"><input name="bookingVat" type="number" defaultValue={v.bookingVat} /></Field>
              <Field label="مبلغ الاشتراك (ر.س)"><input name="subscriptionAmount" type="number" defaultValue={v.subscriptionAmount} /></Field>
              <Field label="ضريبة الاشتراك (ر.س)"><input name="subscriptionVat" type="number" defaultValue={v.subscriptionVat} /></Field>
              <Field label="مدة حجز الإعلان (أيام)"><input name="holdDays" type="number" defaultValue={v.holdDays} /></Field>
              <Field label="أقل عرض افتراضي (% من السعر)"><input name="minOfferPercent" type="number" defaultValue={v.minOfferPercent} /></Field>
            </div>
          </Card>
          <Card title="التشغيل">
            <div className="row" style={{ marginBottom: 10 }}>
              <label><input name="allowRegistration" type="checkbox" defaultChecked={v.allowRegistration} /> السماح بالتسجيل الجديد</label>
              <label><input name="requireAdReview" type="checkbox" defaultChecked={v.requireAdReview} /> مراجعة الإعلانات قبل النشر (موصى به)</label>
              <label><input name="maintenance" type="checkbox" defaultChecked={v.maintenance} /> وضع الصيانة</label>
            </div>
            <div className="grid g2">
              <Field label="رسالة الصيانة (عربي)"><input name="maintenanceMessageAr" defaultValue={v.maintenanceMessageAr} /></Field>
              <Field label="Maintenance message (English)"><input name="maintenanceMessageEn" defaultValue={v.maintenanceMessageEn} dir="ltr" /></Field>
            </div>
          </Card>
          <Card title="الإصدارات (للتحديث الإجباري)">
            <div className="grid g4">
              <Field label="أحدث versionCode منشور"><input name="latestVersionCode" type="number" defaultValue={v.latestVersionCode} /></Field>
              <Field label="أقل versionCode مدعوم"><input name="minVersionCode" type="number" defaultValue={v.minVersionCode} /></Field>
            </div>
            <div className="muted">النسخ الأقدم من «أقل versionCode مدعوم» يعرض لها التطبيق شاشة «يلزم التحديث» ويوجّهها لمتجر Google Play.</div>
          </Card>
          <Card title="التواصل">
            <div className="grid g2">
              <Field label="بريد الدعم"><input name="supportEmail" type="email" defaultValue={v.supportEmail} dir="ltr" /></Field>
              <Field label="واتس آب الدعم (بصيغة دولية، مثل 966557545350)"><input name="supportWhatsapp" defaultValue={v.supportWhatsapp} dir="ltr" /></Field>
            </div>
          </Card>
          {w ? <button className="btn gold" type="submit">حفظ الإعدادات</button> : null}
        </fieldset>
      </form>
    </>
  );
}
