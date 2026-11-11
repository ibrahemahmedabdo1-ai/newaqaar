import { z } from "zod";
import { db } from "@/lib/db";
import { requireUser } from "@/lib/api-auth";
import { ApiError, body, json, route } from "@/lib/http";
import { threadDto, threadInclude } from "@/lib/threads";

const schema = z.object({ id: z.string().min(6).max(40).optional(), propertyId: z.string().optional(), peerName: z.string().max(80).optional() });

export const POST = route(async (req: Request) => {
  const u = await requireUser(req);
  const b = await body(req, schema);
  let bId: string | null = null;
  let peerName = b.peerName ?? "";
  if (b.propertyId) {
    const p = await db.property.findUnique({ where: { id: b.propertyId }, include: { owner: { select: { name: true } } } });
    if (!p) throw new ApiError(404, "NOT_FOUND");
    if (p.ownerId === u.id) throw new ApiError(400, "OWN_PROPERTY");
    bId = p.ownerId;
    peerName = p.owner.name;
  }
  let t = await db.thread.findFirst({
    where: { aId: u.id, bId, propertyId: b.propertyId ?? null, NOT: { deletedBy: { has: u.id } } }, include: threadInclude,
  });
  if (!t) {
    if (b.id && (await db.thread.findUnique({ where: { id: b.id } }))) throw new ApiError(409, "ID_TAKEN");
    t = await db.thread.create({
      data: { id: b.id, aId: u.id, bId, peerName: peerName || "Bawabat Alorood", propertyId: b.propertyId ?? null }, include: threadInclude,
    });
  }
  return json(threadDto(t, u.id, u.phone), 201);
});
