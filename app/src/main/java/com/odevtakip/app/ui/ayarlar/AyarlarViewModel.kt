package com.odevtakip.app.ui.ayarlar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.odevtakip.app.OdevTakipApplication
import com.odevtakip.app.data.TemaSecenegi
import com.odevtakip.app.data.Tercihler
import kotlinx.coroutines.flow.StateFlow

/**
 * Ayarlar ekranının arkasındaki küçük VM.
 *
 * Asıl işi [Tercihler] akışlarını olduğu gibi dışarı vermek: yazma anında
 * akış zaten güncellendiği için ekranın ayrıca bir "kaydet" adımı yoktur.
 * Tema seçimi anında uygulanır, liste gizleme anında listeyi yeniden hesaplar.
 */
class AyarlarViewModel(private val tercihler: Tercihler) : ViewModel() {

    val tema: StateFlow<TemaSecenegi> = tercihler.tema

    val tamamlananlariGizle: StateFlow<Boolean> = tercihler.tamamlananlariGizle

    fun temaAyarla(secenek: TemaSecenegi) = tercihler.temaAyarla(secenek)

    fun tamamlananlariGizleAyarla(deger: Boolean) =
        tercihler.tamamlananlariGizleAyarla(deger)

    companion object {
        /** Ana liste VM'siyle aynı kurulum yolu: DI kütüphanesi gerekmez. */
        val Factory = viewModelFactory {
            initializer {
                val uygulama = this[APPLICATION_KEY] as OdevTakipApplication
                AyarlarViewModel(uygulama.tercihler)
            }
        }
    }
}
