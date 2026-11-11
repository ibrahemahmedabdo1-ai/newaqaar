export type Tone = "ok" | "warn" | "bad" | "info" | "";

export const ROLE_AR: Record<string, string> = { CLIENT: "مشتري", BROKER: "سمسار", DEVELOPER: "مطور عقاري" };
export const USER_STATUS: Record<string, [string, Tone]> = { ACTIVE: ["نشط", "ok"], SUSPENDED: ["موقوف", "warn"], BANNED: ["محظور", "bad"] };
export const AD_STATUS: Record<string, [string, Tone]> = { PENDING: ["قيد المراجعة", "warn"], PUBLISHED: ["منشور", "ok"], REJECTED: ["مرفوض", "bad"], ENDED: ["منتهي", ""] };
export const OFFER_STATUS: Record<string, [string, Tone]> = { PENDING: ["معلّق", "warn"], ACTIVE: ["مقبول", "ok"], REJECTED: ["مرفوض", "bad"] };
export const PAY_STATUS: Record<string, [string, Tone]> = { PENDING: ["معلّقة", "warn"], PAID: ["مدفوعة", "ok"], FAILED: ["فاشلة", "bad"], REFUNDED: ["مستردة", "info"] };
export const PAY_KIND: Record<string, string> = { BOOKING: "حجز", SUBSCRIPTION: "اشتراك" };
export const REPORT_STATUS: Record<string, [string, Tone]> = { OPEN: ["مفتوح", "warn"], RESOLVED: ["تمت المعالجة", "ok"], DISMISSED: ["مرفوض", ""] };
export const TICKET_STATUS: Record<string, [string, Tone]> = { OPEN: ["جديدة", "warn"], IN_PROGRESS: ["قيد المعالجة", "info"], CLOSED: ["مغلقة", "ok"] };
export const KIND_AR: Record<string, string> = { apt: "شقة", annex: "ملحق", villa: "فيلا", duplex: "دوبلكس", office: "مكتب", townhouse: "تاون هاوس" };
