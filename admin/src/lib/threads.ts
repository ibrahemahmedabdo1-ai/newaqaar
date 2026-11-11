import type { Prisma } from "@prisma/client";

export const threadInclude = {
  a: { select: { name: true, phone: true } },
  b: { select: { name: true, phone: true } },
  messages: { orderBy: { createdAt: "desc" as const }, take: 100 },
} satisfies Prisma.ThreadInclude;

export type ThreadFull = Prisma.ThreadGetPayload<{ include: typeof threadInclude }>;

export function threadDto(t: ThreadFull, viewerId: string, viewerPhone: string) {
  const iAmA = t.aId === viewerId;
  return {
    id: t.id, owner: viewerPhone, peer: iAmA ? t.b?.name ?? t.peerName : t.a.name,
    propertyId: t.propertyId ?? "",
    unread: t.messages.some((m) => m.senderId !== viewerId && !m.readAt && !m.deleted),
    blocked: t.blockedBy === viewerId, deleted: false,
  };
}
