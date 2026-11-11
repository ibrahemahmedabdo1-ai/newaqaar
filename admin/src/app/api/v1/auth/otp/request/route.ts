import { z } from "zod";
import { db } from "@/lib/db";
import { authUser } from "@/lib/api-auth";
import { rateLimit } from "@/lib/ratelimit";
import { ApiError, body, json, route } from "@/lib/http";
import { requestOtp } from "@/lib/otp";
import { phoneSchema } from "@/lib/schemas";
import { getSettings } from "@/lib/settings";

const schema = z.object({ phone: phoneSchema, purpose: z.enum(["register", "reset", "change_phone"]) });

export const POST = route(async (req: Request) => {
  await rateLimit(req, "otp_request", 10, 3600);
  const { phone, purpose } = await body(req, schema);
  const existing = await db.user.findUnique({ where: { phone } });
  if (purpose === "register") {
    if (!(await getSettings()).allowRegistration) throw new ApiError(403, "REGISTRATION_CLOSED", "التسجيل متوقف مؤقتًا");
    if (existing) throw new ApiError(409, "PHONE_EXISTS", "هذا الرقم لديه حساب بالفعل");
  }
  if (purpose === "reset" && !existing) throw new ApiError(404, "PHONE_NOT_FOUND", "هذا الرقم غير مسجل");
  if (purpose === "change_phone") {
    if (!(await authUser(req))) throw new ApiError(401, "UNAUTHORIZED");
    if (existing) throw new ApiError(409, "PHONE_EXISTS", "هذا الرقم لديه حساب بالفعل");
  }
  return json({ ok: true, ...(await requestOtp(phone, purpose)) });
});
