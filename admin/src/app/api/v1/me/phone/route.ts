import { z } from "zod";
import { db } from "@/lib/db";
import { checkOtpToken, requireUser } from "@/lib/api-auth";
import { userDto } from "@/lib/dto";
import { ApiError, body, json, route } from "@/lib/http";
import { phoneSchema } from "@/lib/schemas";

export const POST = route(async (req: Request) => {
  const u = await requireUser(req);
  const b = await body(req, z.object({ newPhone: phoneSchema, otpToken: z.string().min(10) }));
  await checkOtpToken(b.otpToken, b.newPhone, "change_phone");
  if (await db.user.findUnique({ where: { phone: b.newPhone } })) throw new ApiError(409, "PHONE_EXISTS", "هذا الرقم لديه حساب بالفعل");
  return json(userDto(await db.user.update({ where: { id: u.id }, data: { phone: b.newPhone } })));
});
