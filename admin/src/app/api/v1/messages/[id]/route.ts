import { z } from "zod";
import { db } from "@/lib/db";
import { requireUser } from "@/lib/api-auth";
import { messageDto } from "@/lib/dto";
import { ApiError, body, json, route } from "@/lib/http";

type Ctx = { params: Promise<{ id: string }> };

async function mine(id: string, userId: string) {
  const m = await db.message.findUnique({ where: { id }, include: { thread: true } });
  if (!m || m.senderId !== userId) throw new ApiError(404, "NOT_FOUND");
  if (m.thread.locked || m.thread.blockedBy) throw new ApiError(403, "THREAD_LOCKED");
  return m;
}

export const PATCH = route(async (req: Request, ctx: Ctx) => {
  const { id } = await ctx.params;
  const u = await requireUser(req);
  const b = await body(req, z.object({ text: z.string().trim().min(1).max(1000) }));
  const m = await mine(id, u.id);
  if (m.deleted) throw new ApiError(409, "DELETED");
  return json(messageDto(await db.message.update({ where: { id }, data: { text: b.text, edited: true } }), u.id));
});

export const DELETE = route(async (req: Request, ctx: Ctx) => {
  const { id } = await ctx.params;
  const u = await requireUser(req);
  await mine(id, u.id);
  await db.message.update({ where: { id }, data: { deleted: true, text: "", fileUrl: "" } });
  return json({ ok: true });
});
