import { z } from "zod";
import { db } from "@/lib/db";
import { requireUser } from "@/lib/api-auth";
import { rateLimit } from "@/lib/ratelimit";
import { body, json, route } from "@/lib/http";

export const POST = route(async (req: Request) => {
  await rateLimit(req, "support", 10, 3600);
  const u = await requireUser(req);
  const b = await body(req, z.object({ subject: z.string().trim().min(2).max(100), body: z.string().trim().min(2).max(2000) }));
  const t = await db.supportTicket.create({ data: { userId: u.id, contact: u.phone, subject: b.subject, body: b.body } });
  return json({ id: t.id }, 201);
});
