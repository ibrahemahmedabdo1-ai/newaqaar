import { z } from "zod";
import { db } from "@/lib/db";
import { requireUser } from "@/lib/api-auth";
import { messageDto } from "@/lib/dto";
import { ApiError, body, json, route } from "@/lib/http";

const schema = z.object({
  id: z.string().min(6).max(40).optional(),
  text: z.string().max(1000).default(""),
  fileUrl: z.string().max(400).default(""),
});

export const POST = route(async (req: Request, ctx: { params: Promise<{ id: string }> }) => {
  const { id } = await ctx.params;
  const u = await requireUser(req);
  const b = await body(req, schema);
  if (!b.text.trim() && !b.fileUrl) throw new ApiError(400, "EMPTY_MESSAGE");
  const t = await db.thread.findUnique({ where: { id } });
  if (!t || (t.aId !== u.id && t.bId !== u.id)) throw new ApiError(404, "NOT_FOUND");
  if (t.locked) throw new ApiError(403, "THREAD_LOCKED", "تم إيقاف هذه المحادثة من الإدارة");
  if (t.blockedBy) throw new ApiError(403, "BLOCKED", "لا يمكنك إرسال أو استقبال الرسائل");
  if (b.id) {
    const ex = await db.message.findUnique({ where: { id: b.id } });
    if (ex) return json(messageDto(ex, u.id));
  }
  const m = await db.message.create({ data: { id: b.id, threadId: id, senderId: u.id, text: b.text.trim(), fileUrl: b.fileUrl } });
  // إن كان الطرف الآخر قد حذف المحادثة من عنده تعاد للظهور
  await db.thread.update({ where: { id }, data: { deletedBy: { set: t.deletedBy.filter((x) => x === u.id) } } });
  return json(messageDto(m, u.id), 201);
});
