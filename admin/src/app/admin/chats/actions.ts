"use server";

import { revalidatePath } from "next/cache";
import { audit } from "@/lib/audit";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";

export async function setThreadLock(id: string, locked: boolean) {
  const s = await requireAdmin("chats.moderate");
  await db.thread.update({ where: { id }, data: { locked } });
  await audit(s, locked ? "chat.lock" : "chat.unlock", "thread", id);
  revalidatePath(`/admin/chats/${id}`);
}

export async function adminDeleteMessage(id: string, threadId: string) {
  const s = await requireAdmin("chats.moderate");
  await db.message.update({ where: { id }, data: { deleted: true, text: "", fileUrl: "" } });
  await audit(s, "chat.delete_message", "message", id);
  revalidatePath(`/admin/chats/${threadId}`);
}

export async function toggleFlag(id: string, threadId: string) {
  const s = await requireAdmin("chats.moderate");
  const m = await db.message.findUnique({ where: { id }, select: { flagged: true } });
  if (!m) return;
  await db.message.update({ where: { id }, data: { flagged: !m.flagged } });
  await audit(s, "chat.flag", "message", id, { flagged: !m.flagged });
  revalidatePath(`/admin/chats/${threadId}`);
}
