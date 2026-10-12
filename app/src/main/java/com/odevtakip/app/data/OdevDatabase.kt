package com.odevtakip.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Uygulamanın tek veritabanı.
 *
 * Şema sürümü 6. Şema JSON'ları `app/schemas/` altına KSP ile kaydediliyor.
 *
 * ### Sürüm 2 (Faz 12)
 *
 *  - `odevler.ders` kolonu eklendi: mevcut kayıtlar `DEFAULT ''` ile taşmadan
 *    güncellenir (dersi "seçilmedi" olarak okunur).
 *  - `dersler` tablosu açıldı ve **yalnızca bir kez** dolduruldu: dosya yeni
 *    oluşturulduğunda [ILK_OLUSTURMA] callback'i, sürüm 1'den yükseltirken
 *    [MIGRATION_1_2] çalışır. Kullanıcı sonradan bütün dersleri silerse
 *    geri gelmezler — iki yol da "tablo ilk kez oluşuyor" anında çalıştığı
 *    için tekrar-tohumlama sorunu yoktur.
 *
 * ### Sürüm 3 (Faz 14)
 *
 *  - `program` tablosu açıldı: haftanın günleri × ders saatleri ızgarası.
 *    Yeni tablo boş gelir; doldurması kullanıcının işidir. Mevcut ödev ve
 *    ders kayıtları bu geçişte hiç okunmaz/yazılmaz.
 *
 * ### Sürüm 4 (Dosya / fotoğraf eki)
 *
 *  - `odevler.ek` kolonu eklendi: `TEXT` ve **boş (`NULL`)**. Dosyanın
 *    kendisi veritabanında değil, uygulamanın özel dosya deposunda durur
 *    ([EkDeposu]); tabloda yalnızca adı tutulur. Eski kayıtlar bu kolonu
 *    `NULL` olarak alır, yani "ek yok" — taşma ya da dönüşüm yok.
 *
 * ### Sürüm 5 (Deneme sınavları)
 *
 *  - `denemeler` tablosu açıldı: ad, tarih, ders, doğru/yanlış, puan ve not.
 *    Yeni tablo boş gelir, doldurması kullanıcının işidir. Mevcut ödev,
 *    ders ve program kayıtları bu geçişte hiç okunmaz/yazılmaz — yalnızca
 *    yeni bir `CREATE TABLE` çalışır.
 *
 * ### Sürüm 6 (TYT / AYT denemeleri)
 *
 *  - `denemeler` **yeniden kuruldu**. Tek "ders" alanının yerini sınavın
 *    **kendi formatı** aldı: `tur` (TYT/AYT) ve `sonuclar` (ders bazlı
 *    doğru/yanlış). `ders` kolonu kaldırıldı — deneme artık bir dersin
 *    değil, sınavın kaydıdır.
 *
 *  - `dogru` ve `yanlis` kolon olmaktan çıktı: artık [Deneme.sonuclar]'ın
 *    toplamıdır. Tıpkı `net` gibi — iki yerdeki sayı asla çelişemesin.
 *
 *  - **Eski deneme kayıtları silinir.** Bu bilinçli bir karardır: deneme
 *    tablosu hiçbir yayımlanmış sürümde yer almadı (v1.4 denemelerden önce
 *    çıktı), yani silinen gerçek kullanıcı verisi yoktur.
 *
 *  - Tablo `DROP` + `CREATE` ile kurulur, `ALTER TABLE` ile değil. İki
 *    neden: (1) eski satırlar silineceğinden taşınacak bir şey yoktur;
 *    (2) `ALTER TABLE ... ADD COLUMN ... NOT NULL` eski satırlara `NULL`
 *    yazamayacağı için her yeni kolona bir `DEFAULT` dayatır — yeniden
 *    kurmak hem daha kısa hem de sonucu daha nettir. Sütun tanımları
 *    [MIGRATION_5_6] içinde Room'un üreteceği şema ile **birebir** aynı
 *    olmak zorundadır (bkz. [MIGRATION_4_5] notları).
 *
 *  - Ödev, ders ve program tabloları bu geçişte hiç okunmaz/yazılmaz.
 */
