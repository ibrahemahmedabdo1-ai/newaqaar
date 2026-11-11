"use server";

import { redirect } from "next/navigation";
import { loginAdmin } from "@/lib/auth";

export async function loginAction(formData: FormData) {
  const email = String(formData.get("email") ?? "");
  const password = String(formData.get("password") ?? "");
  const r = await loginAdmin(email, password);
  if (!r.ok) redirect("/login?e=" + encodeURIComponent(r.error));
  redirect("/admin");
}
