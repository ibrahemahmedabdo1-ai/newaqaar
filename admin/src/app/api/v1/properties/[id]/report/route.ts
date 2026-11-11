import { z } from "zod";
import { db } from "@/lib/db";
import { requireUser } from "@/lib/api-auth";
import { ApiError, body, json, route } from "@/lib/http";

export const POST = route(async (req: Request, ctx: { params: Promise<{ id: string }> }) => {
  const { id } = await ctx.params;
  const u = await requireUser(req);
  const b = await body(req, z.object({ reason: z.string().trim().min(2).max(100), details: z.string().max(500).optional() }));
  if (!(await db.property.findUnique({ where: { id }, select: { id: true } }))) throw new ApiError(404, "NOT_FOUND");
  const open = await db.report.count({ where: { propertyId: id, reporterId: u.id, status: "OPEN" } });
  if (open === 0) await db.report.create({ data: { propertyId: id, reporterId: u.id, reason: b.reason, details: b.details } });
  return json({ ok: true });
});
