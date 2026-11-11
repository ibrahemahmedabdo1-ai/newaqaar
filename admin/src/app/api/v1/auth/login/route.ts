import { z } from "zod";
import bcrypt from "bcryptjs";
import { db } from "@/lib/db";
import { signUserToken } from "@/lib/api-auth";
import { userDto } from "@/lib/dto";
import { rateLimit } from "@/lib/ratelimit";
import { ApiError, body, json, route } from "@/lib/http";
import { phoneSchema } from "@/lib/schemas";

const schema = z.object({ phone: phoneSchema, password: z.string().min(1).max(100) });
const DUMMY = bcrypt.hashSync("not-a-real-password", 10);

export const POST = route(async (req: Request) => {
  await rateLimit(req, "login", 15, 600);
  const { phone, password } = await body(req, schema);
  const user = await db.user.findUnique({ where: { phone } });
  if (user?.lockedUntil && user.lockedUntil > new Date()) throw new ApiError(429, "LOCKED", "محاولات كثيرة، حاول بعد 15 دقيقة");
  const ok = await bcrypt.compare(password, user?.passwordHash ?? DUMMY);
  if (!user || !ok) {
    if (user) {
      const failed = user.failedLogins + 1;
      await db.user.update({ where: { id: user.id }, data: { failedLogins: failed, lockedUntil: failed >= 8 ? new Date(Date.now() + 15 * 60_000) : null } });
    }
    throw new ApiError(401, "BAD_CREDENTIALS", "خطأ في رقم الهاتف أو كلمة المرور");
  }
  if (user.status !== "ACTIVE") throw new ApiError(403, "ACCOUNT_" + user.status, "تم إيقاف حسابك، تواصل مع الدعم");
  await db.user.update({ where: { id: user.id }, data: { failedLogins: 0, lockedUntil: null, lastLoginAt: new Date() } });
  return json({ token: await signUserToken(user), user: userDto(user) });
});
