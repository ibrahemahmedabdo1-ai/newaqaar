import Link from "next/link";
import { BarChart, HBars, type Point } from "@/components/charts";
import { Alert, Card, PageHeader, Stat, StatusBadge, Table } from "@/components/ui";
import { requireAdmin } from "@/lib/auth";
import { db } from "@/lib/db";
import { fmtDateTime, fmtMoney, fmtNum } from "@/lib/format";
import { AD_STATUS, KIND_AR, ROLE_AR } from "@/lib/labels";
import { can } from "@/lib/perms";
import { readiness } from "@/lib/readiness";
import { getSettings } from "@/lib/settings";

type Row = { d: Date; c: bigint };

function fillDays(rows: Row[], days = 14): Point[] {
  const map = new Map(rows.map((r) => [r.d.toISOString().slice(0, 10), Number(r.c)]));
  const out: Point[] = [];
  for (let i = days - 1; i >= 0; i--) {
    const k = new Date(Date.now() - i * 86_400_000).toISOString().slice(0, 10);
    out.push({ label: k.slice(5), value: map.get(k) ?? 0 });
  }
  return out;
}

export default async function Dashboard({ searchParams }: { searchParams: Promise<{ denied?: string }> }) {
  const sess = await requireAdmin("dashboard");
  const { denied } = await searchParams;
  const checks = can(sess.role, "settings.read") ? readiness(await getSettings()) : [];
  const [usersTotal, byRole, suspended, adsByStatus, offersPending, paid, refunded, openReports, openTickets, subs, newUsers, newOffers, pendingAds, openRep] =
    await Promise.all([
      db.user.count(),
      db.user.groupBy({ by: ["role"], _count: true }),
      db.user.count({ where: { status: { not: "ACTIVE" } } }),
      db.property.groupBy({ by: ["status"], _count: true }),
      db.offer.count({ where: { status: "PENDING" } }),
      db.payment.aggregate({ where: { status: "PAID" }, _sum: { total: true } }),
      db.payment.aggregate({ where: { status: "REFUNDED" }, _sum: { total: true } }),
      db.report.count({ where: { status: "OPEN" } }),
      db.supportTicket.count({ where: { status: { not: "CLOSED" } } }),
      db.user.count({ where: { subscribed: true } }),
      db.$queryRaw<Row[]>`SELECT date_trunc('day', "createdAt") AS d, COUNT(*)::bigint AS c FROM "User" WHERE "createdAt" > now() - interval '14 days' GROUP BY 1 ORDER BY 1`,
      db.$queryRaw<Row[]>`SELECT date_trunc('day', "createdAt") AS d, COUNT(*)::bigint AS c FROM "Offer" WHERE "createdAt" > now() - interval '14 days' GROUP BY 1 ORDER BY 1`,
      db.property.findMany({ where: { status: "PENDING" }, orderBy: { createdAt: "asc" }, take: 6, include: { owner: { select: { name: true } } } }),
      db.report.findMany({ where: { status: "OPEN" }, orderBy: { createdAt: "asc" }, take: 6 }),
    ]);
  const adCount = (s: string) => adsByStatus.find((a) => a.status === s)?._count ?? 0;

  return (
    <>
      <PageHeader title="لوحة المؤشرات" sub="نظرة سريعة على أداء المنصة" />
      {denied ? <Alert bad>لا تملك صلاحية الوصول لهذه الصفحة.</Alert> : null}
      {checks.filter((c) => c.level !== "ok").length > 0 ? (
        <Card title="جاهزية الإنتاج">
          {checks.map((c, i) => (
            <div key={i} style={{ margin: "4px 0" }}>
              <span className={`badge ${c.level}`}>{c.level === "ok" ? "جاهز" : c.level === "warn" ? "تنبيه" : "مهم"}</span> {c.text}
            </div>
          ))}
        </Card>
      ) : null}
      <div className="grid g4" style={{ marginBottom: 16 }}>
        <Stat label="إجمالي المستخدمين" value={fmtNum(usersTotal)} hint={`${fmtNum(subs)} مشترك · ${fmtNum(suspended)} موقوف/محظور`} href="/admin/users" />
        <Stat label="إعلانات قيد المراجعة" value={fmtNum(adCount("PENDING"))} tone={adCount("PENDING") > 0 ? "warn" : ""} href="/admin/ads?status=PENDING" />
        <Stat label="إعلانات منشورة" value={fmtNum(adCount("PUBLISHED"))} tone="ok" href="/admin/ads?status=PUBLISHED" />
        <Stat label="عروض معلقة" value={fmtNum(offersPending)} href="/admin/offers?status=PENDING" />
        <Stat label="إجمالي المدفوع" value={fmtMoney(paid._sum.total ?? 0)} tone="ok" href="/admin/payments?status=PAID" />
        <Stat label="إجمالي المسترد" value={fmtMoney(refunded._sum.total ?? 0)} href="/admin/payments?status=REFUNDED" />
        <Stat label="بلاغات مفتوحة" value={fmtNum(openReports)} tone={openReports > 0 ? "bad" : ""} href="/admin/reports?status=OPEN" />
        <Stat label="تذاكر دعم مفتوحة" value={fmtNum(openTickets)} tone={openTickets > 0 ? "warn" : ""} href="/admin/tickets" />
      </div>

      <div className="grid g2">
        <Card title="مستخدمون جدد (آخر 14 يومًا)"><BarChart data={fillDays(newUsers)} /></Card>
        <Card title="عروض جديدة (آخر 14 يومًا)"><BarChart data={fillDays(newOffers)} color="#b7791f" /></Card>
        <Card title="المستخدمون حسب الدور">
          <HBars data={byRole.map((r) => ({ label: ROLE_AR[r.role] ?? r.role, value: r._count }))} />
        </Card>
        <Card title="الإعلانات حسب الحالة">
          <HBars data={adsByStatus.map((a) => ({ label: AD_STATUS[a.status]?.[0] ?? a.status, value: a._count }))} />
        </Card>
      </div>

      <div className="grid g2">
        <Card title="إعلانات تنتظر المراجعة">
          <Table head={["الإعلان", "المعلن", "التاريخ"]} empty={pendingAds.length === 0}>
            {pendingAds.map((p) => (
              <tr key={p.id}>
                <td><Link href={`/admin/ads/${p.id}`}>{KIND_AR[p.kind] ?? p.kind} {p.rooms} غرف — {p.district}</Link></td>
                <td>{p.owner.name}</td>
                <td>{fmtDateTime(p.createdAt)}</td>
              </tr>
            ))}
          </Table>
        </Card>
        <Card title="بلاغات مفتوحة">
          <Table head={["السبب", "الحالة", "التاريخ"]} empty={openRep.length === 0}>
            {openRep.map((r) => (
              <tr key={r.id}>
                <td><Link href="/admin/reports?status=OPEN">{r.reason}</Link></td>
                <td><StatusBadge map={{ OPEN: ["مفتوح", "warn"] }} value={r.status} /></td>
                <td>{fmtDateTime(r.createdAt)}</td>
              </tr>
            ))}
          </Table>
        </Card>
      </div>
    </>
  );
}
