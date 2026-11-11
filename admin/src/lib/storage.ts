import { put } from "@vercel/blob";
import { randomUUID } from "crypto";
import { ApiError } from "./http";

const TYPES: Record<string, string> = { "image/jpeg": "jpg", "image/png": "png", "image/webp": "webp" };
const MAX = 6 * 1024 * 1024;

export async function uploadImage(bytes: ArrayBuffer, contentType: string, folder: string): Promise<string> {
  const ext = TYPES[contentType];
  if (!ext) throw new ApiError(415, "UNSUPPORTED_TYPE", "الصيغ المسموحة: JPG / PNG / WEBP");
  if (bytes.byteLength === 0 || bytes.byteLength > MAX) throw new ApiError(413, "FILE_TOO_LARGE", "الحد الأقصى 6MB");
  if (!process.env.BLOB_READ_WRITE_TOKEN) throw new ApiError(503, "STORAGE_NOT_CONFIGURED", "رفع الصور غير مفعّل على الخادم");
  const blob = await put(`${folder}/${randomUUID()}.${ext}`, Buffer.from(bytes), {
    access: "public", contentType, addRandomSuffix: false,
  });
  return blob.url;
}
