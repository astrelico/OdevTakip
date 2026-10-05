package com.odevtakip.app.data

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
 */
class OdevRepository(
    private val dao: OdevDao,
    private val dersDao: DersDao,
) {

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
     */
    suspend fun kaydet(odev: Odev): Long {
        val simdi = System.currentTimeMillis()
        val kayit = when {
            // Tamamlanmış ödev durumu korunur — tarih değişikliği onu geri açmaz.
            odev.durum == Durum.TAMAMLANDI -> odev
            odev.sonTarih <= simdi -> odev.copy(durum = Durum.GECEKTI)
            else -> odev.copy(durum = Durum.BEKLIYOR)
        }
        return dao.kaydet(kayit)
    }

    suspend fun sil(odev: Odev) = dao.sil(odev)

    suspend fun kimlikleSil(id: Long) = dao.kimlikleSil(id)

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
