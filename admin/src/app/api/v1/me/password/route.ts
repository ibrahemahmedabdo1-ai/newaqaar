import { z } from "zod";
import { db } from "@/lib/db";
import { requireUser, signUserToken } from "@/lib/api-auth";
import { ApiError, body, json, route } from "@/lib/http";
import { hashPassword, passwordError, verifyPassword } from "@/lib/password";

export const POST = route(async (req: Request) => {
  const u = await requireUser(req);
  const b = await body(req, z.object({ current: z.string().min(1), next: z.string().min(8).max(100) }));
  if (!(await verifyPassword(b.current, u.passwordHash))) throw new ApiError(403, "BAD_PASSWORD", "كلمة المرور الحالية غير صحيحة");
  if (passwordError(b.next)) throw new ApiError(400, "WEAK_PASSWORD");
  const updated = await db.user.update({ where: { id: u.id }, data: { passwordHash: await hashPassword(b.next), tokenVersion: { increment: 1 } } });
  return json({ ok: true, token: await signUserToken(updated) });
});
