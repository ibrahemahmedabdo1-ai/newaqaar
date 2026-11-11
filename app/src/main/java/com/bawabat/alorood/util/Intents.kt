package com.bawabat.alorood.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object Intents {
    private fun launch(ctx: Context, i: Intent, failMsg: String) {
        try {
            ctx.startActivity(i)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(ctx, failMsg, Toast.LENGTH_SHORT).show()
        }
    }

    fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    fun dial(ctx: Context, phone: String) =
        launch(ctx, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone))), "No dialer")

    fun whatsapp(ctx: Context, number: String) {
        val d = Validation.digitsOnly(number)
        launch(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$d")), "WhatsApp / browser not found")
    }

    fun email(ctx: Context, to: String, subject: String, body: String) {
        val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        launch(ctx, i, "No email app")
    }

    fun share(ctx: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        launch(ctx, Intent.createChooser(send, null), "Cannot share")
    }

    fun openMap(ctx: Context, lat: Double, lng: Double, label: String) {
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
        launch(ctx, Intent(Intent.ACTION_VIEW, uri), "No maps app")
    }

    fun openUrl(ctx: Context, url: String) =
        launch(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(url)), "No browser")
}
