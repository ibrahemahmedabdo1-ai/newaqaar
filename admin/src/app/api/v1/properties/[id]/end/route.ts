import { z } from "zod";
import { db } from "@/lib/db";
import { requireUser } from "@/lib/api-auth";
import { ApiError, body, json, route } from "@/lib/http";

export const POST = route(async (req: Request, ctx: { params: Promise<{ id: string }> }) => {
  const { id } = await ctx.params;
  const u = await requireUser(req);
  const b = await body(req, z.object({ soldTo: z.string().regex(/^[0-9+]{7,16}$/).optional() }));
  const p = await db.property.findUnique({ where: { id } });
  if (!p) throw new ApiError(404, "NOT_FOUND");
  if (p.ownerId !== u.id) throw new ApiError(403, "FORBIDDEN");
  await db.property.update({ where: { id }, data: { status: "ENDED", endedAt: new Date(), soldTo: b.soldTo ?? null } });
  return json({ ok: true });
});
