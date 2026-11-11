import type { ReactNode } from "react";
import NavLink from "@/components/NavLink";
import { requireAdmin } from "@/lib/auth";
import { can, ROLE_LABELS, type Perm } from "@/lib/perms";
import { logoutAction } from "./actions";

type Item = { href: string; label: string; perm: Perm; exact?: boolean };
const GROUPS: { title: string; items: Item[] }[] = [
  { title: "نظرة عامة", items: [{ href: "/admin", label: "لوحة المؤشرات", perm: "dashboard", exact: true }] },
  {
    title: "العمليات",
    items: [
      { href: "/admin/users", label: "المستخدمون", perm: "users.read" },
      { href: "/admin/ads", label: "الإعلانات", perm: "ads.read" },
      { href: "/admin/offers", label: "العروض والحجوزات", perm: "offers.read" },
      { href: "/admin/payments", label: "المدفوعات", perm: "payments.read" },
      { href: "/admin/reports", label: "البلاغات", perm: "reports.manage" },
      { href: "/admin/chats", label: "المحادثات", perm: "chats.read" },
      { href: "/admin/tickets", label: "الدعم الفني", perm: "tickets.manage" },
    ],
  },
  {
    title: "المحتوى",
    items: [
      { href: "/admin/content/faq", label: "الأسئلة الشائعة", perm: "content.manage" },
      { href: "/admin/content/pages", label: "الصفحات القانونية", perm: "content.manage" },
      { href: "/admin/content/news", label: "أخبار العقارات", perm: "content.manage" },
      { href: "/admin/content/lookups", label: "المناطق والأنواع", perm: "content.manage" },
      { href: "/admin/announcements", label: "الإعلانات داخل التطبيق", perm: "announce.manage" },
    ],
  },
  {
    title: "النظام",
    items: [
      { href: "/admin/settings", label: "الإعدادات", perm: "settings.read" },
      { href: "/admin/team", label: "فريق الإدارة", perm: "team.manage" },
      { href: "/admin/audit", label: "سجل العمليات", perm: "audit.read" },
    ],
  },
];

export default async function AdminLayout({ children }: { children: ReactNode }) {
  const s = await requireAdmin();
  return (
    <div className="shell">
      <aside className="side">
        <div className="brand">
          <div className="logo">BD</div>
          <div>بوابة العروض</div>
        </div>
        <nav className="nav">
          {GROUPS.map((g) => {
            const items = g.items.filter((i) => can(s.role, i.perm));
            if (items.length === 0) return null;
            return (
              <div key={g.title}>
                <div className="grp">{g.title}</div>
                {items.map((i) => (
                  <NavLink key={i.href} href={i.href} exact={i.exact}>{i.label}</NavLink>
                ))}
              </div>
            );
          })}
        </nav>
      </aside>
      <div className="main">
        <div className="top">
          <div className="who">
            {s.name} · {ROLE_LABELS[s.role]} · {s.email}
          </div>
          <form action={logoutAction}>
            <button className="btn alt sm" type="submit">تسجيل الخروج</button>
          </form>
        </div>
        <div className="content">{children}</div>
      </div>
    </div>
  );
}
