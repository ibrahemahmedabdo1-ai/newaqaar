import { db } from "@/lib/db";
import { requireUser } from "@/lib/api-auth";
import { ApiError, json, route } from "@/lib/http";

type Ctx = { params: Promise<{ id: string }> };

export const PUT = route(async (req: Request, ctx: Ctx) => {
  const { id } = await ctx.params;
  const u = await requireUser(req);
  if (!(await db.property.findUnique({ where: { id }, select: { id: true } }))) throw new ApiError(404, "NOT_FOUND");
  await db.favorite.upsert({ where: { userId_propertyId: { userId: u.id, propertyId: id } }, update: {}, create: { userId: u.id, propertyId: id } });
  return json({ ok: true });
});

export const DELETE = route(async (req: Request, ctx: Ctx) => {
  const { id } = await ctx.params;
  const u = await requireUser(req);
  await db.favorite.deleteMany({ where: { userId: u.id, propertyId: id } });
  return json({ ok: true });
});
