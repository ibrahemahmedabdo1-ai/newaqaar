"use server";

import { revalidatePath } from "next/cache";
import { audit } from "@/lib/audit";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { floorsOf, j } from "@/lib/json";
import { gateway } from "@/lib/payments";

export async function adminRejectOffer(id: string) {
  const s = await requireAdmin("offers.write");
  const o = await db.offer.findUnique({ where: { id }, include: { payment: true } });
  if (!o || o.status === "REJECTED") return;
  if (o.payment?.status === "PAID") {
    await gateway().refund(o.payment.providerRef ?? "");
    await db.payment.update({ where: { id: o.payment.id }, data: { status: "REFUNDED", refundedAt: new Date(), note: "refund by admin" } });
  }
  await db.property.update({ where: { id: o.propertyId }, data: { hiddenUntil: null } });
  await db.offer.update({ where: { id }, data: { status: "REJECTED", decidedAt: new Date(), note: "rejected by admin" } });
  await audit(s, "offer.reject", "offer", id);
  revalidatePath("/admin/offers");
}

export async function adminAcceptOffer(id: string) {
  const s = await requireAdmin("offers.write");
  const o = await db.offer.findUnique({ where: { id }, include: { property: true } });
  if (!o || o.status !== "PENDING") return;
  const floors = floorsOf(o.property.floors).map((f) => (f.name === o.floor ? { ...f, state: "RESERVED" as const } : f));
  await db.property.update({ where: { id: o.propertyId }, data: { floors: j(floors) } });
  await db.offer.update({ where: { id }, data: { status: "ACTIVE", decidedAt: new Date(), note: "accepted by admin" } });
  await audit(s, "offer.accept", "offer", id);
  revalidatePath("/admin/offers");
}
