package com.odevtakip.app.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.odevtakip.app.OdevTakipApplication
import com.odevtakip.app.data.Ders
import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.HaftaGunu
import com.odevtakip.app.data.Odev
import com.odevtakip.app.data.OdevRepository
import com.odevtakip.app.data.ProgramSatiri
import com.odevtakip.app.data.Tercihler
import com.odevtakip.app.util.yerelTarih
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Ekranların durumunu ve kullanıcı eylemlerini yönetir.
 *
 * Veri her zaman [OdevRepository] üzerinden akar; ViewModel kopya tutmaz.
 * Bu sayede bir eylem (ekleme/silme/tamamlama) sonrası liste kendiliğinden
 * güncellenir — ekranı elle tazelemek gerekmez.
 *
 * [Tercihler] de aynı mantıkla akar: ayarlar ekranındaki tek bir anahtar
 * buradaki listeyi yeniden hesaplatır, ekranlar arası senkron gerekmez.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OdevViewModel(
    private val repository: OdevRepository,
    private val tercihler: Tercihler,
) : ViewModel() {

    private val _filtre = MutableStateFlow(OdevFiltresi.TUMU)
    val filtre: StateFlow<OdevFiltresi> = _filtre.asStateFlow()

    /**
     * Seçili ders filtresi; `null` = bütün dersler.
     *
     * Ödevler sekmesindeki "Filtreler" tuşu bu değeri kurar. Takvim ekranı
     * bundan **etkilenmez** — kullanıcı bir günün dersine göre değil,
     * gününe göre bakar.
     */
    private val _dersFiltresi = MutableStateFlow<String?>(null)
    val dersFiltresi: StateFlow<String?> = _dersFiltresi.asStateFlow()

    /**
     * Üst bardaki büyüteçte yazılan metin; boş string = arama yok.
     *
     * ViewModel'de tutulur: kullanıcı sekmeye gidip döndüğünde ne aradığını
     * unutmamalı — [seciliGun] ile aynı gerekçe.
     */
    private val _arama = MutableStateFlow("")
    val arama: StateFlow<String> = _arama.asStateFlow()

    /** Sıralama seçeneği; varsayılan [OdevSiralamasi.ONERILEN]. */
    private val _siralama = MutableStateFlow(OdevSiralamasi.ONERILEN)
    val siralama: StateFlow<OdevSiralamasi> = _siralama.asStateFlow()

    /** Ders listesi (formdaki seçici, filtre paneli ve ayarlar ekranı). */
    val dersler: StateFlow<List<Ders>> = repository.tumDersleri()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Seçili ders ayarlardan silinirse filtre sıfırlanır; aksi hâlde
        // kullanıcı ekranda hiçbir açıklama göremeden boş listeyle kalırdı.
        repository.tumDersleri()
            .onEach { liste ->
                val secili = _dersFiltresi.value
                if (secili != null && liste.none { it.ad == secili }) {
                    _dersFiltresi.value = null
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Seçili filtre, arama ve sıralamayla uyan ödevler, gösterim sırasıyla.
     * Ayrıntı için [suzulVeSirala] fonksiyonuna bak.
     */
    val odevler: StateFlow<List<Odev>> = combine(
        _filtre,
        tercihler.tamamlananlariGizle,
        _dersFiltresi,
        _arama,
        _siralama,
    ) { seciliFiltre, gizle, ders, arama, siralama ->
        ListeAyari(seciliFiltre, gizle, ders, arama, siralama)
    }
        .flatMapLatest { ayar ->
            repository.tumOdevleri().map {
                it.suzulVeSirala(
                    filtre = ayar.filtre,
                    tamamlananlariGizle = ayar.gizle,
                    dersFiltresi = ayar.ders,
                    arama = ayar.arama,
                    siralama = ayar.siralama,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Her filtre için ödev sayısı (çip rozetleri).
     *
     * Gizleme açıksa tamamlanmayanlar sayıma girmez; ders filtresi varsa yalnızca
     * o derse ait kayıtlar sayılır; arama açıkken yalnızca aramaya uyanlar.
     * Çipte yazan sayı, listede gerçekten görünen satır sayısıyla aynı olmalı —
     * "Tümü (5)" yazıp iki satır göstermek kullanıcıyı yanıltırdı.
     */
    val sayilar: StateFlow<Map<OdevFiltresi, Int>> = combine(
        repository.tumOdevleri(),
        tercihler.tamamlananlariGizle,
        _dersFiltresi,
        _arama,
    ) { liste, gizle, ders, arama ->
        liste
            .filter { !gizle || it.durum != Durum.TAMAMLANDI }
            .filter { ders == null || it.ders == ders }
            .filter { it.aramayaGore(arama) }
    }
        .map { liste ->
            val simdi = System.currentTimeMillis()
            OdevFiltresi.entries.associateWith { f -> liste.count { f.eslesir(it, simdi) } }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            OdevFiltresi.entries.associateWith { 0 }
        )

    /**
     * Filtre ve gizlemeden bağımsız toplam kayıt sayısı.
     *
     * [OdevListeEkrani]'ndeki boş durum metni bundan beslenir: gizleme
     * açıkken liste boşsa bile "henüz ödev yok" demek doğru değildir.
     */
    val toplamSayi: StateFlow<Int> = repository.tumOdevleri()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * Filtre, gizleme ve ders seçiminden **bağımsız** bütün ödevler.
     *
     * İstatistik ekranı buradan beslenir: kullanıcı listede "Geciken"
     * çipine basıp çıkmış olsa bile özeti tüm kayıtlar üzerinden görmelidir.
     * [odevler] akışını kullanmak, arayüzde 3 yazıp pasta diliminin 5
     * göstermesi gibi bir tutarsızlık doğururdu.
     */
    val tumOdevler: StateFlow<List<Odev>> = repository.tumOdevleri()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * "Tamamlananları gizle" anahtarının anlık değeri.
     *
     * Liste boşsa boş durum metnini seçmek için gereken tek bilgi budur:
     * gizleme açıkken "henüz ödev yok" demek yanlıştır, kayıt vardır ama
     * gizlenmektedir.
     */
    val tamamlananlariGizle: StateFlow<Boolean> = tercihler.tamamlananlariGizle

    // ---- Takvim ----

    /**
     * Takvim ekranının seçili günü.
     *
     * Tek örnek olarak tutulur; kullanıcı liste sekmesine geçip geri döndüğünde
     * seçtiği gün kaybolmaz.
     */
    private val _seciliGun = MutableStateFlow(LocalDate.now())
    val seciliGun: StateFlow<LocalDate> = _seciliGun.asStateFlow()

    /** Seçili günün ödevleri, teslim saatine göre artan (bitenler en sonda). */
    val gununOdevleri: StateFlow<List<Odev>> = _seciliGun
        .flatMapLatest { gun ->
            repository.tumOdevleri().map { it.guneGore(gun) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Güne göre ödev sayısı — tarih şeridindeki nokta göstergesi için.
     *
     * Tamamlanmış ödevler de sayılır; nokta "bu günde kayıt var" der,
     * durum bilgisini rozet zaten veriyor.
     */
    val gunSayilari: StateFlow<Map<LocalDate, Int>> = repository.tumOdevleri()
        .map { liste -> liste.groupingBy { it.sonTarih.yerelTarih() }.eachCount() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun gunAyarla(gun: LocalDate) {
        _seciliGun.value = gun
    }

    /** Detay ekranının izlediği ödev. Silinirse null olur. */
    private val _seciliOdevYuklendi = MutableStateFlow(false)

    /**
     * Detay ekranının ilk okuması tamamlandı mı?
     *
     * `seciliOdev == null` iki anlam taşıyor: "yükleniyor" ve "bulunamadı".
     * Bu bayrak ikisini birbirinden ayırır; ekran kısa süreli yükleme
     * göstergesinden sonra "bulunamadı" durumuna geçebilir.
     */
    val seciliOdevYuklendi: StateFlow<Boolean> = _seciliOdevYuklendi.asStateFlow()

    /** Detay ekranının izlediği ödev. Silinirse null olur. */
    private val _seciliOdevId = MutableStateFlow<Long?>(null)
    val seciliOdev: StateFlow<Odev?> = _seciliOdevId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.odevi(id)
        }
        .onEach { _seciliOdevYuklendi.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ---- Filtre ----

    fun filtreAyarla(yeni: OdevFiltresi) {
        _filtre.value = yeni
    }

    /**
     * Ders filtresini kurar. `null` bütün dersler demektir.
     *
     * Girdi doğrulanmaz: değer zaten ekrandaki listeden gelir. Seçili ders
     * sonradan silinirse filtre [init] içinde kendiliğinden sıfırlanır.
     */
    fun dersFiltresiAyarla(ad: String?) {
        _dersFiltresi.value = ad
    }

    /**
     * Büyüteçte yazılan metni günceller; boş metin aramayı kapatır.
     *
     * Girdi doğrulanmaz — arama yalnızca süzer, yazım hatası "sonuç yok"
     * demekten öteye geçmez.
     */
    fun aramaAyarla(metin: String) {
        _arama.value = metin
    }

    /** Sıralama seçeneğini değiştirir; sekme değiştirilince de korunur. */
    fun siralamaAyarla(siralama: OdevSiralamasi) {
        _siralama.value = siralama
    }

    // ---- Dersler ----

    /**
     * Yeni ders ekler.
     *
     * @param onBasarili `true` eklendiğini, `false` adın boş ya da zaten
     *   listede olduğunu bildirir (ekran hata metnini ona göre gösterir).
     */
    fun dersEkle(ad: String, onBasarili: (Boolean) -> Unit) {
        viewModelScope.launch {
            onBasarili(repository.dersEkle(ad))
        }
    }

    /** Dersi listeden siler; onu kullanan ödevlere dokunmaz. */
    fun dersSil(id: Long) {
        viewModelScope.launch { repository.dersSil(id) }
    }

    // ---- Ders programı ----

    /**
     * Haftalık ders programı, gün ve sıra sırasıyla.
     *
     * Tabloda yalnızca **doldurulmuş** saatler ile "Ders ekle" ile açılmış
     * satırlar durur; program ekranı
     * [com.odevtakip.app.data.gununDersSayisi] sonucu kadar satır çizer ve
     * kayıt bulamadığı saati "boş" sayar. Böylece ekrandaki satır sayısı
     * veritabanıyla aynı kaynaktan gelir, iki yerde ayrı ayrı tutulmaz.
     */
    val program: StateFlow<List<ProgramSatiri>> = repository.programi()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Program ekranının seçili hafta günü (0 = pazartesi).
     *
     * Seçim ViewModel'de tutulur: kullanıcı ödev listesine gidip geri
     * döndüğünde baktığı gün kaybolmaz — [seciliGun] ile aynı gerekçe.
     */
    private val _seciliHaftaGunu = MutableStateFlow(HaftaGunu.bugun().sira)
    val seciliHaftaGunu: StateFlow<Int> = _seciliHaftaGunu.asStateFlow()

    fun haftaGunuAyarla(sira: Int) {
        _seciliHaftaGunu.value = HaftaGunu.den(sira).sira
    }

    /** Bir ders saatine ders atar; `ders` boşsa o saati boşa alır. */
    fun programaYaz(gun: Int, sira: Int, ders: String) {
        viewModelScope.launch { repository.programaYaz(gun, sira, ders) }
    }

    /**
     * Seçili güne bir ders saati daha açar (9., 10., …).
     *
     * Yalnızca **seçili** güne yazılır: okul günlerinin uzunluğu günden güne
     * değişebildiği için satır sayısını hafta geneline yaymak yerine kullanıcı
     * o gün kadarını açar. Sınır yoktur — ders sayısı 0'a da inebilir 20'ye de
     * çıkabilir, karar tamamen kullanıcının okul gününe bağlıdır.
     */
    fun saatAc(gun: Int) {
        viewModelScope.launch { repository.saatAc(gun) }
    }

    /**
     * Bir ders saatini — ve o saate atanmış dersi — programdan kaldırır.
     *
     * Arayüz en az bir satır kala kadar bu düğmeyi çizer; 0'a inmek de
     * mümkündür, dolayısıyla hafta sonu gibi dersi olmayan günler
     * tamamen boşaltılabilir. [com.odevtakip.app.data.ProgramSatiri.SIRA_ISARET]
     * satırı ise kaldırılamaz: sayaç ancak orada "bu gün boş" ile "bu güne hiç
     * dokunulmadı" ayrımını yapabilir.
     */
    fun saatKaldir(gun: Int, sira: Int) {
        viewModelScope.launch {
            if (sira > ProgramSatiri.SIRA_ISARET) {
                repository.saatKaldir(gun, sira)
            }
        }
    }

    // ---- Seçili ödev ----

    fun seciliOdeviAyarla(id: Long?) {
        _seciliOdevYuklendi.value = false
        _seciliOdevId.value = id
    }

    /** Düzenleme formu için ödevi bir kez okur. */
    suspend fun odeviGet(id: Long): Odev? = repository.odeviAl(id)

    // ---- Dosya / fotoğraf eki ----

    /**
     * Seçicinin verdiği Uri'nin kullanıcının gördüğü dosya adını okur.
     *
     * Dosya henüz diske yazılmadı — yalnızca formdaki önizleme için ad.
     * Kaydetme sırasında dosyanın kendisi [ekKopyala] ile kopyalanır.
     */
    suspend fun ekAdi(uri: Uri): String? = repository.ekAdi(uri)

    /** Dosyayı diske kopyalar; başarısızsa `null`. */
    suspend fun ekKopyala(uri: Uri): String? = repository.ekKopyala(uri)

    /** Ek dosyasının tam yolu — detay ekranındaki önizleme için. */
    fun ekDosyasi(ad: String): File = repository.ekDosyasi(ad)

    // ---- Eylemler ----

    /**
     * Ödevi ekler veya günceller.
     * @param onBasarili Kayıt başarılıysa çağrılır (ekranı kapatmak için).
     */
    fun kaydet(odev: Odev, onBasarili: () -> Unit) {
        viewModelScope.launch {
            repository.kaydet(odev)
            onBasarili()
        }
    }

    /**
     * Form ekranındaki verilerle ödevi ekler veya günceller.
     *
     * Düzenlemede mevcut kaydı okur; böylece `olusturmaTarihi` ve
     * `tamamlanmaTarihi` korunur. Durumu [OdevRepository.kaydet] hesaplar —
     * tarih ileri alındıysa "gecikti" geri "bekliyor"ya döner.
     *
     * @param odevId Yeni ekleme için `0` ya da negatif.
     * @param ders Seçilen dersin adı; zorunlu alan olduğu için arayüz bunu
     *   boş göndermemeli.
     * @param ekUri Bu oturumda seçilen yeni dosya. Verilirse içeriği burada
     *   diske kopyalanır ve `ek` yerine geçer. **Kopyalama yalnızca bu
     *   noktada yapılır** — formdan vazgeçildiğinde artık dosya kalmaz.
     * @param ek Yeni seçim yoksa kaydedilecek mevcut ek adı; kullanıcı ek
     *   sildiyse `null`. (`ekUri` verildiğinde yok sayılır.)
     * @param ekKopyaKaynak Ödev kopyalama modunda kaydın **kendi** ek adı.
     *   Verilen dosya diske **yeniden** kopyalanır: iki kayıt da aynı adı
     *   taşısaydı birini silmek ötekinin ekini de silerdi. (`ekUri` ve `ek`
     *   verilmişse onlar kazanır — kullanıcı formda ekini zaten değiştirmiş
     *   ya da kaldırmıştır.)
     * @param onEkHatasi Dosya kopyalanamadığında çağrılır: ödev kaydedilmez,
     *   form açık kalır ve kullanıcı dosyayı yeniden seçebilir. Eksik kayıt,
     *   hatadan kötüdür.
     * @param onBasarili Kayıt başarılıysa çağrılır.
     */
    fun formuKaydet(
        odevId: Long,
        baslik: String,
        aciklama: String,
        ders: String,
        sonTarih: Long,
        ekUri: Uri? = null,
        ek: String? = null,
        ekKopyaKaynak: String? = null,
        onEkHatasi: () -> Unit = {},
        onBasarili: () -> Unit,
    ) {
        viewModelScope.launch {
            val mevcut = if (odevId > 0) repository.odeviAl(odevId) else null

            val yeniEk = when {
                ekUri != null -> repository.ekKopyala(ekUri) ?: run {
                    onEkHatasi()
                    return@launch
                }

                // Kaynak bu arada silinmişse kopyalanacak bir şey yoktur;
                // bu durum hata değil, eksize bir kopyadır.
                ekKopyaKaynak != null &&
                    repository.ekDosyasi(ekKopyaKaynak).exists() ->
                    repository.ekKopyalaMevcut(ekKopyaKaynak) ?: run {
                        onEkHatasi()
                        return@launch
                    }

                else -> ek
            }

            repository.kaydet(
                Odev(
                    id = mevcut?.id ?: 0,
                    baslik = baslik.trim(),
                    aciklama = aciklama.trim(),
                    ders = ders.trim(),
                    sonTarih = sonTarih,
                    durum = mevcut?.durum ?: Durum.BEKLIYOR,
                    olusturmaTarihi = mevcut?.olusturmaTarihi ?: System.currentTimeMillis(),
                    tamamlanmaTarihi = mevcut?.tamamlanmaTarihi,
                    ek = yeniEk,
                )
            )
            onBasarili()
        }
    }

    fun tamamla(id: Long) {
        viewModelScope.launch { repository.tamamla(id) }
    }

    fun tamamlamayiGeriAl(id: Long) {
        viewModelScope.launch { repository.tamamlandiginiGeriAl(id) }
    }

    fun sil(id: Long, onBasarili: () -> Unit) {
        viewModelScope.launch {
            repository.kimlikleSil(id)
            onBasarili()
        }
    }

    companion object {
        /**
         * ViewModel'i [OdevTakipApplication.odevRepository] üzerinden kurar.
         * Ayrı bir DI kütüphanesi gerekmez.
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val uygulama = this[APPLICATION_KEY] as OdevTakipApplication
                OdevViewModel(
                    repository = uygulama.odevRepository,
                    tercihler = uygulama.tercihler,
                )
            }
        }
    }
}

/**
 * Süzme ve sıralama.
 *
 * Dört süzgeç ardışık uygulanır:
 *  1. `tamamlananlariGizle` — gizleme açıksa bitenler daha ilk adımda elenir.
 *  2. `dersFiltresi` — yalnızca o derse ait kayıtlar kalır.
 *  3. `arama` — başlık, açıklama ve ders adında, Türkçe'ye duyarlı arama
 *     ([Odev.aramayaGore]).
 *  4. `filtre` — çip koşulu (geciken / bugün / yaklaşan).
 *
 * Ardından sıralama uygulanır. Varsayılan [OdevSiralamasi.ONERILEN]:
 *
 *  - tamamlanmayanlar önce, teslim tarihine göre artan (en yakın teslim en üstte),
 *  - tamamlananlar sonra, teslim tarihine göre azalan.
 *
 * Tamamlanan ödevler en sonda kaldığı için liste iş bitince kendiliğinden
 * "temizlenir"; kullanıcı aynı anda hem bekleyen hem bitenleri görür.
 *
 * Diğer sıralamalar bu **taban dizilim**nin üzerine kararlı bir sıralama
 * uygular. Kotlin'in `sortedBy`'si kararlıdır: eşit değerdeki kayıtlar
 * taban sıraya geri döner. Yani "Ders adına göre" seçildiğinde aynı dersin
 * **içinde** teslim tarihi düzeni bozulmaz.
 *
 * @param dersFiltresi Süzülecek dersin adı; `null` bütün dersler.
 * @param arama Aranacak metin; boş ya da yalnızca boşluk aramayı kapatır.
 * @param siralama Gösterim sırası; varsayılan [OdevSiralamasi.ONERILEN].
 */
internal fun List<Odev>.suzulVeSirala(
    filtre: OdevFiltresi,
    tamamlananlariGizle: Boolean,
    dersFiltresi: String? = null,
    arama: String = "",
    siralama: OdevSiralamasi = OdevSiralamasi.ONERILEN,
): List<Odev> {
    val simdi = System.currentTimeMillis()
    var havuz: List<Odev> = if (tamamlananlariGizle) {
        filter { it.durum != Durum.TAMAMLANDI }
    } else {
        this
    }
    if (dersFiltresi != null) {
        havuz = havuz.filter { it.ders == dersFiltresi }
    }
    val aranmis = havuz.filter { it.aramayaGore(arama) }
    val uygun = aranmis.filter { filtre.eslesir(it, simdi) }

    val taban = uygun
        .filter { it.durum != Durum.TAMAMLANDI }
        .sortedBy { it.sonTarih } +
        uygun
            .filter { it.durum == Durum.TAMAMLANDI }
            .sortedByDescending { it.sonTarih }

    return when (siralama) {
        OdevSiralamasi.ONERILEN -> taban
        OdevSiralamasi.DERS_ADI -> taban.sortedBy { it.ders.anahtar() }
        OdevSiralamasi.ADA_GORE -> taban.sortedBy { it.baslik.anahtar() }
        OdevSiralamasi.YENI_EKLENEN -> taban.sortedByDescending { it.olusturmaTarihi }
    }
}

/**
 * Arama ve sıralama için kullanılan eşleştirme anahtarı.
 *
 * İki ayrı Türkçe sorununu da kapatır:
 *
 *  - `lowercase()` kök yerelde çalıştığı için `I` → `i` olur; kullanıcı
 *    İngilizce klavyede "FIZIK" yazsa bile "Fizik" bulunur.
 *  - `ı` ayrıca `i`'ye çekilir: "SINIF" araması "Sınıf"ı, "IRMAK" araması
 *    "Irmak"ı bulur. (Tersi de doğrudur.)
 *  - `İ` küçük harfe inerken **birleşen nokta** (U+0307) bırakabilir; o
 *    nokta silinmezse "ing" araması "İngilizce"yi geçer — karakter
 *    klavyede yazılamadığı için kullanıcı asla göremez.
 *
 * @receiver Hem aranacak metin hem aranan ödevin alanı — aynı işlemden
 *   geçmeleri şart, aksi hâlde iki taraf farklı alfabelerle konuşur.
 */
private fun String.anahtar(): String = lowercase()
    .replace('ı', 'i')
    .replace("\u0307", "")

/**
 * [arama] metni bu ödevle eşleşiyor mu?
 *
 * Başlık, açıklama **ve** ders adı üzerinden aranır: kullanıcının bir ders
 * adını yazdığında o dersteki bütün ödevlerin çıkması beklenendir.
 *
 * Boş arama her zaman eşleşir — süzgeç kapalı demektir.
 */
internal fun Odev.aramayaGore(arama: String): Boolean {
    val anahtar = arama.trim().anahtar()
    if (anahtar.isEmpty()) return true
    return baslik.anahtar().contains(anahtar) ||
        aciklama.anahtar().contains(anahtar) ||
        ders.anahtar().contains(anahtar)
}

/**
 * Bir güne düşen ödevler, takvim listesinin sırası.
 *
 * Sıralama kuralı listeyle aynı: bekleyenler önce (teslim saatine göre artan),
 * tamamlananlar sonra. Gün içinde hangi ödevin önce ele alınacağı böylece
 * hem listede hem takvimde aynı olur.
 */
private fun List<Odev>.guneGore(gun: LocalDate): List<Odev> {
    val uygun = filter { it.sonTarih.yerelTarih() == gun }
    return uygun.sortedWith(
        compareBy({ it.durum == Durum.TAMAMLANDI }, { it.sonTarih })
    )
}

/**
 * [OdevViewModel.odevler] akışının beş girdisi, tek pakette.
 *
 * `combine` beş akışı yan yana dizince lambda imzası okunmaz hâle gelir;
 * isimlendirilmiş bir sınıf hem onu düzeltir hem de `flatMapLatest`
 * içindeki çağrıda parametre sırasının karışmasını engeller.
 */
private data class ListeAyari(
    val filtre: OdevFiltresi,
    val gizle: Boolean,
    val ders: String?,
    val arama: String,
    val siralama: OdevSiralamasi,
)
