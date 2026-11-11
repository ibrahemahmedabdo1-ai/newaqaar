import { z } from "zod";

export const phoneSchema = z.string().regex(/^\+[1-9][0-9]{7,14}$/, "رقم هاتف غير صالح (مثال +966557545350)");
export const roleSchema = z.enum(["CLIENT", "BROKER", "DEVELOPER"]);
const httpsUrl = z.string().url().refine((u) => u.startsWith("https://"), "https only");

export const floorSchema = z.object({ name: z.string().min(1).max(40), state: z.enum(["AVAILABLE", "RESERVED", "SOLD"]) });

export const propertySchema = z.object({
  id: z.string().min(6).max(40).optional(),
  kind: z.enum(["apt", "annex", "villa", "duplex", "office", "townhouse"]),
  rooms: z.number().int().min(0).max(30),
  baths: z.number().int().min(0).max(30),
  area: z.number().int().min(10).max(100000),
  price: z.number().int().min(1).max(2_000_000_000),
  minOffer: z.number().int().min(1).max(2_000_000_000),
  commission: z.number().int().min(0).max(2_000_000_000),
  district: z.string().min(2).max(60),
  city: z.string().min(2).max(60).default("جدة"),
  description: z.string().min(5).max(2000),
  facade: z.enum(["front", "back"]).default("front"),
  direction: z.string().max(30).default(""),
  view: z.enum(["city", "garden", "sea"]).default("city"),
  maid: z.boolean().default(false),
  driver: z.boolean().default(false),
  parking: z.boolean().default(true),
  floors: z.array(floorSchema).min(1).max(60),
  guarantees: z.array(z.string().max(120)).max(10).default([]),
  amenities: z.array(z.string().max(60)).max(20).default([]),
  infra: z.array(z.string().max(60)).max(20).default([]),
  photos: z.array(httpsUrl).min(1).max(10),
  lat: z.number().min(-90).max(90).default(21.54),
  lng: z.number().min(-180).max(180).default(39.17),
});
