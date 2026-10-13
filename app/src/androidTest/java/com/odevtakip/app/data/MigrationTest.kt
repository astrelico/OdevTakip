package com.odevtakip.app.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * kolon → kullanıcı ilk kaydı atınca çökme). Bu test, gerçek bir v4 ya da
 * v5 veritabanı açıp güncel sürüme taşıyıp **içeriği de** okuyarak ikisini
 * birden kapatır. Üç geçiş de ayrıca sıralanmıştır: 4 → 5 tabloyu
 * açar, 5 → 6 onu TYT/AYT biçimine geçirir (ve eski deneme satırlarını
 * bilerek siler), 6 → 7 `puan` kolonunu kaldırır — kaldırmadan önce
 * diğer bütün sütunları yeni tabloya taşır.
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

    // ---- Sürüm 5 → 6: TYT / AYT denemeleri ----

    /**
     * v5 tablolarını kurar ve **eski biçimde** bir deneme yazar.
     *
     * Yazılan satır bilerek eski şemanın bütün alanlarını kullanır
     * (`ders`, `dogru`, `yanlis`): migration bu kaydı **silmesi**
     * gerektiğinin kanıtı, silinen şeyin ne olduğunun da.
     */
    private fun v5VeritabaniOlustur(): SupportSQLiteDatabase =
        yardimci.createDatabase(TEST_DOSYA, 5).also { db ->
            db.execSQL(
                "INSERT INTO odevler (baslik, aciklama, ders, sonTarih, durum, olusturmaTarihi) " +
                    "VALUES ('Matematik 1. ünite', 'sayfa 12', 'Matematik', ?, 'BEKLIYOR', ?)",
                arrayOf(2_000_000_000_000L, 1_700_000_000_000L),
            )
            db.execSQL(
                "INSERT INTO denemeler (ad, tarih, ders, dogru, yanlis, aciklama, olusturmaTarihi) " +
                    "VALUES ('Eski Deneme', 0, 'Matematik', 20, 4, '', 1)",
            )
        }

    /** v5'ten 6'ya taşıyıp sonucu açık döndürür. */
    private fun tasiVeAc6(): SupportSQLiteDatabase {
        v5VeritabaniOlustur().close()
        return yardimci.runMigrationsAndValidate(TEST_DOSYA, 6, true, OdevDatabase.MIGRATION_5_6)
    }

    /**
     * Eski deneme kayıtları **silinir**.
     *
     * Bu, migration'ın bilerek verdiği bir kayıptır (bkz.
     * [OdevDatabase] sürüm 6 notları): deneme tablosu hiçbir yayımlanmış
     * sürümde yer almadı, yani gerçek kullanıcı verisi yoktur. Yeni
     * biçimde eski kaydı "taşımak" anlamsızdı — tek ders + toplam doğru,
     * ders bazlı bir sonuca geri dönülemez.
     */
    @Test
    fun eskiDenemeKayitlariSilinir() {
        tasiVeAc6().use { db ->
            db.query("SELECT COUNT(*) FROM denemeler").use { imlec ->
                assertTrue(imlec.moveToFirst())
                assertEquals(0, imlec.getInt(0))
            }
        }
    }

    /** Yeni kolonlar vardır, kaldırılanlar yoktur. */
    @Test
    fun sutunlarYeniBicimeGecer() {
        tasiVeAc6().use { db ->
            db.query("PRAGMA table_info(denemeler)").use { imlec ->
                val sutunlar = mutableListOf<String>()
                while (imlec.moveToNext()) sutunlar.add(imlec.getString(1))

                assertTrue("tur kolonu eklenmeli", "tur" in sutunlar)
                assertTrue("sonuclar kolonu eklenmeli", "sonuclar" in sutunlar)
                assertFalse("ders kolonu kaldırılmalı", "ders" in sutunlar)
                assertFalse("dogru kolonu kaldırılmalı", "dogru" in sutunlar)
                assertFalse("yanlis kolonu kaldırılmalı", "yanlis" in sutunlar)
            }
        }
    }

    /**
     * Denemeler dışında hiçbir tablo **okunmaz/yazılmaz**.
     *
     * Migration'ın `DROP` + `CREATE` yolu yalnızca `denemeler` içindir;
     * ödev kaydı aynen kalmalıdır.
     */
    @Test
    fun digerTablolarEtkilenmez() {
        tasiVeAc6().use { db ->
            db.query("SELECT baslik, ders FROM odevler").use { imlec ->
                assertTrue(imlec.moveToFirst())
                assertEquals("Matematik 1. ünite", imlec.getString(0))
                assertEquals("Matematik", imlec.getString(1))
            }
        }
    }

    /**
     * Yeni şema **kullanılabilir** olmalıdır: `tur` ve `sonuclar`
     * yazılabilir, `puan` yine isteğe bağlıdır.
     *
     * `sonuclar` metni [sonuclariYaz] biçimindedir — ham hâliyle
     * yazılır ve okunur, çünkü migration testi Room'un Java katmanını
     * değil, **tablonun kendisini** sınamaktadır.
     */
    @Test
    fun yeniSemaUzerindeKayitYazilabilir() {
        tasiVeAc6().use { db ->
            db.execSQL(
                "INSERT INTO denemeler (ad, tarih, tur, sonuclar, aciklama, olusturmaTarihi) " +
                    "VALUES ('3. TYT', 0, 'TYT', 'Türkçe=35/4;Sosyal=12/3', '', 1)",
            )

            db.query("SELECT tur, sonuclar, puan FROM denemeler").use { imlec ->
                assertTrue(imlec.moveToFirst())
                assertEquals("TYT", imlec.getString(0))
                assertEquals("Türkçe=35/4;Sosyal=12/3", imlec.getString(1))
                assertTrue("puan yine NULL olabilmeli", imlec.isNull(2))
            }
        }
    }

    // ---- Sürüm 6 → 7: puan kolonu kaldırılır ----

    /**
     * v6 tablolarını kurar ve **puanı dolu** bir deneme yazar.
     *
     * Yazılan satır, v6'ya ait bütün sütunları kullanır; puan'a bir de
     * değer verilir ki migration sonrası **kaybolup kaybolmadığı** ve
     * kalan sütunların **taşınıp taşınmadığı** aynı testte görünsün.
     */
    private fun v6VeritabaniOlustur(): SupportSQLiteDatabase =
        yardimci.createDatabase(TEST_DOSYA, 6).also { db ->
            db.execSQL(
                "INSERT INTO odevler (baslik, aciklama, ders, sonTarih, durum, olusturmaTarihi) " +
                    "VALUES ('Matematik 1. ünite', 'sayfa 12', 'Matematik', ?, 'BEKLIYOR', ?)",
                arrayOf(2_000_000_000_000L, 1_700_000_000_000L),
            )
            db.execSQL(
                "INSERT INTO denemeler " +
                    "(ad, tarih, tur, sonuclar, puan, aciklama, olusturmaTarihi) " +
                    "VALUES ('2. TYT', 1700000000000, 'TYT', 'Türkçe=36/3;Matematik=30/5', 84.25, 'iyi gitti', 77)",
            )
        }

    /** v6'dan 7'ye taşıyıp sonucu açık döndürür. */
    private fun tasiVeAc7(): SupportSQLiteDatabase {
        v6VeritabaniOlustur().close()
        return yardimci.runMigrationsAndValidate(TEST_DOSYA, 7, true, OdevDatabase.MIGRATION_6_7)
    }

    /** `puan` kolonu tablodan tamamen kalkar; diğerleri durur. */
    @Test
    fun puanKolonuKaldirilir() {
        tasiVeAc7().use { db ->
            db.query("PRAGMA table_info(denemeler)").use { imlec ->
                val sutunlar = mutableListOf<String>()
                while (imlec.moveToNext()) sutunlar.add(imlec.getString(1))

                assertFalse("puan kolonu kaldırılmalı", "puan" in sutunlar)
                assertTrue("ad kalmalı", "ad" in sutunlar)
                assertTrue("tur kalmalı", "tur" in sutunlar)
                assertTrue("sonuclar kalmalı", "sonuclar" in sutunlar)
                assertTrue("aciklama kalmalı", "aciklama" in sutunlar)
                assertTrue("olusturmaTarihi kalmalı", "olusturmaTarihi" in sutunlar)
            }
        }
    }

    /**
     * `puan` hariç **her sütun aynen taşınır**.
     *
     * Bu, 5 → 6 ile 6 → 7 arasındaki farkın kanıtıdır: orada tablo
     * bilerek boş kurulmuştu, burada kayıp yoktur. `id`'nin de taşınması
     * özellikle önemlidir — silinseymiş gibi görünse bile detay
     * ekranındaki derin bağlantı o kimlikle açılır.
     */
    @Test
    fun kayitlarPuanHariciAynenTasinir() {
        tasiVeAc7().use { db ->
            db.query(
                "SELECT id, ad, tarih, tur, sonuclar, aciklama, olusturmaTarihi FROM denemeler",
            ).use { imlec ->
                assertTrue("kayıt taşınmalı", imlec.moveToFirst())
                assertEquals(1L, imlec.getLong(0))
                assertEquals("2. TYT", imlec.getString(1))
                assertEquals(1700000000000L, imlec.getLong(2))
                assertEquals("TYT", imlec.getString(3))
                assertEquals("Türkçe=36/3;Matematik=30/5", imlec.getString(4))
                assertEquals("iyi gitti", imlec.getString(5))
                assertEquals(77L, imlec.getLong(6))
            }

            // Taşınan kayıtla yeni kayıt yan yana yazılabilir; kimlik
            // çakışması yoktur (AUTOINCREMENT devralınmış olmalı).
            db.execSQL(
                "INSERT INTO denemeler (ad, tarih, tur, sonuclar, aciklama, olusturmaTarihi) " +
                    "VALUES ('3. TYT', 0, 'TYT', '', '', 88)",
            )
            db.query("SELECT COUNT(*) FROM denemeler").use { imlec ->
                assertTrue(imlec.moveToFirst())
                assertEquals(2, imlec.getInt(0))
            }
        }
    }

    /**
     * Denemeler dışındaki tablolar bu geçişte de **okunmaz/yazılmaz**.
     *
     * `MIGRATION_6_7` yalnızca `denemeler` üzerinde dolaştığı için ödev
     * kaydı aynen kalmalıdır.
     */
    @Test
    fun digerTablolar6dan7deEtkilenmez() {
        tasiVeAc7().use { db ->
            db.query("SELECT baslik, ders FROM odevler").use { imlec ->
                assertTrue(imlec.moveToFirst())
                assertEquals("Matematik 1. ünite", imlec.getString(0))
                assertEquals("Matematik", imlec.getString(1))
            }
        }
    }

    // ---- Sürüm 7 → 8: ders rutini tablosu açılır ----

    /**
     * v7 tablolarını kurar ve **bir deneme** yazar.
     *
     * Denemenin taşınması bu geçişin asıl sözüdür: `MIGRATION_7_8`
     * yalnızca **ekleme** yapar, mevcut hiçbir tabloya dokunmaz. Yazılan
     * satır, geçişten sonra **aynen** durmalıdır.
     */
    private fun v7VeritabaniOlustur(): SupportSQLiteDatabase =
        yardimci.createDatabase(TEST_DOSYA, 7).also { db ->
            db.execSQL(
                "INSERT INTO denemeler (ad, tarih, tur, sonuclar, aciklama, olusturmaTarihi) " +
                    "VALUES ('2. TYT', 1700000000000, 'TYT', 'Türkçe=36/3', 'iyi gitti', 77)",
            )
        }

    /** v7'den 8'e taşıyıp sonucu açık döndürür. */
    private fun tasiVeAc8(): SupportSQLiteDatabase {
        v7VeritabaniOlustur().close()
        return yardimci.runMigrationsAndValidate(TEST_DOSYA, 8, true, OdevDatabase.MIGRATION_7_8)
    }

    /**
     * Tablo, entity'deki **sırayla** aynı sütunlarla açılır.
     *
     * Sıra bilerek kontrol edilir: Room açılışta şemayı karşılaştırırken
     * sütun adlarını değil, tanımı okur. Bir sütunun yer değiştirmesi
     * migration'ın geçerli kalmasına rağmen açılışta "şema eşleşmiyor"
     * hatasına yol açabilirdi.
     */
    @Test
    fun rutinTablosuSutunlariylaAcilir() {
        tasiVeAc8().use { db ->
            db.query("PRAGMA table_info(rutin_kayitlari)").use { imlec ->
                val sutunlar = mutableListOf<String>()
                while (imlec.moveToNext()) sutunlar.add(imlec.getString(1))

                assertEquals(
                    listOf("id", "tarih", "ders", "soru", "dogru", "yanlis"),
                    sutunlar,
                )
            }
        }
    }

    /**
     * `(tarih, ders)` çifti **bir kez** bulunur — tablonun tek kuralı.
     *
     * Bu indeks olmasaydı kullanıcı aynı dersi ikinci kez girdiğinde iki
     * satır belirir ve gün toplamı iki katma çıkardı. Kuralın
     * veritabanında durduğu, uygulama koduna güvenilmeden burada
     * sınanır.
     */
    @Test
    fun ayniGununAyniDersiIkinciKezYazilamaz() {
        tasiVeAc8().use { db ->
            db.execSQL(
                "INSERT INTO rutin_kayitlari (tarih, ders, soru, dogru, yanlis) " +
                    "VALUES (100, 'Matematik', 40, 35, 5)",
            )

            var kuralIhlali = false
            try {
                db.execSQL(
                    "INSERT INTO rutin_kayitlari (tarih, ders, soru, dogru, yanlis) " +
                        "VALUES (100, 'Matematik', 20, 15, 5)",
                )
            } catch (e: SQLiteConstraintException) {
                kuralIhlali = true
            }

            assertTrue("aynı (gün, ders) ikinci kez yazılamaz", kuralIhlali)
            db.query("SELECT COUNT(*) FROM rutin_kayitlari").use { imlec ->
                assertTrue(imlec.moveToFirst())
                assertEquals(1, imlec.getInt(0))
            }
        }
    }

    /**
     * Kural **yalnızca** aynı çift içindir: aynı günün farklı dersi ve
     * aynı dersten farklı gün serbestçe yazılır.
     *
     * Birinci testin sınırını çizer — indeks, "bir günde tek satır"
     * değil "bir günde bir dersten tek satır" demektir.
     */
    @Test
    fun farkliDersVeFarkliGunSerbesttir() {
        tasiVeAc8().use { db ->
            listOf(
                "INSERT INTO rutin_kayitlari (tarih, ders, soru, dogru, yanlis) VALUES (100, 'Matematik', 40, 35, 5)",
                "INSERT INTO rutin_kayitlari (tarih, ders, soru, dogru, yanlis) VALUES (100, 'Fizik', 20, 15, 3)",
                "INSERT INTO rutin_kayitlari (tarih, ders, soru, dogru, yanlis) VALUES (200, 'Matematik', 10, 8, 2)",
            ).forEach(db::execSQL)

            db.query("SELECT COUNT(*) FROM rutin_kayitlari").use { imlec ->
                assertTrue(imlec.moveToFirst())
                assertEquals(3, imlec.getInt(0))
            }
        }
    }

    /**
     * Mevcut tablolar bu geçişte **okunmaz, yazılmaz, yeniden kurulmaz**.
     *
     * 5 → 6'nın satırları bilerek silmesiyle farkı tam olarak budur: burada
     * kayıp yoktur. Yazılan deneme aynen durur ve yeni tablo baştan boştur.
     */
    @Test
    fun mevcutTablolar7den8deEtkilenmez() {
        tasiVeAc8().use { db ->
            db.query("SELECT ad, tarih, tur, sonuclar, aciklama, olusturmaTarihi FROM denemeler")
                .use { imlec ->
                    assertTrue("deneme taşınmalı", imlec.moveToFirst())
                    assertEquals("2. TYT", imlec.getString(0))
                    assertEquals(1700000000000L, imlec.getLong(1))
                    assertEquals("TYT", imlec.getString(2))
                    assertEquals("Türkçe=36/3", imlec.getString(3))
                    assertEquals("iyi gitti", imlec.getString(4))
                    assertEquals(77L, imlec.getLong(5))
                }

            db.query("SELECT COUNT(*) FROM rutin_kayitlari").use { imlec ->
                assertTrue(imlec.moveToFirst())
                assertEquals("yeni tablo baştan boştur", 0, imlec.getInt(0))
            }
        }
    }

    private companion object {
        const val TEST_DOSYA = "migration-test.db"
    }
}
