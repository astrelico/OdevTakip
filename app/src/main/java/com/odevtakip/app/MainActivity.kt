package com.odevtakip.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.odevtakip.app.data.TemaSecenegi
import com.odevtakip.app.ui.OdevViewModel
import com.odevtakip.app.ui.ayarlar.AyarlarEkrani
import com.odevtakip.app.ui.ayarlar.AyarlarViewModel
import com.odevtakip.app.ui.dersler.DersEkrani
import com.odevtakip.app.ui.detay.OdevDetayEkrani
import com.odevtakip.app.ui.form.OdevFormEkrani
import com.odevtakip.app.ui.istatistik.IstatistikEkrani
import com.odevtakip.app.ui.liste.OdevListeEkrani
import com.odevtakip.app.ui.program.ProgramEkrani
import com.odevtakip.app.ui.takvim.TakvimEkrani
import com.odevtakip.app.ui.theme.OdevTakipTheme
import com.odevtakip.app.util.turkceyeSabitle

/** Uygulamanın gezinme rotaları. */
private object Rotalar {
    const val LISTE = "liste"
    const val TAKVIM = "takvim"
    const val FORM = "form/{odevId}"
    const val DETAY = "detay/{odevId}"
    const val AYARLAR = "ayarlar"
    const val DERSLER = "dersler"
    const val PROGRAM = "program"
    const val ISTATISTIK = "istatistik"

    /** Yeni ödev için `odevId = -1`. */
    fun form(odevId: Long = -1L): String = "form/$odevId"
    fun detay(odevId: Long): String = "detay/$odevId"
}

class MainActivity : ComponentActivity() {

    /**
     * Tüm kaynakların Türkçe çözülmesini sağlar.
     *
     * Kütüphane kaynaklı metinler (Material3 seçici başlığı, ay/gün adları)
     * sistem dilini izler; bu override olmasaydı uygulama kendi metinleriyle
     * Türkçe, seçicilerle İngilizce görünürdü. Ayrıntı: [turkceyeSabitle].
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.turkceyeSabitle())
    }

    /**
     * Bildirimden gelinen ödev kimliği.
     *
     * İki yoldan yazılır: [onCreate] (uygulama kapalıyken dokunulmuşsa) ve
     * [onNewIntent] (arkadayken dokunulmuşsa). Değer bir kez tüketilince
     * `null` yapılır; aksi hâlde geri tuşuyla listeye dönüldüğünde aynı
     * detay yeniden açılırdı.
     *
     * Extra ayrıca **okunur okunmaz silinir**. Silinmezse aktivite
     * öldürülüp yeniden kurulduğunda (kayıt durumu geri yüklenirken) eski
     * niyet tekrar okunur ve kullanıcı çıktığı yerden değil bildirimden
     * geldiği yerden başlardı.
     */
    private val hedefOdevId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Kayıt durumu geri yükleniyorsa gezinme zaten korunmuştur; niyeti
        // bir daha işlemek çift gezinme yapardı.
        if (savedInstanceState == null) {
            hedefOdevId.value = intent.odevKimligi()
        }

