"use server";

import { Role, UserStatus } from "@prisma/client";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { audit } from "@/lib/audit";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { hashPassword, passwordError } from "@/lib/password";

export async function setUserStatus(id: string, formData: FormData) {
  const s = await requireAdmin("users.write");
  const status = String(formData.get("status"));
  if (!(Object.values(UserStatus) as string[]).includes(status)) return;
  const note = String(formData.get("note") ?? "").slice(0, 200);
  await db.user.update({ where: { id }, data: { status: status as UserStatus, statusNote: note || null, tokenVersion: { increment: 1 } } });
  await audit(s, "user.status", "user", id, { status, note });
  revalidatePath(`/admin/users/${id}`);
}

export async function setUserRole(id: string, formData: FormData) {
  const s = await requireAdmin("users.write");
  const role = String(formData.get("role"));
  if (!(Object.values(Role) as string[]).includes(role)) return;
  await db.user.update({ where: { id }, data: { role: role as Role } });
  await audit(s, "user.role", "user", id, { role });
  revalidatePath(`/admin/users/${id}`);
}

export async function toggleSubscription(id: string) {
  const s = await requireAdmin("users.write");
  const u = await db.user.findUnique({ where: { id }, select: { subscribed: true } });
  if (!u) return;
  await db.user.update({ where: { id }, data: { subscribed: !u.subscribed } });
  await audit(s, "user.subscription", "user", id, { subscribed: !u.subscribed });
  revalidatePath(`/admin/users/${id}`);
}

export async function resetUserPassword(id: string, formData: FormData) {
  const s = await requireAdmin("users.write");
  const pw = String(formData.get("password") ?? "");
  if (passwordError(pw)) redirect(`/admin/users/${id}?err=weak`);
  await db.user.update({ where: { id }, data: { passwordHash: await hashPassword(pw), tokenVersion: { increment: 1 }, failedLogins: 0, lockedUntil: null } });
  await audit(s, "user.password_reset", "user", id);
  redirect(`/admin/users/${id}?ok=pw`);
}

export async function forceLogout(id: string) {
  const s = await requireAdmin("users.write");
  await db.user.update({ where: { id }, data: { tokenVersion: { increment: 1 } } });
  await audit(s, "user.force_logout", "user", id);
  revalidatePath(`/admin/users/${id}`);
}

export async function deleteUser(id: string) {
  const s = await requireAdmin("users.write");
  const u = await db.user.findUnique({ where: { id }, select: { phone: true } });
  if (!u) return;
  await db.payment.updateMany({ where: { userId: id }, data: { userPhone: "deleted-by-admin" } });
  await db.user.delete({ where: { id } });
  await audit(s, "user.delete", "user", id, { phone: u.phone });
  redirect("/admin/users");
}
