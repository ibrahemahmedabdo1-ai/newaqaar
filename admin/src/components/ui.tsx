import Link from "next/link";
import type { ReactNode } from "react";
import type { Tone } from "@/lib/labels";

export function PageHeader({ title, sub, children }: { title: string; sub?: string; children?: ReactNode }) {
  return (
    <div className="ph">
      <div>
        <h1>{title}</h1>
        {sub ? <div className="sub">{sub}</div> : null}
      </div>
      <div className="row">{children}</div>
    </div>
  );
}

export function Card({ title, children }: { title?: string; children: ReactNode }) {
  return (
    <div className="card">
      {title ? <h3>{title}</h3> : null}
      {children}
    </div>
  );
}

export function Stat({ label, value, hint, tone, href }: { label: string; value: ReactNode; hint?: string; tone?: Tone; href?: string }) {
  const inner = (
    <div className={`stat ${tone ?? ""}`}>
      <div className="l">{label}</div>
      <div className="v">{value}</div>
      {hint ? <div className="h">{hint}</div> : null}
    </div>
  );
  return href ? <Link href={href} style={{ textDecoration: "none" }}>{inner}</Link> : inner;
}

export function Badge({ tone, children }: { tone?: Tone; children: ReactNode }) {
  return <span className={`badge ${tone ?? ""}`}>{children}</span>;
}

export function StatusBadge({ map, value }: { map: Record<string, [string, Tone]>; value: string }) {
  const [label, tone] = map[value] ?? [value, ""];
  return <Badge tone={tone}>{label}</Badge>;
}

export function Table({ head, children, empty }: { head: string[]; children: ReactNode; empty?: boolean }) {
  return (
    <div className="scroll">
      <table className="tbl">
        <thead>
          <tr>{head.map((h) => <th key={h}>{h}</th>)}</tr>
        </thead>
        <tbody>{children}</tbody>
      </table>
      {empty ? <div className="empty">لا توجد بيانات</div> : null}
    </div>
  );
}

export function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="field">
      <label>{label}</label>
      {children}
    </div>
  );
}

export function Pager({ page, total, size, base, params }: { page: number; total: number; size: number; base: string; params: Record<string, string> }) {
  const pages = Math.max(1, Math.ceil(total / size));
  if (pages <= 1) return <div className="muted" style={{ margin: "10px 0" }}>الإجمالي: {total}</div>;
  const href = (p: number) => {
    const q = new URLSearchParams({ ...params, page: String(p) });
    return `${base}?${q.toString()}`;
  };
  const nums: number[] = [];
  for (let i = Math.max(1, page - 2); i <= Math.min(pages, page + 2); i++) nums.push(i);
  return (
    <div className="pager">
      {page > 1 ? <Link href={href(page - 1)}>السابق</Link> : null}
      {nums.map((n) => (n === page ? <span key={n} className="cur">{n}</span> : <Link key={n} href={href(n)}>{n}</Link>))}
      {page < pages ? <Link href={href(page + 1)}>التالي</Link> : null}
      <span>— الإجمالي: {total}</span>
    </div>
  );
}

export function Alert({ children, bad }: { children: ReactNode; bad?: boolean }) {
  return <div className={`alert ${bad ? "bad" : ""}`}>{children}</div>;
}

export type SP = Promise<Record<string, string | string[] | undefined>>;
