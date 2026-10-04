package com.odevtakip.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.odevtakip.app.OdevTakipApplication
import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.Odev
import com.odevtakip.app.data.OdevRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OdevViewModel(private val repository: OdevRepository) : ViewModel() {

    private val _filtre = MutableStateFlow(OdevFiltresi.TUMU)
    val filtre: StateFlow<OdevFiltresi> = _filtre.asStateFlow()

    /**
     * Seçili filtreye uyan ödevler, gösterim sırasıyla.
     * Ayrıntı için [suzulVeSirala] fonksiyonuna bak.
     */
    val odevler: StateFlow<List<Odev>> = _filtre
        .flatMapLatest { seciliFiltre ->
            repository.tumOdevleri().map { it.suzulVeSirala(seciliFiltre) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Her filtre için ödev sayısı (çip rozetleri). */
    val sayilar: StateFlow<Map<OdevFiltresi, Int>> = repository.tumOdevleri()
        .map { liste ->
            val simdi = System.currentTimeMillis()
            OdevFiltresi.entries.associateWith { f -> liste.count { f.eslesir(it, simdi) } }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            OdevFiltresi.entries.associateWith { 0 }
        )

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

    // ---- Seçili ödev ----

    fun seciliOdeviAyarla(id: Long?) {
        _seciliOdevYuklendi.value = false
        _seciliOdevId.value = id
    }

    /** Düzenleme formu için ödevi bir kez okur. */
    suspend fun odeviGet(id: Long): Odev? = repository.odeviAl(id)

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
     * @param onBasarili Kayıt başarılıysa çağrılır.
     */
    fun formuKaydet(
        odevId: Long,
        baslik: String,
        aciklama: String,
        sonTarih: Long,
        onBasarili: () -> Unit,
    ) {
        viewModelScope.launch {
            val mevcut = if (odevId > 0) repository.odeviAl(odevId) else null
            repository.kaydet(
                Odev(
                    id = mevcut?.id ?: 0,
                    baslik = baslik.trim(),
                    aciklama = aciklama.trim(),
                    sonTarih = sonTarih,
                    durum = mevcut?.durum ?: Durum.BEKLIYOR,
                    olusturmaTarihi = mevcut?.olusturmaTarihi ?: System.currentTimeMillis(),
                    tamamlanmaTarihi = mevcut?.tamamlanmaTarihi,
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
                OdevViewModel(uygulama.odevRepository)
            }
        }
    }
}

/**
 * Filtreler ve sıralama.
 *
 * Kural:
 *  1. Filtreye uymayanlar atılır.
 *  2. Tamamlanmayanlar önce, teslim tarihine göre artan sırada
 *     (en yakın teslim en üstte).
 *  3. Tamamlanmışlar sonra, teslim tarihine göre azalan sırada.
 *
 * Tamamlanan ödevler en sonda kaldığı için liste iş bitince kendiliğinden
 * "temizlenir"; kullanıcı aynı anda hem bekleyen hem bitenleri görür.
 */
private fun List<Odev>.suzulVeSirala(filtre: OdevFiltresi): List<Odev> {
    val simdi = System.currentTimeMillis()
    val uygun = filter { filtre.eslesir(it, simdi) }

    val bekleyenler = uygun
        .filter { it.durum != Durum.TAMAMLANDI }
        .sortedBy { it.sonTarih }

    val tamamlananlar = uygun
        .filter { it.durum == Durum.TAMAMLANDI }
        .sortedByDescending { it.sonTarih }

    return bekleyenler + tamamlananlar
}
