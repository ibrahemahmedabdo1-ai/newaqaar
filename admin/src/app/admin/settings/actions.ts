"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { audit } from "@/lib/audit";
import { requireAdmin } from "@/lib/auth";
import { DEFAULTS, getSettings, saveSettings, type Settings } from "@/lib/settings";

export async function saveSettingsAction(f: FormData) {
  const s = await requireAdmin("settings.write");
  const int = (k: string, min: number, max: number, d: number) => {
    const n = Math.floor(Number(f.get(k)));
    return Number.isFinite(n) ? Math.min(max, Math.max(min, n)) : d;
  };
  const txt = (k: string, max = 300) => String(f.get(k) ?? "").trim().slice(0, max);
  const old = await getSettings();
  const next: Partial<Settings> = {
    bookingAmount: int("bookingAmount", 0, 10_000_000, DEFAULTS.bookingAmount),
    bookingVat: int("bookingVat", 0, 10_000_000, DEFAULTS.bookingVat),
    subscriptionAmount: int("subscriptionAmount", 0, 10_000_000, DEFAULTS.subscriptionAmount),
    subscriptionVat: int("subscriptionVat", 0, 10_000_000, DEFAULTS.subscriptionVat),
    holdDays: int("holdDays", 1, 30, DEFAULTS.holdDays),
    minOfferPercent: int("minOfferPercent", 50, 100, DEFAULTS.minOfferPercent),
    minVersionCode: int("minVersionCode", 1, 10_000_000, 1),
    latestVersionCode: int("latestVersionCode", 1, 10_000_000, 1),
    maintenance: f.get("maintenance") === "on",
    allowRegistration: f.get("allowRegistration") === "on",
    requireAdReview: f.get("requireAdReview") === "on",
    maintenanceMessageAr: txt("maintenanceMessageAr"),
    maintenanceMessageEn: txt("maintenanceMessageEn"),
    supportEmail: txt("supportEmail", 120),
    supportWhatsapp: txt("supportWhatsapp", 30),
  };
  await saveSettings(next);
  await audit(s, "settings.update", "settings", undefined, { before: old, after: next });
  revalidatePath("/admin/settings");
  redirect("/admin/settings?ok=1");
}
