import bcrypt from "bcryptjs";
import { SignJWT, jwtVerify } from "jose";
import { cookies, headers } from "next/headers";
import { redirect } from "next/navigation";
import type { AdminRole } from "@prisma/client";
import { db } from "./db";
import { isProd, secret } from "./env";
import { can, type Perm } from "./perms";

const COOKIE = "bw_admin";
const MAX_AGE = 8 * 3600;

export type Session = { id: string; email: string; name: string; role: AdminRole };

export async function createSession(admin: { id: string; tokenVersion: number }) {
  const token = await new SignJWT({ tv: admin.tokenVersion })
    .setProtectedHeader({ alg: "HS256" })
    .setSubject(admin.id)
    .setIssuedAt()
    .setExpirationTime(`${MAX_AGE}s`)
    .sign(secret("ADMIN_JWT_SECRET"));
  const c = await cookies();
  c.set(COOKIE, token, { httpOnly: true, secure: isProd, sameSite: "lax", path: "/", maxAge: MAX_AGE });
}

export async function destroySession() {
  (await cookies()).delete(COOKIE);
}

export async function getSession(): Promise<Session | null> {
  const token = (await cookies()).get(COOKIE)?.value;
  if (!token) return null;
  try {
    const { payload } = await jwtVerify(token, secret("ADMIN_JWT_SECRET"));
    const admin = await db.adminUser.findUnique({ where: { id: String(payload.sub) } });
    if (!admin || !admin.active || admin.tokenVersion !== payload.tv) return null;
    return { id: admin.id, email: admin.email, name: admin.name, role: admin.role };
  } catch {
    return null;
  }
}

/** يتحقق من الجلسة والصلاحية داخل كل صفحة وكل Server Action (لا نعتمد على middleware فقط). */
export async function requireAdmin(perm?: Perm): Promise<Session> {
  const s = await getSession();
  if (!s) redirect("/login");
  if (perm && !can(s.role, perm)) redirect("/admin?denied=1");
  return s;
}

const DUMMY_HASH = bcrypt.hashSync("not-a-real-password", 10);

export async function loginAdmin(email: string, password: string): Promise<{ ok: true } | { ok: false; error: string }> {
  const admin = await db.adminUser.findUnique({ where: { email: email.trim().toLowerCase() } });
  if (admin?.lockedUntil && admin.lockedUntil > new Date()) {
    return { ok: false, error: "الحساب مقفل مؤقتًا بسبب محاولات فاشلة. حاول بعد قليل." };
  }
  const valid = await bcrypt.compare(password, admin?.passwordHash ?? DUMMY_HASH);
  if (!admin || !admin.active || !valid) {
    if (admin) {
      const failed = admin.failedLogins + 1;
      await db.adminUser.update({
        where: { id: admin.id },
        data: { failedLogins: failed, lockedUntil: failed >= 5 ? new Date(Date.now() + 15 * 60_000) : null },
      });
    }
    return { ok: false, error: "بيانات الدخول غير صحيحة" };
  }
  await db.adminUser.update({ where: { id: admin.id }, data: { failedLogins: 0, lockedUntil: null, lastLoginAt: new Date() } });
  await createSession(admin);
  await db.auditLog.create({
    data: { adminId: admin.id, adminName: admin.name, action: "login", entity: "admin", entityId: admin.id, ip: await clientIp() },
  });
  return { ok: true };
}

export async function clientIp(): Promise<string | null> {
  const h = await headers();
  return h.get("x-forwarded-for")?.split(",")[0]?.trim() ?? h.get("x-real-ip") ?? null;
}
