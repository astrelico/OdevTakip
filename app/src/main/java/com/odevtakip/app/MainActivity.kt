package com.odevtakip.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.odevtakip.app.ui.OdevViewModel
import com.odevtakip.app.ui.detay.OdevDetayEkrani
import com.odevtakip.app.ui.form.OdevFormEkrani
import com.odevtakip.app.ui.liste.OdevListeEkrani
import com.odevtakip.app.ui.takvim.TakvimEkrani
import com.odevtakip.app.ui.theme.OdevTakipTheme
import com.odevtakip.app.util.turkceyeSabitle

/** Uygulamanın gezinme rotaları. */
private object Rotalar {
    const val LISTE = "liste"
    const val TAKVIM = "takvim"
    const val FORM = "form/{odevId}"
    const val DETAY = "detay/{odevId}"

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OdevTakipTheme {
                OdevUygulamasi()
            }
        }
    }
}

/**
 * Uygulama iskeleti: alt menü + ortak "yeni ödev" düğmesi + gezinme grafiği.
 *
 * Çizim sırası şöyle kurulur:
 *
 *  - **Liste** ve **Takvim** ana ekranlardır; alt menü yalnızca onlarda görünür.
 *  - **Form** ve **detay** üst üste itilen tam ekran rotalardır; menü gizlenir
 *    böylece klavye/açılır listeyle boğuşmazlar.
 *  - Her ekran kendi üst barını diker. Bu yüzden dış `Scaffold`'un
 *    `contentWindowInsets`'i sıfırlanır; sistem çubuğu ücreti iki kez
 *    ödenirse başlık durum çubuğunun altına kayar.
 *
 * [OdevViewModel] aktivite kapsamında tek örnek olarak tutulur; böylece
 * filtre, seçili gün ve liste durumu ekranlar arasında kaybolmaz.
 */
@Composable
private fun OdevUygulamasi(
    viewModel: OdevViewModel = viewModel(factory = OdevViewModel.Factory),
) {
    val navController = rememberNavController()
    val geriAlabilir by navController.currentBackStackEntryAsState()
    val mevcutRota = geriAlabilir?.destination?.route
    val anaEkran = mevcutRota == Rotalar.LISTE || mevcutRota == Rotalar.TAKVIM

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

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        bottomBar = {
            if (anaEkran) {
                AltMenu(
                    seciliRota = mevcutRota,
                    onSecim = ::git,
                    onYeniOdev = {
                        navController.navigate(Rotalar.form()) { launchSingleTop = true }
                    },
                )
            }
        },
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
                )
            }

            composable(Rotalar.TAKVIM) {
                TakvimEkrani(
                    viewModel = viewModel,
                    onOdevSec = { navController.navigate(Rotalar.detay(it)) },
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
        }
    }
}

/**
 * Alt menü: iki sekme + ortada duran "yeni ödev" düğmesi.
 *
 * Düğme `Scaffold`'un FAB yuvasında değil menünün **içinde** duruyor; böylece
 * referanstaki gibi barın üstüne biner (yuvasında olsaydı barın tam üstünde
 * boşlukla asılı kalırdı). Çubuk genişliği neredeyse ekran kadar olduğu için
 * merkezdeki bu alan ikonlarla çakışmaz.
 */
@Composable
private fun AltMenu(
    seciliRota: String?,
    onSecim: (String) -> Unit,
    onYeniOdev: () -> Unit,
) {
    val renkler = MaterialTheme.colorScheme

    Box {
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
        }

        FloatingActionButton(
            onClick = onYeniOdev,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-22).dp),
            shape = CircleShape,
            containerColor = renkler.primary,
            contentColor = renkler.onPrimary,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.yeni_odev_ekle),
            )
        }
    }
}
