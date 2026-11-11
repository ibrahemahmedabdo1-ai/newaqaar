"use server";

import { revalidatePath } from "next/cache";
import { audit } from "@/lib/audit";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";

export async function handleReport(id: string, formData: FormData) {
  const s = await requireAdmin("reports.manage");
  const action = String(formData.get("action") ?? "");
  const note = String(formData.get("note") ?? "").trim().slice(0, 300);
  const r = await db.report.findUnique({ where: { id }, include: { property: { select: { id: true, ownerId: true } }, thread: { select: { id: true } } } });
  if (!r || r.status !== "OPEN") return;

  let status: "RESOLVED" | "DISMISSED" = "RESOLVED";
  if (action === "dismiss") status = "DISMISSED";
  if (action === "remove_ad" && r.property) {
    await db.property.update({ where: { id: r.property.id }, data: { status: "REJECTED", rejectReason: note || "تمت إزالة الإعلان بعد بلاغ" } });
  }
  if (action === "suspend_owner" && r.property) {
    await db.user.update({ where: { id: r.property.ownerId }, data: { status: "SUSPENDED", statusNote: note || "بعد بلاغ", tokenVersion: { increment: 1 } } });
  }
  if (action === "lock_thread" && r.thread) {
    await db.thread.update({ where: { id: r.thread.id }, data: { locked: true } });
  }
  await db.report.update({ where: { id }, data: { status, resolution: `${action}${note ? ": " + note : ""}`, resolvedBy: s.name, resolvedAt: new Date() } });
  await audit(s, "report." + action, "report", id, { note });
  revalidatePath("/admin/reports");
}