@Database(
    entities = [Odev::class, Ders::class, ProgramSatiri::class, Deneme::class],
    version = 6,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class OdevDatabase : RoomDatabase() {

    abstract fun odevDao(): OdevDao

    abstract fun dersDao(): DersDao

    abstract fun programDao(): ProgramDao

    abstract fun denemeDao(): DenemeDao

    companion object {

        /**
         * İlk açılışta hazır gelen dersler.
         *
         * Değer **ekleme sırası**dır (`olusturmaTarihi`): liste ekleme
         * sırasına göre dizilir, sonradan eklenenler en alta düşer.
         */
        private val VARSAYILAN_DERSLER = listOf(
            "Matematik",
            "Türkçe",
            "Fen Bilimleri",
            "İngilizce",
            "Tarih",
            "Coğrafya",
            "Fizik",
            "Kimya",
            "Biyoloji",
        )

        /**
         * Varsayılan dersleri yazar; tablo zaten doluysa hiçbir şey yapmaz.
         *
         * Hem migration hem `onCreate` callback'i bu fonksiyonu çağırır. İki
         * yol da yalnızca tablonun **ilk oluştuğu** anda çalıştığı için aynı
         * açılışta iki kez çağırmak yine de güvenlidir (sayım koruması).
         */
        private fun varsayilanDersleriBasla(db: SupportSQLiteDatabase) {
            val mevcut = db.query("SELECT COUNT(*) FROM dersler").use { imlec ->
                if (imlec.moveToFirst()) imlec.getInt(0) else 0
            }
            if (mevcut > 0) return

            VARSAYILAN_DERSLER.forEachIndexed { sira, ad ->
                // sira 1..9; yeni eklenen dersler currentTimeMillis aldığı için
                // ekleme sırası bozulmadan listenin sonuna geçer.
                db.execSQL(
                    "INSERT INTO dersler (ad, olusturmaTarihi) VALUES (?, ?)",
                    arrayOf<Any>(ad, sira.toLong() + 1L),
                )
            }
        }

        /** Sürüm 1 → 2: ders kolonu + dersler tablosu. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE odevler ADD COLUMN ders TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `dersler` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`ad` TEXT NOT NULL, " +
                        "`olusturmaTarihi` INTEGER NOT NULL)"
                )
                varsayilanDersleriBasla(db)
            }
        }

        /** Sürüm 2 → 3: ders programı ızgarası (Faz 14). */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Birleşik anahtar: aynı gün/saat ikinci kez yazılamaz, sadece
                // mevcut satırın ders adı değişir (ProgramDao).
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `program` (" +
                        "`gun` INTEGER NOT NULL, " +
                        "`sira` INTEGER NOT NULL, " +
                        "`ders` TEXT NOT NULL, " +
                        "PRIMARY KEY(`gun`, `sira`))"
                )
            }
        }

        /**
         * Sürüm 3 → 4: ödevin ek dosyasının adı (Faz: dosya / fotoğraf eki).
         *
         * `ALTER TABLE` kolonu `NULL` değerle açar; dolayısıyla mevcut bütün
         * kayıtlar "ek yok" olarak okunur ve hiçbir satır güncellenmez.
         * `DEFAULT` bilinçli olarak **verilmiyor**: Room'un beklediği şema ile
         * birebir uyuşsun diye kolon yalın `TEXT` kalır (bkz. [Odev.ek]).
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE odevler ADD COLUMN ek TEXT")
            }
        }

        /**
         * Sürüm 4 → 5: deneme sınavları tablosu.
         *
         * **Neden `internal`:** migration testi bunu doğrudan okuyup
         * `MigrationTestHelper`a verir. `private` olsaydı test ya da XML'e
         * gömülü bir kopya üretmek zorunda kalırdı — ikisi de testin
         * **gerçekten yürüttüğü kodu** ölçme garantisini zayıflatırdı.
         * `internal` yalnızca modül içinden erişilebilir, yani çalışma
         * zamanında dışarı açılmaz.
         *
         * Yeni tablo **boş açılır**; mevcut hiçbir satır okunmaz/yazılmaz.
         * Bu bilerek en ucuz yoldur: taşıma gereken bir şey yok, dolayısıyla
         * taşımada hata yapılabilecek bir yer de yok.
         *
         * Sütun tanımları Room'un üreteceği şema ile **birebir** aynı olmak
         * zorundadır — migration'da tek bir ekstra `DEFAULT` ya da eksik
         * `NOT NULL` bile açılışta `Migration didn't properly handle` ile
         * çöküşe yol açar:
         *
         *  - `puan` tek nullable alandır (`Double?` → `REAL`, `NOT NULL`
         *    yok): kullanıcı puanı girmediyse hücre `NULL` kalır.
         *  - `ad`, `tarih` gibi alanlar `NOT NULL`; entity'de de varsayılanı
         *    yok ya da zorunlu.
         *  - `net` **kolon değildir** — [Deneme.net] hesaplanmış bir özellik
         *    olduğu için Room onu şemaya dahil etmez.
         */
        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `denemeler` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`ad` TEXT NOT NULL, " +
                        "`tarih` INTEGER NOT NULL, " +
                        "`ders` TEXT NOT NULL, " +
                        "`dogru` INTEGER NOT NULL, " +
                        "`yanlis` INTEGER NOT NULL, " +
                        "`puan` REAL, " +
                        "`aciklama` TEXT NOT NULL, " +
                        "`olusturmaTarihi` INTEGER NOT NULL)"
                )
            }
        }

        /**
         * Sürüm 5 → 6: denemeler TYT/AYT biçimine geçer.
         *
         * Tablo **düşürülüp yeniden kurulur** (bkz. sınıf açıklaması,
         * Sürüm 6): `tur` ve `sonuclar` kolonları `NOT NULL` olduğu için
         * `ALTER TABLE` eski satırlara yazacak bir şey bulamazdı.
         *
         * [MIGRATION_4_5]in `internal` olmasının nedeni (testin bunu
         * doğrudan okuması) burada da geçerlidir: migration testi bu
         * değeri okuyup `MigrationTestHelper`a verir.
         *
         * `ders`, `dogru` ve `yanlis` kolonları bilinçli olarak **yoktur**:
         * birincisi anlamını yitirdi, ikincisi artık [Deneme.sonuclar]'ın
         * toplamıdır.
         */
        internal val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `denemeler`")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `denemeler` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`ad` TEXT NOT NULL, " +
                        "`tarih` INTEGER NOT NULL, " +
                        "`tur` TEXT NOT NULL, " +
                        "`sonuclar` TEXT NOT NULL, " +
                        "`puan` REAL, " +
                        "`aciklama` TEXT NOT NULL, " +
                        "`olusturmaTarihi` INTEGER NOT NULL)"
                )
            }
        }

        /**
         * Dosya ilk kez oluşturulurken (yükseltme değil, taze kurulum) çağrılır.
         *
         * Room bu callback'i tabloları oluşturduktan sonra çalıştırır; migration
         * gereken durumlarda ise dosya zaten var olduğu için hiç çalışmaz —
         * varsayılanlar o durumu [MIGRATION_1_2] yazar.
         */
        private val ILK_OLUSTURMA = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                varsayilanDersleriBasla(db)
            }
        }

        @Volatile
        private var INSTANCE: OdevDatabase? = null

        fun getInstance(context: Context): OdevDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    OdevDatabase::class.java,
                    "odev-takip.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                    )
                    .addCallback(ILK_OLUSTURMA)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
