const dtf = new Intl.DateTimeFormat("ar-EG", { dateStyle: "medium", timeStyle: "short", timeZone: "Asia/Riyadh", numberingSystem: "latn" } as Intl.DateTimeFormatOptions);
const df = new Intl.DateTimeFormat("ar-EG", { dateStyle: "medium", timeZone: "Asia/Riyadh", numberingSystem: "latn" } as Intl.DateTimeFormatOptions);

export const fmtDateTime = (d?: Date | null) => (d ? dtf.format(d) : "—");
export const fmtDate = (d?: Date | null) => (d ? df.format(d) : "—");
export const fmtNum = (n: number | bigint) => new Intl.NumberFormat("en-US").format(n);
export const fmtMoney = (n: number) => `${fmtNum(n)} ر.س`;

export function pageParams(sp: Record<string, string | string[] | undefined>, size = 20) {
  const raw = Array.isArray(sp.page) ? sp.page[0] : sp.page;
  const page = Math.max(1, parseInt(raw ?? "1", 10) || 1);
  return { page, take: size, skip: (page - 1) * size };
}

export function one(v: string | string[] | undefined): string {
  return (Array.isArray(v) ? v[0] : v) ?? "";
}
