package com.odevtakip.app.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room migration'larının doğruluğu.
 *
 * **Neden var:** migration yanlış yazıldığında hata hemen fırlamaz. Room
 * migration sonrası şemayı karşılaştırır ve uyuşmazsa açılışta patlar; ama
 * uyuşsa bile **yanlış tablo** kurulmuş olabilir (ör. `NOT NULL` eksik
 * kolon → kullanıcı ilk kaydı atınca çökme). Bu test, gerçek bir v4
 * veritabanı açıp 5'e taşıyıp **içeriği de** okuyarak ikisini birden
 * kapatır.
 *
 * Şema JSON'ları `app/schemas/` altındadır ve androidTest asset'ine
 * kaynak olarak eklenmiştir (bkz. `app/build.gradle.kts`).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    /**
     * Yardımcı, test cihazında **geçici** bir veritabanı açar; `@Rule`
     * her testten sonra dosyayı siler.
     *
     * [FrameworkSQLiteOpenHelperFactory] Room'un üretimde kullandığı
     * sağlayıcıdır — test ile gerçek açılış aynı yolla şema kurar.
     */
    @get:Rule
    val yardimci = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        OdevDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    /**
     * v4 tablolarını kurar ve bir miktar gerçek veri yazar.
     *
     * `createDatabase` şema JSON'undaki `createStatements` ile kurulum
     * yaptığı için elle yazılmış `CREATE TABLE` ile şemanın sapması testin
     * dışında kalır — test tam olarak **migration'ın** getirdiği değişikliği
     * ölçer.
     */
    private fun v4VeritabaniOlustur(): SupportSQLiteDatabase =
        yardimci.createDatabase(TEST_DOSYA, 4).also { db ->
            db.execSQL(
                "INSERT INTO odevler (baslik, aciklama, ders, sonTarih, durum, olusturmaTarihi) " +
                    "VALUES ('Matematik 1. ünite', 'sayfa 12', 'Matematik', ?, 'BEKLIYOR', ?)",
                arrayOf(2_000_000_000_000L, 1_700_000_000_000L),
            )
            db.execSQL("INSERT INTO dersler (ad, olusturmaTarihi) VALUES ('Fizik', 99)")
            db.execSQL("INSERT INTO program (gun, sira, ders) VALUES (1, 1, 'Kimya')")
        }

    /** v4'ten 5'e taşıyıp sonucu açık döndürür. */
    private fun tasiVeAc(): SupportSQLiteDatabase {
        v4VeritabaniOlustur().close()
        return yardimci.runMigrationsAndValidate(TEST_DOSYA, 5, true, OdevDatabase.MIGRATION_4_5)
    }

    @Test
    fun surum4ten5eTasinirVeTabloOlusur() {
        tasiVeAc().use { db ->
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='denemeler'")
                .use { imlec ->
                    assertTrue("denemeler tablosu oluşmalı", imlec.moveToFirst())
                }
        }
    }

    @Test
    fun tasinanVeriKaybolmaz() {
        tasiVeAc().use { db ->
            db.query("SELECT baslik, ders FROM odevler").use { imlec ->
                assertTrue(imlec.moveToFirst())
                assertEquals("Matematik 1. ünite", imlec.getString(0))
                assertEquals("Matematik", imlec.getString(1))
            }

            db.query("SELECT ad FROM dersler WHERE olusturmaTarihi = 99").use { imlec ->
                assertTrue(imlec.moveToFirst())
                assertEquals("Fizik", imlec.getString(0))
            }

            db.query("SELECT ders FROM program WHERE gun = 1 AND sira = 1").use { imlec ->
                assertTrue(imlec.moveToFirst())
                assertEquals("Kimya", imlec.getString(0))
            }
        }
    }

    @Test
    fun yeniTabloBosAcilir() {
        tasiVeAc().use { db ->
            db.query("SELECT COUNT(*) FROM denemeler").use { imlec ->
                assertTrue(imlec.moveToFirst())
                assertEquals(0, imlec.getInt(0))
            }
        }
    }

    /**
     * `puan` **tek nullable kolondur**; migration `NOT NULL` koymuş olsaydı
     * kullanıcı puan girmeden kayıt atılamazdı.
     *
     * Bu, migration testlerinin elle yazılan SQL'i yakaladığı yerdir: `NOT
     * NULL` fazlalığı `runMigrationsAndValidate`'in şema karşılaştırmasında
     * fark edilir, ama şema doğru yazılmış olsaydı bile içerik testi bu
     * INSERT'i çalıştırarak ikinci bir güvence sağlar.
     */
    @Test
    fun puanGirilmedenKayitYazilabilir() {
        tasiVeAc().use { db ->
            db.execSQL(
                "INSERT INTO denemeler (ad, tarih, ders, dogru, yanlis, aciklama, olusturmaTarihi) " +
                    "VALUES ('3. Deneme', 0, '', 20, 4, '', 1)",
            )

            db.query("SELECT puan, dogru, yanlis FROM denemeler").use { imlec ->
                assertTrue(imlec.moveToFirst())
                assertTrue("puan NULL olmalı", imlec.isNull(0))
                assertEquals(20, imlec.getInt(1))
                assertEquals(4, imlec.getInt(2))
            }
        }
    }

    /**
     * `runMigrationsAndValidate(..., validateDroppedTables = true)` yalnızca
     * **eklenen** sütunları değil, yanlışlıkla **düşürülen** tabloyu da
     * yakalar: v4'teki ders/program tabloları 5'te hâlâ duruyor olmalı.
     *
     * Yukarıdaki içerik testi bunu zaten dolaylı doğruluyor; bu test
     * niyeti açıkça ortaya koyar.
     */
    @Test
    fun eskiTablolarDusurulmez() {
        tasiVeAc().use { db ->
            db.query(
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' " +
                    "AND name IN ('odevler', 'dersler', 'program')",
            ).use { imlec ->
                assertTrue(imlec.moveToFirst())
                assertEquals(3, imlec.getInt(0))
            }
        }
    }

    private companion object {
        const val TEST_DOSYA = "migration-test.db"
    }
}
