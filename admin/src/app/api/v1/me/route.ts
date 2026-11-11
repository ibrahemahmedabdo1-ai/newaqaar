import { z } from "zod";
import { createHash } from "crypto";
import { db } from "@/lib/db";
import { requireUser } from "@/lib/api-auth";
import { userDto } from "@/lib/dto";
import { ApiError, body, json, route } from "@/lib/http";
import { verifyPassword } from "@/lib/password";

export const GET = route(async (req: Request) => json(userDto(await requireUser(req))));

const patch = z.object({ name: z.string().trim().min(2).max(60).optional(), avatarUrl: z.string().url().startsWith("https://").nullable().optional() });

export const PATCH = route(async (req: Request) => {
  const u = await requireUser(req);
  const b = await body(req, patch);
  const updated = await db.user.update({ where: { id: u.id }, data: { name: b.name ?? undefined, avatarUrl: b.avatarUrl === undefined ? undefined : b.avatarUrl } });
  return json(userDto(updated));
});

/** حذف الحساب وبياناته نهائيًا. سجلات الدفع تُحتفظ بدون بيانات تعريفية مباشرة (التزام محاسبي). */
export const DELETE = route(async (req: Request) => {
  const u = await requireUser(req);
  const b = await body(req, z.object({ password: z.string().min(1) }));
  if (!(await verifyPassword(b.password, u.passwordHash))) throw new ApiError(403, "BAD_PASSWORD", "كلمة المرور غير صحيحة");
  const tag = "deleted:" + createHash("sha256").update(u.phone).digest("hex").slice(0, 10);
  await db.$transaction([
    db.payment.updateMany({ where: { userId: u.id }, data: { userPhone: tag } }),
    db.user.delete({ where: { id: u.id } }),
  ]);
  return json({ ok: true });
});
