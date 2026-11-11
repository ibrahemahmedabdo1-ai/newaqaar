import type { Prisma } from "@prisma/client";
import { db } from "./db";
import { clientIp, type Session } from "./auth";

export async function audit(s: Session, action: string, entity: string, entityId?: string, meta?: unknown) {
  await db.auditLog.create({
    data: {
      adminId: s.id, adminName: s.name, action, entity, entityId: entityId ?? null,
      meta: meta === undefined ? undefined : (JSON.parse(JSON.stringify(meta)) as Prisma.InputJsonValue),
      ip: await clientIp(),
    },
  });
}
