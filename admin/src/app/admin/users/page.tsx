import Link from "next/link";
import type { Prisma, Role, UserStatus } from "@prisma/client";
import { Badge, PageHeader, Pager, StatusBadge, Table, type SP } from "@/components/ui";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { fmtDateTime, one, pageParams } from "@/lib/format";
import { ROLE_AR, USER_STATUS } from "@/lib/labels";
import { can } from "@/lib/perms";

export default async function UsersPage({ searchParams }: { searchParams: SP }) {
  const s = await requireAdmin("users.read");
  const sp = await searchParams;
  const q = one(sp.q).trim();
  const role = one(sp.role);
  const status = one(sp.status);
  const { page, take, skip } = pageParams(sp);
  const where: Prisma.UserWhereInput = {
    ...(q ? { OR: [{ name: { contains: q, mode: "insensitive" } }, { phone: { contains: q } }] } : {}),
    ...(role in ROLE_AR ? { role: role as Role } : {}),
    ...(status in USER_STATUS ? { status: status as UserStatus } : {}),
  };
  const [total, rows] = await Promise.all([
    db.user.count({ where }),
    db.user.findMany({ where, orderBy: { createdAt: "desc" }, take, skip }),
  ]);
  return (
    <>
      <PageHeader title="المستخدمون" sub="إدارة الحسابات والأدوار والحالة">
        {can(s.role, "export") ? <a className="btn alt sm" href="/admin/export/users">تصدير CSV</a> : null}
      </PageHeader>
      <form className="filters" method="GET">
        <input name="q" defaultValue={q} placeholder="بحث بالاسم أو الهاتف" />
        <select name="role" defaultValue={role}>
          <option value="">كل الأدوار</option>
          {Object.entries(ROLE_AR).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        <select name="status" defaultValue={status}>
          <option value="">كل الحالات</option>
          {Object.entries(USER_STATUS).map(([k, v]) => <option key={k} value={k}>{v[0]}</option>)}
        </select>
        <button className="btn" type="submit">بحث</button>
      </form>
      <Table head={["الاسم", "الهاتف", "الدور", "الحالة", "اشتراك", "التسجيل", "آخر دخول"]} empty={rows.length === 0}>
        {rows.map((u) => (
          <tr key={u.id}>
            <td><Link href={`/admin/users/${u.id}`}>{u.name}</Link></td>
            <td dir="ltr" style={{ textAlign: "end" }}>{u.phone}</td>
            <td>{ROLE_AR[u.role]}</td>
            <td><StatusBadge map={USER_STATUS} value={u.status} /></td>
            <td>{u.subscribed ? <Badge tone="ok">مشترك</Badge> : <span className="muted">—</span>}</td>
            <td>{fmtDateTime(u.createdAt)}</td>
            <td>{fmtDateTime(u.lastLoginAt)}</td>
          </tr>
        ))}
      </Table>
      <Pager page={page} total={total} size={take} base="/admin/users" params={{ q, role, status }} />
    </>
  );
}
