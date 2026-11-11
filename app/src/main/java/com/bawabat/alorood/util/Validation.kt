package com.bawabat.alorood.util

import java.security.MessageDigest
import java.util.Locale
import kotlin.math.ceil

object Validation {
    /** تحويل الأرقام العربية/الفارسية إلى أرقام لاتينية */
    fun normalizeDigits(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) {
            when (c) {
                in '\u0660'..'\u0669' -> sb.append('0' + (c - '\u0660'))
                in '\u06F0'..'\u06F9' -> sb.append('0' + (c - '\u06F0'))
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    fun digitsOnly(s: String): String = normalizeDigits(s).filter { it in '0'..'9' }

    /** رقم الهاتف بدون كود الدولة: من 7 إلى 12 رقم */
    fun isValidPhone(number: String): Boolean {
        val d = digitsOnly(number)
        return d.length in 7..12
    }

    fun fullPhone(code: String, number: String): String {
        var d = digitsOnly(number)
        if (d.startsWith("0")) d = d.trimStart('0')
        return code.trim() + d
    }

    /** null = سليمة، وإلا رمز الخطأ */
    fun passwordError(pw: String): String? = when {
        pw.length < 8 -> "short"
        pw.none { it.isLetter() } -> "letter"
        pw.none { it.isDigit() } -> "digit"
        else -> null
    }

    fun isOfferValid(amount: Long, minOffer: Long): Boolean = amount >= minOffer && amount > 0

    fun formatPrice(v: Long): String {
        val s = v.toString()
        val sb = StringBuilder()
        for ((i, c) in s.reversed().withIndex()) {
            if (i > 0 && i % 3 == 0) sb.append(',')
            sb.append(c)
        }
        return sb.reverse().toString()
    }

    fun parseAmount(s: String): Long = digitsOnly(s).take(12).toLongOrNull() ?: 0L

    fun daysLeft(createdAt: Long, now: Long, totalDays: Int = 3): Int {
        val remainMs = createdAt + totalDays * 86_400_000L - now
        return if (remainMs <= 0) 0 else ceil(remainMs / 86_400_000.0).toInt()
    }

    fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { String.format(Locale.US, "%02x", it) }

    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2)
        return 2 * r * Math.asin(Math.sqrt(a))
    }
}
