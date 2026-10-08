package com.odevtakip.app.data

import android.net.Uri
import java.io.File
import kotlinx.coroutines.flow.Flow

/**
 * Odev verisine tek giriş noktası.
 *
 * ViewModel'ler doğrudan [OdevDao] yerine bu sınıfı görür; böylece iş kuralları
 * (ne zaman "gecikti", ne zaman "tamamlandı") tek yerde toplanır ve
 * arayüz ile WorkManager aynı kuralı paylaşır.
 *
 * Ders listesi de buradan geçer: bir dersin eklenip silinmesi iş kuralı
 * (yinelenen ad) barındırdığı için arayüzün doğrudan [DersDao] görmemesi gerekir.
 *
 * Aynı doğruluk dosya eki için de geçerli: ek ne zaman kopyalanır, ne zaman
 * kimse ona referans vermediği için silinir — hepsi bu sınıfın içindedir.
 */
class OdevRepository(
    private val dao: OdevDao,
    private val dersDao: DersDao,
    private val programDao: ProgramDao,
    private val ekDeposu: EkDeposu,
) {

    // ---- Dosya / fotoğraf eki ----

    /** Seçicinin verdiği Uri'nin kullanıcının gördüğü dosya adını okur. */
    suspend fun ekAdi(uri: Uri): String? = ekDeposu.adiniOku(uri)

    /**
     * Dosyayı uygulamanın deposuna kopyalar; başarısızsa `null`.
     *
     * Kopyalama yalnızca **kayıt anında** yapılır: formdan vazgeçildiğinde
     * geriye hiçbir artık dosya kalmaz.
     */
    suspend fun ekKopyala(uri: Uri): String? = ekDeposu.kopyala(uri)

    /**
     * Depodaki mevcut bir ekin **bağımsız kopyasını** oluşturur; başarısızsa
     * `null`.
     *
     * Ödev kopyalanırken kullanılır: iki kayıt aynı dosya adını taşısaydı,
     * birini silmek ötekinin ekini de silerdi.
     */
    suspend fun ekKopyalaMevcut(ad: String): String? = ekDeposu.kopyalaMevcut(ad)

    /** Ek dosyasının tam yolu — detay ekranındaki önizleme için. */
    fun ekDosyasi(ad: String): File = ekDeposu.dosya(ad)

    /** [ad]lı ek dosyasını siler. Dosya yoksa sessizce biter. */
    suspend fun ekSil(ad: String) = ekDeposu.sil(ad)

    // ---- Ders programı ----

    /** Haftalık programı gün ve sıra sırasıyla yayınlar. */
    fun programi(): Flow<List<ProgramSatiri>> = programDao.programiIzle()

    /**
     * Bir ders saatine ders atar.
     *
     * @param ders Seçilen dersin adı; boşsa o saat boşa alınır. Satır
     *   silinmez — bkz. [ProgramSatiri].
     */
    suspend fun programaYaz(gun: Int, sira: Int, ders: String) {
        gunuHazirla(gun)
        programDao.kaydet(ProgramSatiri(gun = gun, sira = sira, ders = ders.trim()))
    }

    /**
     * Seçili güne bir ders saati daha açar.
     *
     * Sıra numarası **tablodan** okunur ([ProgramDao.gununSonSirasi]); ekranın
     * o an kaç satır çizdiği bir yere yazıldığı için iki hızlı dokunuşta
     * aynı satır iki kez açılmaz. Sonuç her zaman [gununDersSayisi] ile
     * ekranda görülen sayının bir fazlasıdır.
     *
     * Satır boş (`ders = ""`) yazılır: tabloda olmak, o saatin **var olduğu**
     * anlamına gelir (bkz. [ProgramSatiri]). Dolu bir saat asla ezilmez,
     * çünkü sıra numarası zaten dolu saatlerin ilerisindedir. Gün 0'a
     * boşaltılmışsa ilk satır 1'e yazılır.
     */
    suspend fun saatAc(gun: Int) {
        gunuHazirla(gun)
        val sira = programDao.gununSonSirasi(gun) + 1
        programDao.kaydet(ProgramSatiri(gun = gun, sira = sira, ders = ""))
    }

    /**
     * Bir ders saatini — ve o saate atanmış dersi — programdan kaldırır.
     *
     * Satır yoksa sessizce biter. Sıra her zaman 1 ya da daha büyük olmalıdır:
     * 0 numaralı satır [ProgramSatiri.SIRA_ISARET]tir ve bir ders saati
     * değildir — koruması [com.odevtakip.app.ui.OdevViewModel.saatKaldir]
     * tarafındadır. Günün tamamı 0'a inince işaret satırı kalır, sayaç da
     * böylece "bu gün boş" ile "bu güne hiç dokunulmadı" ayrımını korur.
     */
    suspend fun saatKaldir(gun: Int, sira: Int) {
        gunuHazirla(gun)
        programDao.sil(gun, sira)
    }

    /**
     * Bir güne ilk kez yazmadan önce onu **kullanıma** alır.
     *
     * Sayaç satırlardan türüdüğü için satırların **1'den başması** gerekir.
     * Eski veride (Faz 14–15) yalnızca atanan saatler yazılıyordu: 3. saate
     * ders atanmış bir günde 1, 2, 4… satırları yoktu ve bu günler 8'den
     * aşağı düşerdi. Burada gün önce [GUNLUK_DERS_SAYISI]e tamamlanır —
     * [ProgramDao.ekle] ezmediği için var olan dersler olduğu gibi kalır —
     * sonra günün kullanımda olduğunu söyleyen [ProgramSatiri.SIRA_ISARET]
     * satırı yazılır.
     *
     * İşaret zaten varsa gün dönüştürülmüştür ve işlev hiçbir şey yapmaz;
     * 0'a boşaltılmış bir gün yeniden 8'e dolmaz. Aksi hâlde her yazma işlemi
     * boşaltılmış bir günü geri doldururdu.
     */
    private suspend fun gunuHazirla(gun: Int) {
        if (programDao.satirVarMi(gun, ProgramSatiri.SIRA_ISARET)) return

        for (sira in 1..GUNLUK_DERS_SAYISI) {
            programDao.ekle(ProgramSatiri(gun = gun, sira = sira, ders = ""))
        }
        programDao.kaydet(ProgramSatiri(gun = gun, sira = ProgramSatiri.SIRA_ISARET, ders = ""))
    }

    // ---- Dersler ----

    /** Dersleri ekleme sırasıyla yayınlar. */
    fun tumDersleri(): Flow<List<Ders>> = dersDao.tumDersleriIzle()

    /**
     * Yeni ders ekler.
     *
     * @return Ders eklendiyse `true`; ad boş ya da listede zaten varsa
     *   (büyük/küçük harf duyarsız) `false`.
     */
    suspend fun dersEkle(ad: String): Boolean {
        val mevcut = dersDao.tumDersleriAl()
        if (mevcut.dersAdiVarMi(ad)) return false
        dersDao.ekle(Ders(ad = ad.temizDersAdi()))
        return true
    }

    /**
     * Dersi listeden siler.
     *
     * Onu kullanan ödevlere dokunmaz — [Odev.ders] yalnızca addır.
     */
    suspend fun dersSil(id: Long) {
        dersDao.sil(id)
    }

    // ---- Okuma ----

    fun tumOdevleri(): Flow<List<Odev>> = dao.tumOdevleriIzle()

    fun durumdakiOdevleri(durum: Durum): Flow<List<Odev>> = dao.durumdakiOdevleriIzle(durum)

    fun odevi(id: Long): Flow<Odev?> = dao.odeviIzle(id)

    fun sayiyi(durum: Durum): Flow<Int> = dao.sayiyiIzle(durum)

    suspend fun odeviAl(id: Long): Odev? = dao.odeviAl(id)

    // ---- Yazma ----

    /**
     * Yeni ödev ekler veya var olanı günceller.
     *
     * Kaydedilen ödev, tarihi geçmişse [Durum.GECEKTI] olarak yazılır;
     * kullanıcı tarihi ileri alıp kaydettiyse yeniden [Durum.BEKLIYOR] olur.
     *
     * **Ek dosyası da burada sahiplenilir:** kayıt `ek` alanını değiştirdiyse
     * eskisi artık hiçbir ödevin referans vermediği bir dosyadır ve silinir.
     * Satır yazıldıktan **sonra** silinir; tersi sırayla, yazı işlemi
     * başarısız olursa kullanıcının eski eki hayattayken kaybolurdu.
     */
    suspend fun kaydet(odev: Odev): Long {
        val simdi = System.currentTimeMillis()
        val kayit = when {
            // Tamamlanmış ödev durumu korunur — tarih değişikliği onu geri açmaz.
            odev.durum == Durum.TAMAMLANDI -> odev
            odev.sonTarih <= simdi -> odev.copy(durum = Durum.GECEKTI)
            else -> odev.copy(durum = Durum.BEKLIYOR)
        }

        val eskiEk = if (odev.id > 0) dao.odeviAl(odev.id)?.ek else null
        val id = dao.kaydet(kayit)

        if (eskiEk != null && eskiEk != kayit.ek) ekSil(eskiEk)
        return id
    }

    /** Ödevi — ve varsa ek dosyasını — siler. */
    suspend fun sil(odev: Odev) {
        dao.sil(odev)
        odev.ek?.let { ekSil(it) }
    }

    /**
     * Ödevi kimliğiyle siler; ek dosyası da gider.
     *
     * @return Silinen satır sayısı (0 = kayıt yoktu).
     */
    suspend fun kimlikleSil(id: Long): Int {
        val odev = dao.odeviAl(id)
        val silinen = dao.kimlikleSil(id)
        if (silinen > 0) odev?.ek?.let { ekSil(it) }
        return silinen
    }

    /**
     * Ödevi tamamlandı olarak işaretler.
     *
     * Geçmişte kalmış bir ödevi de tamamlamak mümkündür; durum o zaman da
     * [Durum.TAMAMLANDI] olur, [Durum.GECEKTI]'ye geri dönmez.
     */
    suspend fun tamamla(id: Long): Boolean =
        dao.durumuGuncelle(
            id = id,
            durum = Durum.TAMAMLANDI,
            tamamlanmaZamani = System.currentTimeMillis()
        ) > 0

    /**
     * Tamamlandı işaretini geri alır; ödev tekrar bekleyen duruma döner ve
     * tarihi geçmişse bir sonraki senkronda "gecikti" olarak işaretlenir.
     */
    suspend fun tamamlandiginiGeriAl(id: Long): Boolean =
        dao.durumuGuncelle(id = id, durum = Durum.BEKLIYOR, tamamlanmaZamani = null) > 0

    // ---- Otomatik durum geçişleri ----

    /**
     * Son tarihi geçmiş ama tamamlanmamış ödevleri [Durum.GECEKTI] işaretler ve
     * **bu geçişte yeni olarak** geciken ödevleri döndürür.
     *
     * Uygulama açılışında, [com.odevtakip.app.work.DurumGuncelleWorker] içinde
     * ve WorkManager periyodik olarak çağırır. Tamamlanmış ödevler bu işlemden
     * etkilenmez (sorgu yalnızca [Durum.BEKLIYOR] satırlarına dokunur).
     *
     * Dönüş değeri bildirim içindir: yalnızca **yeni** gecikenler bildirilir.
     * Zaten gecikmiş olanlar döndürülmezdi; değilse her 12 saatte bir aynı
     * ödevler tekrar hatırlatılırdı.
     *
     * İki okuma arasında başka bir yazar devreye girerse küçük bir tutarsızlık
     * olabilir; yalnızca arka plan işi yazdığı için pratikte sorun çıkmaz ve
     * sonraki senkron düzeltir.
     *
     * @return Bu geçişte geciken ödevler. Hiçbiri yeni gecikmediyse boş liste.
     */
    suspend fun gecikmisleriIsaretle(): List<Odev> {
        val simdi = System.currentTimeMillis()

        val adayIdler = dao.gecikmisOdevIdleri(Durum.BEKLIYOR, simdi)
        val yeniler = if (adayIdler.isEmpty()) emptyList() else dao.idleriGetir(adayIdler)

        dao.gecikmisleriIsaretle(
            eski = Durum.BEKLIYOR,
            yeni = Durum.GECEKTI,
            simdi = simdi,
        )

        return yeniler
    }

    /**
     * Belirli bir ödevi, tarihi geçmediyse yeniden bekleyen duruma alır.
     * Kullanıcı süreyi uzattığında çağrılır.
     *
     * @return Durum gerçekten değiştiyse true.
     */
    suspend fun beklemeyeAl(id: Long): Boolean =
        dao.ertelendigindeBeklemeyeAl(
            id = id,
            eski = Durum.GECEKTI,
            yeni = Durum.BEKLIYOR,
            simdi = System.currentTimeMillis()
        ) > 0
}
