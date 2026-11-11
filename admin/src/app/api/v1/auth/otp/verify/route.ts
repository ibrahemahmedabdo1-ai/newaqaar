import { z } from "zod";
import { rateLimit } from "@/lib/ratelimit";
import { body, json, route } from "@/lib/http";
import { verifyOtp } from "@/lib/otp";
import { phoneSchema } from "@/lib/schemas";

const schema = z.object({ phone: phoneSchema, purpose: z.enum(["register", "reset", "change_phone"]), code: z.string().regex(/^[0-9]{4}$/) });

export const POST = route(async (req: Request) => {
  await rateLimit(req, "otp_verify", 30, 600);
  const { phone, purpose, code } = await body(req, schema);
  return json({ otpToken: await verifyOtp(phone, purpose, code) });
});
