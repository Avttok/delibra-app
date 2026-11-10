package com.pwexpress.feature.tracking.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.pwexpress.feature.tracking.Modo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

internal data class PontoGps(
    val idLocal: String,
    val sessaoId: String?,
    val status: Modo,
    val lat: Double,
    val lng: Double,
    val precisao: Float?,
    val velocidade: Float?,
    val direcao: Float?,
    val altitude: Double?,
    val bateria: Int?,
    val registradoEm: Long,
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("id_local", idLocal)
        sessaoId?.let {
            put("sessao_id", it)
            if (status == Modo.EM_CORRIDA) put("corrida_id", it) // em corrida, a sessão é a própria corrida
        }
        put("status", status.api)
        put("lat", lat)
        put("lng", lng)
        precisao?.let { put("precisao_m", it) }
        velocidade?.let { put("velocidade_ms", it) }
        direcao?.let { put("direcao_graus", it) }
        altitude?.let { put("altitude_m", it) }
        bateria?.let { put("bateria", it) }
        put("registrado_em", Instant.ofEpochMilli(registradoEm).toString())
    }
}

/**
 * Fila offline: todo ponto GPS é gravado aqui primeiro e só sai depois que
 * o servidor confirmar. Sem sinal, nada se perde.
 */
internal class FilaLocalizacoes(context: Context) :
    SQLiteOpenHelper(context, "pwex_fila.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE fila (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              id_local TEXT NOT NULL,
              sessao_id TEXT,
              status TEXT NOT NULL,
              lat REAL NOT NULL,
              lng REAL NOT NULL,
              precisao REAL,
              velocidade REAL,
              direcao REAL,
              altitude REAL,
              bateria INTEGER,
              registrado_em INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS fila")
        onCreate(db)
    }

    @Synchronized
    fun inserir(p: PontoGps) {
        val v = ContentValues().apply {
            put("id_local", p.idLocal)
            put("sessao_id", p.sessaoId)
            put("status", p.status.api)
            put("lat", p.lat)
            put("lng", p.lng)
            put("precisao", p.precisao)
            put("velocidade", p.velocidade)
            put("direcao", p.direcao)
            put("altitude", p.altitude)
            put("bateria", p.bateria)
            put("registrado_em", p.registradoEm)
        }
        writableDatabase.insert("fila", null, v)
    }

    @Synchronized
    fun proximos(limite: Int): List<Pair<Long, PontoGps>> {
        val lista = mutableListOf<Pair<Long, PontoGps>>()
        readableDatabase.rawQuery("SELECT * FROM fila ORDER BY id LIMIT ?", arrayOf(limite.toString())).use { c ->
            while (c.moveToNext()) {
                lista += c.long("id") to PontoGps(
                    idLocal = c.str("id_local")!!,
                    sessaoId = c.str("sessao_id"),
                    status = Modo.de(c.str("status")),
                    lat = c.dbl("lat")!!,
                    lng = c.dbl("lng")!!,
                    precisao = c.dbl("precisao")?.toFloat(),
                    velocidade = c.dbl("velocidade")?.toFloat(),
                    direcao = c.dbl("direcao")?.toFloat(),
                    altitude = c.dbl("altitude"),
                    bateria = c.dbl("bateria")?.toInt(),
                    registradoEm = c.long("registrado_em"),
                )
            }
        }
        return lista
    }

    @Synchronized
    fun remover(ids: List<Long>) {
        if (ids.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            ids.forEach { db.delete("fila", "id = ?", arrayOf(it.toString())) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun total(): Int = DatabaseUtils.queryNumEntries(readableDatabase, "fila").toInt()

    @Synchronized
    fun limpar() {
        writableDatabase.delete("fila", null, null)
    }

    private fun Cursor.idx(col: String) = getColumnIndexOrThrow(col)
    private fun Cursor.str(col: String): String? = idx(col).let { if (isNull(it)) null else getString(it) }
    private fun Cursor.dbl(col: String): Double? = idx(col).let { if (isNull(it)) null else getDouble(it) }
    private fun Cursor.long(col: String): Long = getLong(idx(col))
}
