import { z } from "zod";
import { db } from "@/lib/db";
import { checkOtpToken, signUserToken } from "@/lib/api-auth";
import { userDto } from "@/lib/dto";
import { rateLimit } from "@/lib/ratelimit";
import { ApiError, body, json, route } from "@/lib/http";
import { hashPassword, passwordError } from "@/lib/password";
import { phoneSchema, roleSchema } from "@/lib/schemas";
import { getSettings } from "@/lib/settings";

const schema = z.object({
  phone: phoneSchema, name: z.string().trim().min(2).max(60), role: roleSchema,
  password: z.string().min(8).max(100), otpToken: z.string().min(10),
});

export const POST = route(async (req: Request) => {
  await rateLimit(req, "register", 10, 3600);
  const b = await body(req, schema);
  if (!(await getSettings()).allowRegistration) throw new ApiError(403, "REGISTRATION_CLOSED", "التسجيل متوقف مؤقتًا");
  await checkOtpToken(b.otpToken, b.phone, "register");
  if (passwordError(b.password)) throw new ApiError(400, "WEAK_PASSWORD", "٨ أحرف على الأقل وتحتوي على حروف وأرقام");
  if (await db.user.findUnique({ where: { phone: b.phone } })) throw new ApiError(409, "PHONE_EXISTS", "هذا الرقم لديه حساب بالفعل");
  const user = await db.user.create({
    data: { phone: b.phone, name: b.name, role: b.role, passwordHash: await hashPassword(b.password), lastLoginAt: new Date() },
  });
  return json({ token: await signUserToken(user), user: userDto(user) }, 201);
});
