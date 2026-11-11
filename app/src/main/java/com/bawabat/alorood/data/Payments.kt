package com.bawabat.alorood.data

import kotlinx.coroutines.delay

/**
 * واجهة الدفع. النسخة الحالية تجريبية ولا تسحب أي مبالغ.
 *
 * قبل النشر على Google Play:
 *  - مبلغ الحجز العقاري (خدمة/سلعة حقيقية): اربطه ببوابة دفع مرخّصة (مثل Moyasar / HyperPay / Tap / مدى).
 *  - اشتراك المزايا داخل التطبيق (سلعة رقمية): يجب استخدام Google Play Billing وفق سياسة الدفع في Google Play.
 */
interface PaymentGateway {
    suspend fun charge(amountSar: Long, purpose: String): Boolean
}

object DemoGateway : PaymentGateway {
    override suspend fun charge(amountSar: Long, purpose: String): Boolean {
        delay(900)
        return true
    }
}

object Payments {
    var gateway: PaymentGateway = DemoGateway
}
