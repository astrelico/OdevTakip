package com.odevtakip.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Uygulamanın tek veritabanı.
 *
 * Şema sürümü 1. İleride alan eklenirse migration yazılacak;
 * şema JSON'ları `app/schemas/` altına KSP ile kaydediliyor.
 */
@Database(
    entities = [Odev::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class OdevDatabase : RoomDatabase() {

    abstract fun odevDao(): OdevDao

    companion object {
        @Volatile
        private var INSTANCE: OdevDatabase? = null

        fun getInstance(context: Context): OdevDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    OdevDatabase::class.java,
                    "odev-takip.db"
                )
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
