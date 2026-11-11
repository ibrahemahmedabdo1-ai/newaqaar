"use server";

import { TicketStatus } from "@prisma/client";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { audit } from "@/lib/audit";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";

export async function replyTicket(id: string, formData: FormData) {
  const s = await requireAdmin("tickets.manage");
  const reply = String(formData.get("reply") ?? "").trim().slice(0, 3000);
  const status = String(formData.get("status") ?? "IN_PROGRESS");
  if (!(Object.values(TicketStatus) as string[]).includes(status)) return;
  await db.supportTicket.update({
    where: { id },
    data: { status: status as TicketStatus, ...(reply ? { reply, repliedBy: s.name, repliedAt: new Date() } : {}) },
  });
  await audit(s, "ticket.reply", "ticket", id, { status });
  revalidatePath("/admin/tickets");
  redirect(`/admin/tickets/${id}?ok=1`);
}
