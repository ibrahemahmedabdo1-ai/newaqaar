import type { PaymentKind, PaymentStatus, Prisma } from "@prisma/client";
import ConfirmButton from "@/components/ConfirmButton";
import { Alert, PageHeader, Pager, Stat, StatusBadge, Table, type SP } from "@/components/ui";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { fmtDateTime, fmtMoney, fmtNum, one, pageParams } from "@/lib/format";
import { PAY_KIND, PAY_STATUS } from "@/lib/labels";
import { can } from "@/lib/perms";
import { refundPayment } from "./actions";

export default async function PaymentsPage({ searchParams }: { searchParams: SP }) {
  const s = await requireAdmin("payments.read");
  const sp = await searchParams;
  const status = one(sp.status);
  const kind = one(sp.kind);
  const { page, take, skip } = pageParams(sp);
  const where: Prisma.PaymentWhereInput = {
    ...(status in PAY_STATUS ? { status: status as PaymentStatus } : {}),
    ...(kind in PAY_KIND ? { kind: kind as PaymentKind } : {}),
  };
  const [total, rows, paid, refunded, demoCount] = await Promise.all([
    db.payment.count({ where }),
    db.payment.findMany({ where, orderBy: { createdAt: "desc" }, take, skip, include: { user: { select: { name: true } } } }),
    db.payment.aggregate({ where: { status: "PAID" }, _sum: { total: true, vat: true }, _count: true }),
    db.payment.aggregate({ where: { status: "REFUNDED" }, _sum: { total: true }, _count: true }),
    db.payment.count({ where: { provider: "demo" } }),
  ]);
  const w = can(s.role, "payments.write");
  return (
    <>
      <PageHeader title="المدفوعات" sub="الحجوزات والاشتراكات والمبالغ المستردة">
        {can(s.role, "export") ? <a className="btn alt sm" href="/admin/export/payments">تصدير CSV</a> : null}
      </PageHeader>
      {demoCount > 0 ? <Alert>يوجد {fmtNum(demoCount)} عملية بالبوابة التجريبية (demo) — لا تمثل أموالًا حقيقية. اربط بوابة دفع فعلية قبل الإطلاق.</Alert> : null}
      <div className="grid g4" style={{ marginBottom: 14 }}>
        <Stat label="إجمالي المدفوع" value={fmtMoney(paid._sum.total ?? 0)} hint={`${fmtNum(paid._count)} عملية`} tone="ok" />
        <Stat label="منها ضريبة" value={fmtMoney(paid._sum.vat ?? 0)} />
        <Stat label="إجمالي المسترد" value={fmtMoney(refunded._sum.total ?? 0)} hint={`${fmtNum(refunded._count)} عملية`} />
      </div>
      <form className="filters" method="GET">
        <select name="status" defaultValue={status}>
          <option value="">كل الحالات</option>
          {Object.entries(PAY_STATUS).map(([k, v]) => <option key={k} value={k}>{v[0]}</option>)}
        </select>
        <select name="kind" defaultValue={kind}>
          <option value="">كل الأنواع</option>
          {Object.entries(PAY_KIND).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        <button className="btn" type="submit">تصفية</button>
      </form>
      <Table head={["المستخدم", "النوع", "المبلغ", "الضريبة", "الإجمالي", "الحالة", "البوابة", "التاريخ", ""]} empty={rows.length === 0}>
        {rows.map((p) => (
          <tr key={p.id}>
            <td>{p.user?.name ?? <span className="muted">{p.userPhone ?? "—"}</span>}</td>
            <td>{PAY_KIND[p.kind]}</td>
            <td>{fmtMoney(p.amount)}</td><td>{fmtMoney(p.vat)}</td><td><b>{fmtMoney(p.total)}</b></td>
            <td><StatusBadge map={PAY_STATUS} value={p.status} /></td>
            <td>{p.provider}</td>
            <td>{fmtDateTime(p.createdAt)}</td>
            <td>{w && p.status === "PAID" ? <form action={refundPayment.bind(null, p.id)}><ConfirmButton message="استرداد هذه العملية؟">استرداد</ConfirmButton></form> : null}</td>
          </tr>
        ))}
      </Table>
      <Pager page={page} total={total} size={take} base="/admin/payments" params={{ status, kind }} />
    </>
  );
}
