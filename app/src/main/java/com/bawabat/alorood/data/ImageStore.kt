package com.bawabat.alorood.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.exifinterface.media.ExifInterface
import java.io.File

/** نسخ الصور المختارة عبر منتقي الصور إلى مخزن التطبيق الداخلي بحجم مضغوط. */
object ImageStore {
    private fun orientation(ctx: Context, uri: Uri): Int = try {
        ctx.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (e: Exception) { ExifInterface.ORIENTATION_NORMAL }

    private fun rotate(b: Bitmap, o: Int): Bitmap {
        val deg = when (o) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return b
        }
        val m = Matrix().apply { postRotate(deg) }
        return Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
    }

    fun save(ctx: Context, uri: Uri, name: String): String? = try {
        val dir = File(ctx.filesDir, "images").apply { mkdirs() }
        val out = File(dir, "$name.jpg")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        if (bmp == null) null else {
            val fixed = rotate(bmp, orientation(ctx, uri))
            out.outputStream().use { fixed.compress(Bitmap.CompressFormat.JPEG, 82, it) }
            out.absolutePath
        }
    } catch (e: Exception) { null }

    fun saveBitmap(ctx: Context, bmp: Bitmap, name: String): String? = try {
        val dir = File(ctx.filesDir, "images").apply { mkdirs() }
        val out = File(dir, "$name.jpg")
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 82, it) }
        out.absolutePath
    } catch (e: Exception) { null }

    fun load(path: String, maxPx: Int): ImageBitmap? = try {
        val f = File(path)
        if (!f.exists()) null else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            while (bounds.outWidth / sample > maxPx || bounds.outHeight / sample > maxPx) sample *= 2
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
        }
    } catch (e: Exception) { null }

    private val cache = android.util.LruCache<String, ImageBitmap>(48)

    /** تنزيل صورة من رابط (صور الإعلانات القادمة من الخادم) مع تخزين مؤقت في الذاكرة. */
    suspend fun loadUrl(url: String, maxPx: Int): ImageBitmap? {
        cache.get(url)?.let { return it }
        val bytes = Api.download(url) ?: return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / sample > maxPx || bounds.outHeight / sample > maxPx) sample *= 2
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                ?.asImageBitmap()
            if (bmp != null) cache.put(url, bmp)
            bmp
        } catch (e: Exception) { null }
    }

    fun delete(path: String) { try { File(path).delete() } catch (e: Exception) { /* ignore */ } }
}
