"use server";

import { AdminRole } from "@prisma/client";
import bcrypt from "bcryptjs";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { audit } from "@/lib/audit";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";

const roles = Object.values(AdminRole) as string[];

async function superCountExcept(id: string) {
  return db.adminUser.count({ where: { role: "SUPER_ADMIN", active: true, NOT: { id } } });
}

export async function createAdmin(f: FormData) {
  const s = await requireAdmin("team.manage");
  const email = String(f.get("email") ?? "").trim().toLowerCase();
  const name = String(f.get("name") ?? "").trim().slice(0, 80);
  const role = String(f.get("role") ?? "");
  const password = String(f.get("password") ?? "");
  if (!/^\S+@\S+\.\S+$/.test(email) || !name || !roles.includes(role) || password.length < 12) redirect("/admin/team?err=invalid");
  if (await db.adminUser.findUnique({ where: { email } })) redirect("/admin/team?err=exists");
  const a = await db.adminUser.create({ data: { email, name, role: role as AdminRole, passwordHash: await bcrypt.hash(password, 12) } });
  await audit(s, "admin.create", "admin", a.id, { email, role });
  revalidatePath("/admin/team");
}

export async function setAdminRole(id: string, f: FormData) {
  const s = await requireAdmin("team.manage");
  const role = String(f.get("role") ?? "");
  if (!roles.includes(role)) return;
  if (id === s.id && role !== "SUPER_ADMIN") redirect("/admin/team?err=self");
  if (role !== "SUPER_ADMIN" && (await superCountExcept(id)) === 0) redirect("/admin/team?err=last");
  await db.adminUser.update({ where: { id }, data: { role: role as AdminRole, tokenVersion: { increment: 1 } } });
  await audit(s, "admin.role", "admin", id, { role });
  revalidatePath("/admin/team");
}

export async function toggleAdmin(id: string) {
  const s = await requireAdmin("team.manage");
  if (id === s.id) redirect("/admin/team?err=self");
  const a = await db.adminUser.findUnique({ where: { id } });
  if (!a) return;
  if (a.active && a.role === "SUPER_ADMIN" && (await superCountExcept(id)) === 0) redirect("/admin/team?err=last");
  await db.adminUser.update({ where: { id }, data: { active: !a.active, tokenVersion: { increment: 1 } } });
  await audit(s, "admin.toggle", "admin", id, { active: !a.active });
  revalidatePath("/admin/team");
}

export async function resetAdminPassword(id: string, f: FormData) {
  const s = await requireAdmin("team.manage");
  const password = String(f.get("password") ?? "");
  if (password.length < 12) redirect("/admin/team?err=invalid");
  await db.adminUser.update({
    where: { id }, data: { passwordHash: await bcrypt.hash(password, 12), tokenVersion: { increment: 1 }, failedLogins: 0, lockedUntil: null },
  });
  await audit(s, "admin.password_reset", "admin", id);
  revalidatePath("/admin/team");
}
