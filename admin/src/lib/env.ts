export function must(name: string): string {
  const v = process.env[name];
  if (!v) throw new Error(`Missing environment variable: ${name}`);
  return v;
}

export function secret(name: "ADMIN_JWT_SECRET" | "API_JWT_SECRET"): Uint8Array {
  const v = must(name);
  if (v.length < 32) throw new Error(`${name} must be at least 32 characters`);
  return new TextEncoder().encode(v);
}

export const isProd = process.env.NODE_ENV === "production";
