"use server";

import { revalidatePath } from "next/cache";
import { audit } from "@/lib/audit";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";

export async function createAnnouncement(f: FormData) {
  const s = await requireAdmin("announce.manage");
  const str = (k: string, m = 1000) => String(f.get(k) ?? "").trim().slice(0, m);
  const titleAr = str("titleAr", 120);
  const bodyAr = str("bodyAr");
  if (!titleAr || !bodyAr) return;
  const audience = ["ALL", "CLIENT", "BROKER", "DEVELOPER"].includes(str("audience")) ? str("audience") : "ALL";
  const level = ["INFO", "WARN"].includes(str("level")) ? str("level") : "INFO";
  const ends = str("endsAt");
  const a = await db.announcement.create({
    data: { titleAr, titleEn: str("titleEn", 120) || titleAr, bodyAr, bodyEn: str("bodyEn") || bodyAr, audience, level, endsAt: ends ? new Date(ends) : null },
  });
  await audit(s, "announcement.create", "announcement", a.id);
  revalidatePath("/admin/announcements");
}

export async function toggleAnnouncement(id: string) {
  const s = await requireAdmin("announce.manage");
  const a = await db.announcement.findUnique({ where: { id } });
  if (!a) return;
  await db.announcement.update({ where: { id }, data: { active: !a.active } });
  await audit(s, "announcement.toggle", "announcement", id, { active: !a.active });
  revalidatePath("/admin/announcements");
}

export async function deleteAnnouncement(id: string) {
  const s = await requireAdmin("announce.manage");
  await db.announcement.delete({ where: { id } });
  await audit(s, "announcement.delete", "announcement", id);
  revalidatePath("/admin/announcements");
}
