"use server";

import { revalidatePath } from "next/cache";
import { audit } from "@/lib/audit";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { gateway } from "@/lib/payments";

export async function refundPayment(id: string) {
  const s = await requireAdmin("payments.write");
  const p = await db.payment.findUnique({ where: { id } });
  if (!p || p.status !== "PAID") return;
  await gateway().refund(p.providerRef ?? "");
  await db.payment.update({ where: { id }, data: { status: "REFUNDED", refundedAt: new Date(), note: "refund by admin" } });
  await audit(s, "payment.refund", "payment", id, { total: p.total });
  revalidatePath("/admin/payments");
}
