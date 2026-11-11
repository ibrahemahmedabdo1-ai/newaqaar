import { redirect } from "next/navigation";
import { getSession } from "@/lib/auth";
import { loginAction } from "./actions";

export default async function LoginPage({ searchParams }: { searchParams: Promise<{ e?: string }> }) {
  if (await getSession()) redirect("/admin");
  const { e } = await searchParams;
  return (
    <div className="login">
      <form className="box" action={loginAction}>
        <h1>بوابة العروض</h1>
        <p className="muted" style={{ marginTop: 0 }}>تسجيل دخول لوحة التحكم</p>
        {e ? <div className="alert bad">{e}</div> : null}
        <div className="field">
          <label>البريد الإلكتروني</label>
          <input name="email" type="email" required autoComplete="username" />
        </div>
        <div className="field">
          <label>كلمة المرور</label>
          <input name="password" type="password" required autoComplete="current-password" />
        </div>
        <button className="btn gold" style={{ width: "100%" }} type="submit">دخول</button>
      </form>
    </div>
  );
}
