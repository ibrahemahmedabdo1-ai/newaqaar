import { z } from "zod";
import { db } from "@/lib/db";
import { checkOtpToken } from "@/lib/api-auth";
import { rateLimit } from "@/lib/ratelimit";
import { ApiError, body, json, route } from "@/lib/http";
import { hashPassword, passwordError } from "@/lib/password";
import { phoneSchema } from "@/lib/schemas";

const schema = z.object({ phone: phoneSchema, password: z.string().min(8).max(100), otpToken: z.string().min(10) });

export const POST = route(async (req: Request) => {
  await rateLimit(req, "reset", 10, 3600);
  const b = await body(req, schema);
  await checkOtpToken(b.otpToken, b.phone, "reset");
  if (passwordError(b.password)) throw new ApiError(400, "WEAK_PASSWORD");
  const user = await db.user.findUnique({ where: { phone: b.phone } });
  if (!user) throw new ApiError(404, "PHONE_NOT_FOUND");
  await db.user.update({
    where: { id: user.id },
    data: { passwordHash: await hashPassword(b.password), tokenVersion: { increment: 1 }, failedLogins: 0, lockedUntil: null },
  });
  return json({ ok: true });
});