        setContent {
            // Ayarlar ekranındaki tema seçimi bu akıştan okunur; akış anında
            // güncellendiği için seçim uygulamayı yeniden başlatmadan uygulanır.
            val tercihler = (application as OdevTakipApplication).tercihler
            val tema by tercihler.tema.collectAsStateWithLifecycle()

            OdevTakipTheme(
                darkTheme = tema.koyuTemayaDonusur(isSystemInDarkTheme()),
            ) {
                OdevUygulamasi(
                    hedefOdevId = hedefOdevId.value,
                    onHedefTukendi = { hedefOdevId.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val kimlik = intent.odevKimligi()
        setIntent(intent)
        hedefOdevId.value = kimlik
    }

    /**
     * Niyetten ödev kimliğini okur ve extra'yı siler.
     *
     * @return Hedef ödev varsa kimliği, yoksa `null`.
     */
    private fun Intent.odevKimligi(): Long? {
        val kimlik = getLongExtra(EXTRA_ODEV_ID, -1L)
        removeExtra(EXTRA_ODEV_ID)
        return kimlik.takeIf { it > 0 }
    }

    companion object {
        /**
         * Yaklaşan teslim bildiriminin açtığı ödev.
         *
         * [com.odevtakip.app.bildirim.BildirimYonetici] bu anahtarı yazar;
         * bu yüzden özel olamaz.
         */
        const val EXTRA_ODEV_ID = "odevId"
    }
}

/**
 * Uygulama iskeleti: alt menü + ortak "yeni ödev" düğmesi + gezinme grafiği.
 *
 * Çizim sırası şöyle kurulur:
 *
 *  - **Liste**, **Takvim**, **Ders Programı** ve **İstatistik** ana
 *    ekranlardır; alt menü yalnızca onlarda görünür.
 *  - **"Yeni ödev" düğmesi** Scaffold'un kendi FAB yuvasında, sağ alt
 *    köşede ve alt menünün üstünde durur; yalnızca Liste ile Takvim'de
 *    çizilir. Ayrıntı: [YeniOdevDugmesi].
 *  - **Form**, **detay** ve **ayarlar** üst üste itilen tam ekran rotalardır;
 *    menü gizlenir böylece klavye/açılır listeyle boğuşmazlar.
 *  - Her ekran kendi üst barını diker. Bu yüzden dış `Scaffold`'un
 *    `contentWindowInsets`'i sıfırlanır; sistem çubuğu ücreti iki kez
 *    ödenirse başlık durum çubuğunun altına kayar.
 *
 * [OdevViewModel] aktivite kapsamında tek örnek olarak tutulur; böylece
 * filtre, seçili gün ve liste durumu ekranlar arasında kaybolmaz.
 *
 * @param hedefOdevId Bildirimden gelinen ödev; `null` ise normal açılış.
 * @param onHedefTukendi Hedef bir kez işlendikten sonra çağrılır — bkz.
 *   [MainActivity] içindeki `hedefOdevId`.
 */
@Composable
private fun OdevUygulamasi(
    hedefOdevId: Long?,
    onHedefTukendi: () -> Unit,
    viewModel: OdevViewModel = viewModel(factory = OdevViewModel.Factory),
) {
    val navController = rememberNavController()
    val geriAlabilir by navController.currentBackStackEntryAsState()
    val mevcutRota = geriAlabilir?.destination?.route
    val anaEkran = mevcutRota == Rotalar.LISTE ||
        mevcutRota == Rotalar.TAKVIM ||
        mevcutRota == Rotalar.PROGRAM ||
        mevcutRota == Rotalar.ISTATISTIK

    fun git(rota: String) {
        // Yerel fonksiyon bileşimler arasında yeniden kurulmadığı için
        // `mevcutRota`'yı yakaladığımızda eski bir değerde kalabiliyoruz.
        // Bu yüzden rota her dokunuşta doğrudan navController'dan okunur.
        val simdi = navController.currentBackStackEntry?.destination?.route
        if (simdi == rota) return
        navController.navigate(rota) {
            popUpTo(Rotalar.LISTE) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Bildirime dokunulduğunda hedef ödevin detayına in.
    //
    // `LaunchedEffect`, bütün kompozisyon uygulandıktan sonra çalışır — yani
    // NavHost aşağıda çizilip grafını kurduktan sonra. Hedef tükendiğinde
    // parametre `null` olur ve etki yeniden başlayıp hiçbir şey yapmaz.
    LaunchedEffect(hedefOdevId) {
        val id = hedefOdevId ?: return@LaunchedEffect
        navController.navigate(Rotalar.detay(id)) { launchSingleTop = true }
        onHedefTukendi()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        bottomBar = {
            if (anaEkran) {
                AltMenu(
                    seciliRota = mevcutRota,
                    onSecim = ::git,
                )
            }
        },
        // "Yeni ödev" düğmesi alt menünün **üzerinde**, sağ uçta durur;
        // `FabPosition.End` bunu kurar, `EndOverlay` barın üstüne bindirirdi.
        floatingActionButton = {
            if (mevcutRota == Rotalar.LISTE || mevcutRota == Rotalar.TAKVIM) {
                YeniOdevDugmesi(
                    onClick = {
                        navController.navigate(Rotalar.form()) { launchSingleTop = true }
                    },
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Rotalar.LISTE,
            modifier = Modifier
                .padding(innerPadding)
                // Alt menü zeden ödedi; iç ekranların kendi Scaffold'ları
                // aynı ücreti ikinci kez almasın.
                .consumeWindowInsets(innerPadding),
        ) {
            composable(Rotalar.LISTE) {
                OdevListeEkrani(
                    viewModel = viewModel,
                    onOdevSec = { navController.navigate(Rotalar.detay(it)) },
                    onAyarlar = { navController.navigate(Rotalar.AYARLAR) },
                )
            }

            composable(Rotalar.TAKVIM) {
                TakvimEkrani(
                    viewModel = viewModel,
                    onOdevSec = { navController.navigate(Rotalar.detay(it)) },
                    onAyarlar = { navController.navigate(Rotalar.AYARLAR) },
                )
            }

            // Ders programı üçüncü ana sekmedir; alt menüde görünür.
            composable(Rotalar.PROGRAM) {
                ProgramEkrani(
                    viewModel = viewModel,
                    onAyarlar = { navController.navigate(Rotalar.AYARLAR) },
                )
            }

            // İstatistik dördüncü ana sekmedir; salt okunur, yazma eylemi yok.
            composable(Rotalar.ISTATISTIK) {
                IstatistikEkrani(
                    viewModel = viewModel,
                    onAyarlar = { navController.navigate(Rotalar.AYARLAR) },
                )
            }

            composable(
                route = Rotalar.FORM,
                arguments = listOf(
                    navArgument("odevId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                ),
            ) { entry ->
                OdevFormEkrani(
                    odevId = entry.arguments?.getLong("odevId") ?: -1L,
                    viewModel = viewModel,
                    onGeri = { navController.popBackStack() },
                )
            }

            composable(
                route = Rotalar.DETAY,
                arguments = listOf(navArgument("odevId") { type = NavType.LongType }),
            ) { entry ->
                OdevDetayEkrani(
                    odevId = entry.arguments?.getLong("odevId") ?: -1L,
                    viewModel = viewModel,
                    onGeri = { navController.popBackStack() },
                    onDuzenle = { navController.navigate(Rotalar.form(it)) },
                )
            }

            // Ayarlar tam ekran bir rota: alt menü gizlenir, geri düğmesi döner.
            composable(Rotalar.AYARLAR) {
                AyarlarEkrani(
                    viewModel = viewModel(factory = AyarlarViewModel.Factory),
                    onGeri = { navController.popBackStack() },
                    onDersler = {
                        navController.navigate(Rotalar.DERSLER) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            // Ders listesi de ayarların altındaki tam ekran bir rotadır.
            composable(Rotalar.DERSLER) {
                DersEkrani(
                    viewModel = viewModel,
                    onGeri = { navController.popBackStack() },
                )
            }
        }
    }
}

/**
 * Alt menü: dört sekme.
 *
 * Barın içinde yalnızca gezinme var; "yeni ödev" eylemi artık
 * [YeniOdevDugmesi] olarak **Scaffold'un kendi FAB yuvasında**, sağ alt
 * köşede durur. İkisinin ayrı yerlerde olması, ortak `Box` içinde birbirine
 * binmelerini gerektiren eski düzeni gereksiz kıldı: menü bir `Box`'a bile
 * ihtiyaç duymuyor.
 *
 * @param seciliRota Hangi sekmenin seçili olduğunu belirler.
 */
@Composable
private fun AltMenu(
    seciliRota: String?,
    onSecim: (String) -> Unit,
) {
    val renkler = MaterialTheme.colorScheme

    NavigationBar(
        containerColor = renkler.surface,
        contentColor = renkler.onSurface,
    ) {
        NavigationBarItem(
            selected = seciliRota == Rotalar.LISTE,
            onClick = { onSecim(Rotalar.LISTE) },
            icon = { Icon(Icons.AutoMirrored.Rounded.Assignment, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_odevler)) },
        )
        NavigationBarItem(
            selected = seciliRota == Rotalar.TAKVIM,
            onClick = { onSecim(Rotalar.TAKVIM) },
            icon = { Icon(Icons.Rounded.DateRange, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_takvim)) },
        )
        NavigationBarItem(
            selected = seciliRota == Rotalar.PROGRAM,
            onClick = { onSecim(Rotalar.PROGRAM) },
            icon = { Icon(Icons.Rounded.School, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_program)) },
        )
        NavigationBarItem(
            selected = seciliRota == Rotalar.ISTATISTIK,
            onClick = { onSecim(Rotalar.ISTATISTIK) },
            icon = { Icon(Icons.Rounded.PieChart, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_istatistik)) },
        )
    }
}

/**
 * "Yeni ödev" düğmesi: **hap** biçiminde uzatılmış FAB, sağ alt köşede.
 *
 * Eski düzen yuvarlak, ortalanmış ve yalnızca `+` simgeli bir düğmeydi;
 * dört sekmeye geçince orta nokta artık iki sekme arasındaydı ve barla
 * çakışıyordu. Taşınma iki sorunu da bitirdi:
 *
 *  - **Konum:** `Scaffold`'un `floatingActionButton` yuvası, düğmeyi alt
 *    menünün üstüne 16 dp boşlukla ve **uçta** yerleştirir. İçerik
 *    (liste/takvim) aynı anda 80 dp alt dolgu ödediği için son kart
 *    düğmenin altında kalmaz.
 *  - **Görünüm:** Uzatılmış form, hem eylemi yazdığı için daha açıklayıcı
 *    (`+` tek başına "ekle" mi "yeni sekme" mi belirsizdi) hem de hap
 *    kesim [CircleShape] ile düz daireden daha çağdaş duruyor. Kalem
 *    simgesi, kart üzerindeki "Düzenle" eylemiyle aynı dili konuşur.
 *
 * Düğme yalnızca **ödevle ilgili** iki sekmede çizilir: Ders Programı'nda
 * hem gereksiz hem de kartın altındaki "Ders ekle" satırının ortasına
 * biniyordu; İstatistik ise salt okunur. Takvim'de kalmasının nedeni,
 * ileride seçili güne ödev ekleme yolunun oradan geçmesi.
 *
 * Simgenin `contentDescription`'ı bilinçli olarak boş: etiket zaten
 * [Text] çocuğundan geliyor. Simgeye de bir açıklama eklemek "Ödev Ekle"
 * sözünün aynı düğümde iki kez durmasına yol açardı — tıpkı eski
 * yuvarlak düğmede olduğu gibi etiket tek bir yerden okunur.
 */
@Composable
private fun YeniOdevDugmesi(onClick: () -> Unit) {
    val renkler = MaterialTheme.colorScheme

    ExtendedFloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = renkler.primary,
        contentColor = renkler.onPrimary,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = null,
            )
        },
        text = { Text(stringResource(R.string.odev_ekle)) },
    )
}
