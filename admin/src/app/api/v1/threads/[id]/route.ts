import { z } from "zod";
import { db } from "@/lib/db";
import { requireUser } from "@/lib/api-auth";
import { ApiError, body, json, route } from "@/lib/http";

const schema = z.object({ blocked: z.boolean().optional(), deleted: z.boolean().optional(), read: z.boolean().optional() });

export const PATCH = route(async (req: Request, ctx: { params: Promise<{ id: string }> }) => {
  const { id } = await ctx.params;
  const u = await requireUser(req);
  const b = await body(req, schema);
  const t = await db.thread.findUnique({ where: { id } });
  if (!t || (t.aId !== u.id && t.bId !== u.id)) throw new ApiError(404, "NOT_FOUND");
  if (b.blocked !== undefined) {
    if (b.blocked && !t.blockedBy) await db.thread.update({ where: { id }, data: { blockedBy: u.id } });
    if (!b.blocked && t.blockedBy === u.id) await db.thread.update({ where: { id }, data: { blockedBy: null } });
  }
  if (b.deleted && !t.deletedBy.includes(u.id)) {
    await db.thread.update({ where: { id }, data: { deletedBy: { push: u.id } } });
  }
  if (b.read) {
    await db.message.updateMany({ where: { threadId: id, senderId: { not: u.id }, readAt: null }, data: { readAt: new Date() } });
  }
  return json({ ok: true });
});
