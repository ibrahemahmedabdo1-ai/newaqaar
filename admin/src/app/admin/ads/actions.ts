"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { audit } from "@/lib/audit";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";

const back = (id: string) => revalidatePath(`/admin/ads/${id}`);

export async function approveAd(id: string) {
  const s = await requireAdmin("ads.moderate");
  const p = await db.property.findUnique({ where: { id }, select: { publishedAt: true } });
  if (!p) return;
  await db.property.update({ where: { id }, data: { status: "PUBLISHED", rejectReason: null, publishedAt: p.publishedAt ?? new Date(), endedAt: null } });
  await audit(s, "ad.approve", "property", id);
  back(id);
  revalidatePath("/admin/ads");
}

export async function rejectAd(id: string, formData: FormData) {
  const s = await requireAdmin("ads.moderate");
  const reason = String(formData.get("reason") ?? "").trim().slice(0, 300);
  if (!reason) redirect(`/admin/ads/${id}?err=reason`);
  await db.property.update({ where: { id }, data: { status: "REJECTED", rejectReason: reason } });
  await audit(s, "ad.reject", "property", id, { reason });
  back(id);
  revalidatePath("/admin/ads");
}

export async function endAd(id: string) {
  const s = await requireAdmin("ads.moderate");
  await db.property.update({ where: { id }, data: { status: "ENDED", endedAt: new Date() } });
  await audit(s, "ad.end", "property", id);
  back(id);
}

export async function toggleFeatured(id: string) {
  const s = await requireAdmin("ads.moderate");
  const p = await db.property.findUnique({ where: { id }, select: { featured: true } });
  if (!p) return;
  await db.property.update({ where: { id }, data: { featured: !p.featured } });
  await audit(s, "ad.featured", "property", id, { featured: !p.featured });
  back(id);
}

export async function clearHold(id: string) {
  const s = await requireAdmin("ads.moderate");
  await db.property.update({ where: { id }, data: { hiddenUntil: null } });
  await audit(s, "ad.clear_hold", "property", id);
  back(id);
}

export async function updateAdFields(id: string, formData: FormData) {
  const s = await requireAdmin("ads.moderate");
  const num = (k: string) => Math.max(0, Math.floor(Number(formData.get(k) ?? 0)) || 0);
  const price = num("price");
  const minOffer = num("minOffer");
  const commission = num("commission");
  const description = String(formData.get("description") ?? "").trim().slice(0, 2000);
  if (price <= 0 || minOffer <= 0 || minOffer > price || description.length < 5) redirect(`/admin/ads/${id}?err=fields`);
  await db.property.update({ where: { id }, data: { price, minOffer, commission, description } });
  await audit(s, "ad.edit", "property", id, { price, minOffer, commission });
  redirect(`/admin/ads/${id}?ok=1`);
}

export async function deleteAd(id: string) {
  const s = await requireAdmin("ads.moderate");
  await db.property.delete({ where: { id } });
  await audit(s, "ad.delete", "property", id);
  redirect("/admin/ads");
}
