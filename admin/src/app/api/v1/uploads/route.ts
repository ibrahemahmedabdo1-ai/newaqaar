import { requireUser } from "@/lib/api-auth";
import { rateLimit } from "@/lib/ratelimit";
import { ApiError, json, route } from "@/lib/http";
import { uploadImage } from "@/lib/storage";

const FOLDERS = ["ads", "avatars", "chat"];

/** رفع صورة: POST الجسم هو بايتات الصورة، و Content-Type = image/jpeg|png|webp، و ?folder=ads|avatars|chat */
export const POST = route(async (req: Request) => {
  await rateLimit(req, "upload", 80, 3600);
  await requireUser(req);
  const folder = new URL(req.url).searchParams.get("folder") ?? "ads";
  if (!FOLDERS.includes(folder)) throw new ApiError(400, "BAD_FOLDER");
  const ct = (req.headers.get("content-type") ?? "").split(";")[0].trim();
  const url = await uploadImage(await req.arrayBuffer(), ct, folder);
  return json({ url }, 201);
});
