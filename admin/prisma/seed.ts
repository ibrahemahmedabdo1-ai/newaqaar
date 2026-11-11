import { PrismaClient } from "@prisma/client";
import bcrypt from "bcryptjs";
import { SEED } from "./seed-content";

const db = new PrismaClient();

async function main() {
  const email = (process.env.SEED_ADMIN_EMAIL ?? "").trim().toLowerCase();
  const password = process.env.SEED_ADMIN_PASSWORD ?? "";
  const name = process.env.SEED_ADMIN_NAME ?? "Super Admin";

  const admins = await db.adminUser.count();
  if (admins === 0) {
    if (!email || password.length < 12) {
      throw new Error("اضبط SEED_ADMIN_EMAIL و SEED_ADMIN_PASSWORD (12 حرفًا على الأقل). لا توجد كلمة مرور افتراضية.");
    }
    await db.adminUser.create({ data: { email, name, passwordHash: await bcrypt.hash(password, 12), role: "SUPER_ADMIN" } });
    console.log("Created super admin:", email);
  } else {
    console.log("Admin users already exist — skipping admin creation.");
  }

  for (const [key, p] of Object.entries(SEED.pages)) {
    await db.page.upsert({ where: { key }, update: {}, create: { key, ...p } });
  }
  if ((await db.faq.count()) === 0) {
    await db.faq.createMany({ data: SEED.faqs.map((f, i) => ({ ...f, sort: i })) });
  }
  for (const d of SEED.districts) {
    await db.district.upsert({ where: { name: d.name }, update: {}, create: d });
  }
  for (const t of SEED.types) {
    await db.propertyType.upsert({ where: { key: t.key }, update: {}, create: t });
  }
  console.log("Seed done.");
}

main().finally(() => db.$disconnect());
