"use server";

import { revalidatePath } from "next/cache";
import { audit } from "@/lib/audit";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";

const str = (f: FormData, k: string, max = 4000) => String(f.get(k) ?? "").trim().slice(0, max);
const bool = (f: FormData, k: string) => f.get(k) === "on";

// ---- FAQ ----
export async function saveFaq(id: string, f: FormData) {
  const s = await requireAdmin("content.manage");
  const data = { sort: Number(f.get("sort") ?? 0) || 0, qAr: str(f, "qAr", 300), aAr: str(f, "aAr"), qEn: str(f, "qEn", 300), aEn: str(f, "aEn"), published: bool(f, "published") };
  if (!data.qAr || !data.aAr) return;
  if (id === "new") {
    const c = await db.faq.create({ data });
    await audit(s, "faq.create", "faq", c.id);
  } else {
    await db.faq.update({ where: { id }, data });
    await audit(s, "faq.update", "faq", id);
  }
  revalidatePath("/admin/content/faq");
}

export async function deleteFaq(id: string) {
  const s = await requireAdmin("content.manage");
  await db.faq.delete({ where: { id } });
  await audit(s, "faq.delete", "faq", id);
  revalidatePath("/admin/content/faq");
}

// ---- Pages ----
export async function savePage(key: string, f: FormData) {
  const s = await requireAdmin("content.manage");
  if (!["terms", "privacy", "about"].includes(key)) return;
  const data = { titleAr: str(f, "titleAr", 120), titleEn: str(f, "titleEn", 120), bodyAr: str(f, "bodyAr", 40000), bodyEn: str(f, "bodyEn", 40000) };
  await db.page.upsert({ where: { key }, update: data, create: { key, ...data } });
  await audit(s, "page.update", "page", key);
  revalidatePath("/admin/content/pages");
}

// ---- News ----
export async function saveNews(id: string, f: FormData) {
  const s = await requireAdmin("content.manage");
  const published = bool(f, "published");
  const data = {
    titleAr: str(f, "titleAr", 200), titleEn: str(f, "titleEn", 200), bodyAr: str(f, "bodyAr"), bodyEn: str(f, "bodyEn"),
    imageUrl: str(f, "imageUrl", 500) || null, published,
  };
  if (!data.titleAr || !data.bodyAr) return;
  if (id === "new") {
    const c = await db.news.create({ data: { ...data, publishedAt: published ? new Date() : null } });
    await audit(s, "news.create", "news", c.id);
  } else {
    const old = await db.news.findUnique({ where: { id }, select: { publishedAt: true } });
    await db.news.update({ where: { id }, data: { ...data, publishedAt: published ? old?.publishedAt ?? new Date() : null } });
    await audit(s, "news.update", "news", id);
  }
  revalidatePath("/admin/content/news");
}

export async function deleteNews(id: string) {
  const s = await requireAdmin("content.manage");
  await db.news.delete({ where: { id } });
  await audit(s, "news.delete", "news", id);
  revalidatePath("/admin/content/news");
}

// ---- Lookups ----
export async function addDistrict(f: FormData) {
  const s = await requireAdmin("content.manage");
  const name = str(f, "name", 60);
  const lat = Number(f.get("lat"));
  const lng = Number(f.get("lng"));
  if (!name || !Number.isFinite(lat) || !Number.isFinite(lng)) return;
  await db.district.upsert({ where: { name }, update: { lat, lng, nameEn: str(f, "nameEn", 60) || null, active: true }, create: { name, lat, lng, nameEn: str(f, "nameEn", 60) || null } });
  await audit(s, "district.save", "district", name);
  revalidatePath("/admin/content/lookups");
}

export async function toggleDistrict(id: string) {
  const s = await requireAdmin("content.manage");
  const d = await db.district.findUnique({ where: { id } });
  if (!d) return;
  await db.district.update({ where: { id }, data: { active: !d.active } });
  await audit(s, "district.toggle", "district", id, { active: !d.active });
  revalidatePath("/admin/content/lookups");
}

export async function toggleType(key: string) {
  const s = await requireAdmin("content.manage");
  const t = await db.propertyType.findUnique({ where: { key } });
  if (!t) return;
  await db.propertyType.update({ where: { key }, data: { active: !t.active } });
  await audit(s, "type.toggle", "type", key, { active: !t.active });
  revalidatePath("/admin/content/lookups");
}
