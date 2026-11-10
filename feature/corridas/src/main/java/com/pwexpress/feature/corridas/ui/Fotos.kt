package com.pwexpress.feature.corridas.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

internal object Fotos {

    /** Arquivo temporário onde a câmera grava a foto. */
    fun novoDestino(ctx: Context): Pair<File, Uri> {
        val pasta = File(ctx.cacheDir, "fotos").apply { mkdirs() }
        val arquivo = File(pasta, "comprovante-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.comprovantes", arquivo)
        return arquivo to uri
    }

    /** Reduz para no máximo 1600 px, corrige a rotação e comprime em JPEG. */
    fun preparar(arquivo: File, ladoMax: Int = 1600): ByteArray {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(arquivo.absolutePath, limites)
        var amostra = 1
        while (maxOf(limites.outWidth, limites.outHeight) / (amostra * 2) >= ladoMax) amostra *= 2
        val bmp = BitmapFactory.decodeFile(arquivo.absolutePath, BitmapFactory.Options().apply { inSampleSize = amostra })
            ?: error("Não foi possível ler a foto")

        val graus = when (ExifInterface(arquivo.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val escala = minOf(1f, ladoMax.toFloat() / maxOf(bmp.width, bmp.height))
        val m = Matrix().apply { postScale(escala, escala); postRotate(graus) }
        val final = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        return ByteArrayOutputStream().also { final.compress(Bitmap.CompressFormat.JPEG, 80, it) }.toByteArray()
    }

    fun miniatura(bytes: ByteArray): Bitmap? = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
