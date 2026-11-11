import type { Prisma } from "@prisma/client";

export type PropWithOwner = Prisma.PropertyGetPayload<{ include: { owner: { select: { name: true; phone: true } } } }>;
export const propInclude = { owner: { select: { name: true, phone: true } } } satisfies Prisma.PropertyInclude;

export const ms = (d?: Date | null) => (d ? d.getTime() : 0);

export function seedOf(id: string): number {
  let h = 0;
  for (let i = 0; i < id.length; i++) h = (h * 31 + id.charCodeAt(i)) | 0;
  return Math.abs(h) % 50;
}

export function propertyDto(p: PropWithOwner, viewerId?: string) {
  const mine = viewerId === p.ownerId;
  return {
    id: p.id, kind: p.kind, rooms: p.rooms, baths: p.baths, area: p.area, price: p.price,
    minOffer: p.minOffer, commission: p.commission, district: p.district, city: p.city,
    description: p.description, facade: p.facade, direction: p.direction, view: p.view,
    maid: p.maid, driver: p.driver, parking: p.parking,
    floors: p.floors, guarantees: p.guarantees, amenities: p.amenities, infra: p.infra,
    publisher: p.owner.name, ownerPhone: mine ? p.owner.phone : "", contactPhone: p.owner.phone,
    lat: p.lat, lng: p.lng, seed: seedOf(p.id), photos: p.photos,
    status: p.status, rejectReason: p.rejectReason ?? "",
    createdAt: ms(p.createdAt), publishedAt: ms(p.publishedAt), soldTo: mine ? p.soldTo ?? "" : "",
  };
}

export function userDto(u: { phone: string; name: string; role: string; subscribed: boolean; avatarUrl: string | null }) {
  return { phone: u.phone, name: u.name, role: u.role, salt: "", pwHash: "", subscribed: u.subscribed, avatar: u.avatarUrl ?? "" };
}

export function offerDto(o: { id: string; propertyId: string; floor: string; amount: number; clientName: string; clientPhone: string; status: string; createdAt: Date; byUser: { phone: string } }) {
  return {
    id: o.id, propertyId: o.propertyId, floor: o.floor, amount: o.amount, clientName: o.clientName,
    clientPhone: o.clientPhone, byPhone: o.byUser.phone, status: o.status, createdAt: ms(o.createdAt),
  };
}

export function messageDto(m: { id: string; threadId: string; senderId: string; text: string; fileUrl: string; edited: boolean; deleted: boolean; createdAt: Date }, viewerId: string) {
  return {
    id: m.id, threadId: m.threadId, text: m.deleted ? "" : m.text, mine: m.senderId === viewerId,
    time: ms(m.createdAt), file: m.deleted ? "" : m.fileUrl, edited: m.edited, deleted: m.deleted,
  };
}
