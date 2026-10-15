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
 * Şema sürümü 9. Şema JSON'ları `app/schemas/` altına KSP ile kaydediliyor.
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
 *
 * ### Sürüm 7 (Puan alanı kaldırıldı)
 *
 *  - `denemeler`ten `puan` kolonu silindi. Puan alanı kullanıcı tarafından
 *    giriliyordu ve uygulamanın asıl işi olan "sorular + netler" kaydından
 *    bağımsız bir değeri temsil ediyordu — kaldırılarak deneme kaydı
 *    **yalnızca sınavın kendisine** indirgendi.
 *
 *  - Yol, `ALTER TABLE DROP COLUMN` yerine **yeni tablo + taşı + ad
 *    değiştirme**: `DROP COLUMN` her SQLite sürümünde bulunmaz (Android'in
 *    SQLite'ı cihaza göre değişir). Fark [MIGRATION_5_6] ile burası
 *    arasındaki tek önemli farktır: orada kayıtlar biçimsel olarak
 *    taşınamazdı ve bilerek silindi; burada silinecek bir şey yok, yalnızca
 *    `puan` atılır — **diğer bütün sütunlar aynen taşınır.**
 *
 *  - Sütun tanımları [MIGRATION_6_7] içinde [Deneme] entity'siyle **birebir**
 *    aynı olmak zorundadır (bkz. [MIGRATION_4_5] notları).
 *
 * ### Sürüm 8 (Ders rutini)
 *
 *  - `rutin_kayitlari` tablosu açıldı: bir günde bir dersin çözülen
 *    sorularının dökümü (`tarih`, `ders`, `soru`, `dogru`, `yanlis`).
 *    Ekran haftalık ya da aylık bakar; kaydın özü "o gün bu dersten ne
 *    çözüldü"dür.
 *
 *  - **Unique indeks `(tarih, ders)`** — tablonun tek gerçek kuralı. Bir
 *    günde bir dersten yalnızca bir satır durur; ikinci kez girilen
 *    değer satırı **değiştirir**, ikinci satır açmaz. Böylece gün
 *    toplamı iki katma çıkmaz. *(Bu kural 9. sürümde kaldırıldı:
 *    aynı dersten çok kayıt, başlıkla ayrıştırılarak serbest bırakıldı.)*
 *
 *  - Yalnızca **ekleme**: mevcut hiçbir tablo okunmaz ya da yazılmaz.
 *    `DROP`+`CREATE` gibi bir yol gerekmedi (bkz. [MIGRATION_7_8]).
 *
 * ### Sürüm 9 (Rutin başlığı, aynı dersten çok kayıt)
 *
 *  - `rutin_kayitlari.baslik` kolonu eklendi: aynı günün aynı dersinden
 *    **birden fazla** kaydı birbirinden ayıran isteğe bağlı etiket
 *    (ör. "Matematik 2"). `DEFAULT ''` ile eklenir ve migration
 *    **hiçbir satıra dokunmaz** — eski kayıtlar `''` alır, kartta
 *    oldukları gibi ders adını gösterir, hiçbir veri silinmez ya da
 *    yeniden yazılmaz (bkz. [MIGRATION_8_9]).
 *
 *  - **`(tarih, ders)` unique indeksi kaldırılır.** Sürüm 8'in tek
 *    kuralıydı; sürüm 9'da bilinçli olarak terk edilir, çünkü kullanıcı
 *    aynı dersi ikinci kez girdiğinde artık **ayrı bir satır** ister —
 *    eski kaydın sorularının sessizce değişmesi yerine ikinci kayıt
 *    açılacaktır. Gün toplamı yine doğrudur: her satır kendi
 *    çözümünün dökümüdür, ikisi de toplama girer.
 *
 *  - Yol saf `ALTER TABLE` + `DROP INDEX`'tir: tablo yeniden kurulmaz,
 *    satırlar kopyalanmaz, tek bir hücre bile okunmaz/yazılmaz. Eski
 *    kayıtları koruma sözünün en güçlü hâlidir.
 */
@Database(
    entities = [Odev::class, Ders::class, ProgramSatiri::class, Deneme::class, RutinKaydi::class],
    version = 9,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class OdevDatabase : RoomDatabase() {

    abstract fun odevDao(): OdevDao

    abstract fun dersDao(): DersDao

    abstract fun programDao(): ProgramDao

    abstract fun denemeDao(): DenemeDao

    abstract fun rutinDao(): RutinDao

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
         * Sürüm 6 → 7: `denemeler`ten `puan` kolonu kaldırılır.
         *
         * Puan, kullanıcının elle girdiği ve deneme kaydının özünden
         * (sorular + netler) ayrı bir değerdi; artık yok. Tablo, `ALTER
         * TABLE ... DROP COLUMN` her SQLite sürümünde bulunmadığı için
         * **yeniden kurulur** — ama bu sefer eski kayıtlar **taşınır**:
         * yalnızca `puan` kolonu atlanır, `id`'den `olusturmaTarihi`'ne
         * kadar kalan her sütun aynen yazılır (bkz. sınıf açıklaması,
         * Sürüm 7).
         *
         * [MIGRATION_5_6] ile aynı `internal` gerekçesi burada da geçerli:
         * migration testi bunu doğrudan okur.
         *
         * Yeni tablonun sütun tanımları [Deneme] entity'siyle **birebir**
         * aynı olmalıdır — Room açılışta ikisini karşılaştırır.
         */
        internal val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `denemeler_yeni` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`ad` TEXT NOT NULL, " +
                        "`tarih` INTEGER NOT NULL, " +
                        "`tur` TEXT NOT NULL, " +
                        "`sonuclar` TEXT NOT NULL, " +
                        "`aciklama` TEXT NOT NULL, " +
                        "`olusturmaTarihi` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO `denemeler_yeni` " +
                        "(`id`, `ad`, `tarih`, `tur`, `sonuclar`, `aciklama`, " +
                        "`olusturmaTarihi`) " +
                        "SELECT `id`, `ad`, `tarih`, `tur`, `sonuclar`, " +
                        "`aciklama`, `olusturmaTarihi` FROM `denemeler`"
                )
                db.execSQL("DROP TABLE `denemeler`")
                db.execSQL("ALTER TABLE `denemeler_yeni` RENAME TO `denemeler`")
            }
        }

        /**
         * Sürüm 7 → 8: ders rutini tablosu açılır.
         *
         * Yeni tablo **yalnızca eklenir**; mevcut hiçbir tablo okunmaz,
         * yazılmaz ya da yeniden kurulmaz. Bu, migration'ların en ucuz
         * ve en güvenli türüdür — Room da açılışta yalnızca yeni
         * tabloyu bekler.
         *
         * Unique indeks bilinçlidir ve tek gerçek karardır: `(tarih,
         * ders)` çifti bir kez bulunur. Böylece kullanıcı aynı dersi aynı
         * gün ikinci kez girerse ayrı bir satır değil, **mevcut satır
         * değişir** ([MIGRATION_6_7]'deki tablo yeniden kurma işi gibi
         * veri kaybı olmaz; yalnızca bir hücre güncellenir). Kartta iki
         * "Matematik" satırı belmez ve gün toplamı iki katma çıkmaz.
         *
         * [MIGRATION_5_6] ile başlayan `internal` gerekçesi burada da
         * geçerli: migration testi bunu doğrudan okur.
         *
         * Sütun tanımları [RutinKaydi] entity'siyle **birebir** aynı
         * olmalıdır — Room açılışta ikisini karşılaştırır.
         */
        internal val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `rutin_kayitlari` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`tarih` INTEGER NOT NULL, " +
                        "`ders` TEXT NOT NULL, " +
                        "`soru` INTEGER NOT NULL, " +
                        "`dogru` INTEGER NOT NULL, " +
                        "`yanlis` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_rutin_kayitlari_tarih_ders` " +
                        "ON `rutin_kayitlari` (`tarih`, `ders`)"
                )
            }
        }

        /**
         * Sürüm 8 → 9: `baslik` kolonu eklenir, `(tarih, ders)` unique
         * indeksi kaldırılır.
         *
         * İki SQL, **tek bir satır okunmadan ya da yazılmadan**:
         *
         *  - `ALTER TABLE ... ADD COLUMN` sütunu sona ekler; `DEFAULT ''`
         *    sayesinde migration'ın çalıştırılması gerekmez — mevcut
         *    bütün satırlar `''` alır ve kartta eskisi gibi yalnız
         *    [RutinKaydi.ders] adını gösterir. `DEFAULT` bilinçli ve
         *    zorunludur: `NOT NULL` sütun SQLite'ta sabit bir varsayılan
         *    olmadan eklenemez. Entity'deki
         *    `@ColumnInfo(defaultValue = "''")` ile birebir aynıdır.
         *
         *  - `DROP INDEX` aynı günün aynı dersinin ikinci kez
         *    yazılamaması kuralını kaldırır — sürüm 9'un sözü budur:
         *    ikinci "Matematik" artık ayrı bir satır, eski kaydın
         *    **değişmesi** değil. İndeks adı [MIGRATION_7_8] içinde
         *    birebir yazıldığı için `IF EXISTS` ile emniyete alınır.
         *
         * Tablonun kendisi yeniden kurulmaz, satırlar kopyalanmaz: eski
         * kayıtlar dosyada oldukları yerde, aynen durur (bkz. sınıf
         * açıklaması, Sürüm 9).
         *
         * [MIGRATION_5_6] ile başlayan `internal` gerekçesi burada da
         * geçerli: migration testi bunu doğrudan okur.
         */
        internal val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE rutin_kayitlari ADD COLUMN `baslik` TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL("DROP INDEX IF EXISTS `index_rutin_kayitlari_tarih_ders`")
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
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                    )
                    .addCallback(ILK_OLUSTURMA)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
