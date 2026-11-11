import { SignJWT, jwtVerify } from "jose";
import type { User } from "@prisma/client";
import { db } from "./db";
import { secret } from "./env";
import { ApiError } from "./http";

export async function signUserToken(user: { id: string; tokenVersion: number }): Promise<string> {
  return new SignJWT({ tv: user.tokenVersion, typ: "user" })
    .setProtectedHeader({ alg: "HS256" })
    .setSubject(user.id)
    .setIssuedAt()
    .setExpirationTime("30d")
    .sign(secret("API_JWT_SECRET"));
}

export async function signOtpToken(phone: string, purpose: string): Promise<string> {
  return new SignJWT({ phone, purpose, typ: "otp" })
    .setProtectedHeader({ alg: "HS256" })
    .setIssuedAt()
    .setExpirationTime("15m")
    .sign(secret("API_JWT_SECRET"));
}

export async function checkOtpToken(token: string, phone: string, purpose: string): Promise<void> {
  try {
    const { payload } = await jwtVerify(token, secret("API_JWT_SECRET"));
    if (payload.typ !== "otp" || payload.phone !== phone || payload.purpose !== purpose) throw new Error("mismatch");
  } catch {
    throw new ApiError(401, "OTP_TOKEN_INVALID", "انتهت صلاحية رمز التأكيد، أعد المحاولة");
  }
}

export async function authUser(req: Request): Promise<User | null> {
  const h = req.headers.get("authorization");
  if (!h?.startsWith("Bearer ")) return null;
  try {
    const { payload } = await jwtVerify(h.slice(7), secret("API_JWT_SECRET"));
    if (payload.typ !== "user") return null;
    const user = await db.user.findUnique({ where: { id: String(payload.sub) } });
    if (!user || user.tokenVersion !== payload.tv) return null;
    return user;
  } catch {
    return null;
  }
}

export async function requireUser(req: Request): Promise<User> {
  const u = await authUser(req);
  if (!u) throw new ApiError(401, "UNAUTHORIZED");
  if (u.status !== "ACTIVE") throw new ApiError(403, "ACCOUNT_" + u.status, "تم إيقاف حسابك، تواصل مع الدعم");
  return u;
}
