package com.odevtakip.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * OdevRepository + Room DAO üzerine entegrasyon testleri.
 *
 * Bu testler uygulamanın özünü doğrular: tarih geçen ödev otomatik olarak
 * "gecikti"ye mi dönüyor, tamamlanan ödev geri açılıyor mu, kullanıcı süreyi
 * uzatınca bekleyene mi dönüyor.
 *
 * Bellek içi veritabanı kullanıldığı için cihazda kalıcı veri bırakmaz.
 */
@RunWith(AndroidJUnit4::class)
class OdevRepositoryTest {

    private lateinit var db: OdevDatabase
    private lateinit var repo: OdevRepository

    private val simdi = System.currentTimeMillis()
    private val birGun = 24 * 60 * 60 * 1000L

    @Before
    fun kur() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OdevDatabase::class.java
        ).build()
        repo = OdevRepository(db.odevDao())
    }

    @After
    fun yik() {
        db.close()
    }

    private fun yeniOdev(
        baslik: String = "Matematik 1. ünite",
        sonTarih: Long,
    ) = Odev(baslik = baslik, sonTarih = sonTarih)

    // ---- Kayıt ekleme ----

    @Test
    fun gelecektekiTeslimTarihiIleEklenenOdevBekleyenOlur() = runBlocking {
        val id = repo.kaydet(yeniOdev(sonTarih = simdi + birGun))
        assertEquals(Durum.BEKLIYOR, repo.odeviAl(id)?.durum)
    }

    @Test
    fun gecmisTeslimTarihiIleEklenenOdevAnindaGeciktiOlur() = runBlocking {
        val id = repo.kaydet(yeniOdev(sonTarih = simdi - birGun))
        assertEquals(Durum.GECEKTI, repo.odeviAl(id)?.durum)
    }

    @Test
    fun eklenenOdevListeyeYansir() = runBlocking {
        repo.kaydet(yeniOdev(baslik = "Türkçe kompozisyon", sonTarih = simdi + birGun))
        val liste = repo.tumOdevleri().first()
        assertEquals(1, liste.size)
        assertEquals("Türkçe kompozisyon", liste.first().baslik)
    }

    // ---- Otomatik gecikme ----

    @Test
    fun tarihiGecenBekleyenOdevIsaretlemeIleGeciktiyeDoner() = runBlocking {
        val id = repo.kaydet(yeniOdev(sonTarih = simdi + birGun))
        assertEquals(Durum.BEKLIYOR, repo.odeviAl(id)?.durum)

        // Cihaz kapalıyken tarih geçtiğini simüle et: kaydı doğrudan geçmişe çek.
        val gecmis = repo.odeviAl(id)!!.copy(sonTarih = simdi - 1)
        repo.kaydet(gecmis)
        assertEquals(Durum.GECEKTI, repo.odeviAl(id)?.durum)
    }

    @Test
    fun gecikmisleriIsaretleBeklemedekiGecmisOdevleriTopluGunceller() = runBlocking {
        // Manuel olarak "eski" durumda bırakılmış kayıtlar (cihaz kapalıyken
        // tarih geçti senaryosu) — DAO sorgusu bunları yakalamalı.
        val dao = db.odevDao()
        val id1 = repo.kaydet(yeniOdev(sonTarih = simdi - birGun))
        val id2 = repo.kaydet(yeniOdev(sonTarih = simdi - 2 * birGun))
        val id3 = repo.kaydet(yeniOdev(sonTarih = simdi + birGun)) // gelecek, dokunulmamalı

        // İki kaydı geriye "bekliyor" olarak zorla.
        dao.durumuGuncelle(id1, Durum.BEKLIYOR, null)
        dao.durumuGuncelle(id2, Durum.BEKLIYOR, null)

        val yeniGecikenler = repo.gecikmisleriIsaretle()

        assertEquals(2, yeniGecikenler.size)
        assertEquals(Durum.GECEKTI, repo.odeviAl(id1)?.durum)
        assertEquals(Durum.GECEKTI, repo.odeviAl(id2)?.durum)
        assertEquals(Durum.BEKLIYOR, repo.odeviAl(id3)?.durum)
    }

    @Test
    fun gecikmisleriIsaretleYeniGecikenleriBasliklariylaDondurur() = runBlocking {
        val dao = db.odevDao()
        // Geçmiş tarihli kayıtlar. Repository ilk kayıtda "gecikti" yazar;
        // biz durumu geri "bekliyor"ya alarak "cihaz kapalıyken tarih geçti"
        // senaryosunu taklit ediyoruz.
        val id1 = repo.kaydet(yeniOdev(baslik = "Kimya denemesi", sonTarih = simdi - birGun))
        val id2 = repo.kaydet(yeniOdev(baslik = "Biyoloji sunumu", sonTarih = simdi - 2 * birGun))
        dao.durumuGuncelle(id1, Durum.BEKLIYOR, null)
        dao.durumuGuncelle(id2, Durum.BEKLIYOR, null)

        val yeniGecikenler = repo.gecikmisleriIsaretle()

        assertEquals(
            setOf("Kimya denemesi", "Biyoloji sunumu"),
            yeniGecikenler.map { it.baslik }.toSet()
        )
    }

    @Test
    fun gecikmisleriIsaretleAyniOdevuIkinciKezBildirmez() = runBlocking {
        val dao = db.odevDao()
        val id = repo.kaydet(yeniOdev(sonTarih = simdi - birGun))
        dao.durumuGuncelle(id, Durum.BEKLIYOR, null) // tarih geçmiş, durum bekliyor

        assertEquals(1, repo.gecikmisleriIsaretle().size)

        // İkinci senkron (12 saatlik periyot) aynı ödevi yeniden "yeni" saymamalı;
        // yoksa kullanıcı aynı işi sürekli tekrar bildirim alırdı.
        assertTrue(
            "Aynı ödev ikinci senkronda tekrar dönmemeli",
            repo.gecikmisleriIsaretle().isEmpty()
        )
    }

    @Test
    fun gecikmisleriIsaretleZatenGecikmisOdevleriDondurmez() = runBlocking {
        // Daha önce "gecikti" olarak işaretlenmiş kayıt.
        db.odevDao().kaydet(
            Odev(baslik = "Eski gecikme", sonTarih = simdi - birGun, durum = Durum.GECEKTI)
        )

        assertTrue(repo.gecikmisleriIsaretle().isEmpty())
    }

    @Test
    fun gecikmisleriIsaretleTamamlanmisOdevlereDokunmaz() = runBlocking {
        val id = repo.kaydet(yeniOdev(sonTarih = simdi - birGun))
        repo.tamamla(id)
        assertEquals(Durum.TAMAMLANDI, repo.odeviAl(id)?.durum)

        repo.gecikmisleriIsaretle()

        assertEquals(Durum.TAMAMLANDI, repo.odeviAl(id)?.durum)
    }

    // ---- Tamamlama ----

    @Test
    fun gecmisOdevTamamlanincaTamamliOlurVeZamanDamgasiAlir() = runBlocking {
        val id = repo.kaydet(yeniOdev(sonTarih = simdi - birGun))
        assertEquals(Durum.GECEKTI, repo.odeviAl(id)?.durum)

        assertTrue(repo.tamamla(id))

        val odev = repo.odeviAl(id)!!
        assertEquals(Durum.TAMAMLANDI, odev.durum)
        assertTrue("tamamlanmaTarihi atanmalı", odev.tamamlanmaTarihi != null)
    }

    @Test
    fun gecmisOdevTamamlanincaTekrarGeciktiyeDusmez() = runBlocking {
        val id = repo.kaydet(yeniOdev(sonTarih = simdi - birGun))
        repo.tamamla(id)

        // Uygulama bir gün açık kalsa bile senkron çalıştırsa bile
        // tamamlanmış ödev geri "gecikti"ye dönmemeli.
        repo.gecikmisleriIsaretle()

        assertEquals(Durum.TAMAMLANDI, repo.odeviAl(id)?.durum)
    }

    @Test
    fun tamamlandiginiGeriAlOdeviBekleyeneCevirir() = runBlocking {
        val id = repo.kaydet(yeniOdev(sonTarih = simdi + birGun))
        repo.tamamla(id)
        assertEquals(Durum.TAMAMLANDI, repo.odeviAl(id)?.durum)

        assertTrue(repo.tamamlandiginiGeriAl(id))

        val odev = repo.odeviAl(id)!!
        assertEquals(Durum.BEKLIYOR, odev.durum)
        assertNull("tamamlanma zamanı silinmeli", odev.tamamlanmaTarihi)
    }

    // ---- Süre uzatma ----

    @Test
    fun sureUzatilincaGeciktiOdevYenidenBekleyeneDoner() = runBlocking {
        val id = repo.kaydet(yeniOdev(sonTarih = simdi - birGun))
        assertEquals(Durum.GECEKTI, repo.odeviAl(id)?.durum)

        // Kullanıcı teslim tarihini ileri alıp kaydediyor.
        val uzatilmis = repo.odeviAl(id)!!.copy(sonTarih = simdi + 3 * birGun)
        repo.kaydet(uzatilmis)

        assertEquals(Durum.BEKLIYOR, repo.odeviAl(id)?.durum)
    }

    @Test
    fun beklemeyeAlTarihiHalaGecmisOdeviDegistirmez() = runBlocking {
        val id = repo.kaydet(yeniOdev(sonTarih = simdi - birGun))
        assertEquals(Durum.GECEKTI, repo.odeviAl(id)?.durum)

        // Tarih hâlâ geçmişte → uygulama "bekliyor"ya almamalı.
        assertFalse(repo.beklemeyeAl(id))

        assertEquals(Durum.GECEKTI, repo.odeviAl(id)?.durum)
    }

    // ---- Silme ----

    @Test
    fun odevSilininceListedenKaybolur() = runBlocking {
        val id = repo.kaydet(yeniOdev(sonTarih = simdi + birGun))
        val odev = repo.odeviAl(id)!!

        repo.sil(odev)

        assertNull(repo.odeviAl(id))
        assertTrue(repo.tumOdevleri().first().isEmpty())
    }

    @Test
    fun kimlikleSilVarOlmayanKayittaSifirDondurur() = runBlocking {
        assertEquals(0, repo.kimlikleSil(999L))
    }

    // ---- Flow / reaktif okuma ----

    @Test
    fun yeniOdevEkleninceFlowYenidenEmitEder() = runBlocking {
        val flow = repo.tumOdevleri()
        assertEquals(0, flow.first().size)

        repo.kaydet(yeniOdev(sonTarih = simdi + birGun))

        assertEquals(1, flow.first().size)
    }

    @Test
    fun durumSayaciFlowIleGuncellenir() = runBlocking {
        repo.kaydet(yeniOdev(sonTarih = simdi - birGun)) // gecikti
        repo.kaydet(yeniOdev(sonTarih = simdi + birGun)) // bekleyen

        assertEquals(1, repo.sayiyi(Durum.GECEKTI).first())
        assertEquals(1, repo.sayiyi(Durum.BEKLIYOR).first())
        assertEquals(0, repo.sayiyi(Durum.TAMAMLANDI).first())
    }
}
