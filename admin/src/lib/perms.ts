import type { AdminRole } from "@prisma/client";

export type Perm =
  | "dashboard" | "users.read" | "users.write" | "ads.read" | "ads.moderate"
  | "offers.read" | "offers.write" | "payments.read" | "payments.write"
  | "reports.manage" | "chats.read" | "chats.moderate" | "tickets.manage"
  | "content.manage" | "announce.manage" | "settings.read" | "settings.write"
  | "team.manage" | "audit.read" | "export";

const ALL: Perm[] = [
  "dashboard", "users.read", "users.write", "ads.read", "ads.moderate", "offers.read", "offers.write",
  "payments.read", "payments.write", "reports.manage", "chats.read", "chats.moderate", "tickets.manage",
  "content.manage", "announce.manage", "settings.read", "settings.write", "team.manage", "audit.read", "export",
];

export const ROLE_PERMS: Record<AdminRole, Perm[]> = {
  SUPER_ADMIN: ALL,
  ADMIN: ALL.filter((p) => p !== "team.manage" && p !== "settings.write"),
  MODERATOR: ["dashboard", "users.read", "ads.read", "ads.moderate", "reports.manage", "chats.read", "chats.moderate", "tickets.manage"],
  SUPPORT: ["dashboard", "users.read", "ads.read", "offers.read", "tickets.manage", "chats.read"],
  FINANCE: ["dashboard", "payments.read", "payments.write", "offers.read", "export"],
};

export const ROLE_LABELS: Record<AdminRole, string> = {
  SUPER_ADMIN: "مدير عام",
  ADMIN: "مدير",
  MODERATOR: "مشرف محتوى",
  SUPPORT: "دعم فني",
  FINANCE: "مالية",
};

export function can(role: AdminRole, perm: Perm): boolean {
  return ROLE_PERMS[role].includes(perm);
}
