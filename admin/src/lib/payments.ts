import { randomUUID } from "crypto";

export type ChargeInput = { amountSar: number; kind: "BOOKING" | "SUBSCRIPTION"; userId: string; reference: string };
export type ChargeResult = { ok: true; ref: string; provider: string } | { ok: false; error: string };

export interface Gateway {
  name: string;
  charge(input: ChargeInput): Promise<ChargeResult>;
  refund(ref: string): Promise<boolean>;
}

/**
 * بوابة تجريبية: لا تسحب أي مبلغ.
 * في الإنتاج تُرفض ما لم تضبط ALLOW_DEMO_PAYMENTS=true صراحةً (حتى لا تُعتبر مدفوعات وهمية حقيقية بالخطأ).
 *
 * لربط بوابة حقيقية (Moyasar / HyperPay / Tap ...): أضف كلاسًا يطبّق Gateway
 * وأرجعه من gateway() حسب PAYMENT_PROVIDER. لا تحفظ بيانات البطاقات عندك.
 * ملاحظة Google Play: اشتراك المزايا الرقمية داخل التطبيق يجب أن يمر عبر Google Play Billing وليس هذه البوابة.
 */
const demo: Gateway = {
  name: "demo",
  async charge() {
    if (process.env.NODE_ENV === "production" && process.env.ALLOW_DEMO_PAYMENTS !== "true") {
      return { ok: false, error: "PAYMENTS_NOT_CONFIGURED" };
    }
    return { ok: true, ref: "demo_" + randomUUID(), provider: "demo" };
  },
  async refund() {
    return true;
  },
};

export function gateway(): Gateway {
  const p = process.env.PAYMENT_PROVIDER ?? "demo";
  if (p === "demo") return demo;
  throw new Error(`PAYMENT_PROVIDER=${p} is not implemented. Add a Gateway in src/lib/payments.ts`);
}
