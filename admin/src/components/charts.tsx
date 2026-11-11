export type Point = { label: string; value: number };

export function BarChart({ data, color = "#001716" }: { data: Point[]; color?: string }) {
  const max = Math.max(1, ...data.map((d) => d.value));
  const w = 600;
  const h = 170;
  const bw = w / Math.max(1, data.length);
  return (
    <svg className="chart" viewBox={`0 0 ${w} ${h}`} preserveAspectRatio="none" role="img">
      {data.map((d, i) => {
        const bh = (d.value / max) * (h - 36);
        return (
          <g key={i}>
            <rect x={i * bw + 4} y={h - 20 - bh} width={Math.max(2, bw - 8)} height={bh} rx={3} fill={color} />
            <text x={i * bw + bw / 2} y={h - 6} fontSize="10" textAnchor="middle" fill="#6b6b6b">{d.label}</text>
            {d.value > 0 ? <text x={i * bw + bw / 2} y={h - 24 - bh} fontSize="10" textAnchor="middle" fill="#2b2a2a">{d.value}</text> : null}
          </g>
        );
      })}
    </svg>
  );
}

export function HBars({ data }: { data: Point[] }) {
  const max = Math.max(1, ...data.map((d) => d.value));
  return (
    <div>
      {data.map((d) => (
        <div key={d.label} style={{ margin: "8px 0" }}>
          <div className="row" style={{ justifyContent: "space-between" }}>
            <span>{d.label}</span>
            <b>{d.value}</b>
          </div>
          <div style={{ background: "#eee", borderRadius: 6, height: 8 }}>
            <div style={{ width: `${(d.value / max) * 100}%`, background: "#f2c200", height: 8, borderRadius: 6 }} />
          </div>
        </div>
      ))}
    </div>
  );
}
