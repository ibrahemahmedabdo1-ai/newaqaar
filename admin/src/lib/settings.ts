import { db } from "./db";

export const DEFAULTS = {
  bookingAmount: 1000,
  bookingVat: 20,
  subscriptionAmount: 300,
  subscriptionVat: 20,
  holdDays: 3,
  minOfferPercent: 91,
  maintenance: false,
  maintenanceMessageAr: "التطبيق تحت الصيانة حاليًا، نعود قريبًا.",
  maintenanceMessageEn: "The app is under maintenance. We will be back soon.",
  minVersionCode: 1,
  latestVersionCode: 1,
  supportEmail: "",
  supportWhatsapp: "",
  allowRegistration: true,
  requireAdReview: true,
};

export type Settings = typeof DEFAULTS;

export async function getSettings(): Promise<Settings> {
  const rows = await db.setting.findMany();
  const out: Record<string, unknown> = { ...DEFAULTS };
  for (const r of rows) if (r.key in DEFAULTS) out[r.key] = r.value;
  return out as Settings;
}

export async function saveSettings(patch: Partial<Settings>) {
  for (const [key, value] of Object.entries(patch)) {
    if (!(key in DEFAULTS)) continue;
    await db.setting.upsert({
      where: { key }, update: { value: value as never }, create: { key, value: value as never },
    });
  }
}
