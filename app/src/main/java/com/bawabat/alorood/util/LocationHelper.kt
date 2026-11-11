package com.bawabat.alorood.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import com.bawabat.alorood.data.Repo

object LocationHelper {
    /** آخر موقع معروف (تقريبي). يُستدعى فقط بعد منح صلاحية ACCESS_COARSE_LOCATION. */
    @SuppressLint("MissingPermission")
    fun updateFromLastKnown(ctx: Context): Boolean = try {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc = lm.getProviders(true).mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
        if (loc != null) {
            Repo.userLat = loc.latitude
            Repo.userLng = loc.longitude
            true
        } else false
    } catch (e: Exception) { false }
}
