import type { Prisma } from "@prisma/client";

export const j = (v: unknown) => v as Prisma.InputJsonValue;
export type FloorJson = { name: string; state: "AVAILABLE" | "RESERVED" | "SOLD" };
export const floorsOf = (v: unknown): FloorJson[] => (Array.isArray(v) ? (v as FloorJson[]) : []);
