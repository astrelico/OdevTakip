package com.odevtakip.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.odevtakip.app.ui.OdevViewModel
import com.odevtakip.app.ui.detay.OdevDetayEkrani
import com.odevtakip.app.ui.form.OdevFormEkrani
import com.odevtakip.app.ui.liste.OdevListeEkrani
import com.odevtakip.app.ui.theme.OdevTakipTheme
import com.odevtakip.app.util.turkceyeSabitle

/** Uygulamanın gezinme rotaları. */
private object Rotalar {
    const val LISTE = "liste"
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
 * Tek ekranlı gezinme grafiği: liste → detay → form.
 *
 * [OdevViewModel] aktivite kapsamında tek örnek olarak tutulur; böylece
 * filtre seçimi ve liste durumu ekranlar arasında kaybolmaz.
 */
@Composable
private fun OdevUygulamasi(
    viewModel: OdevViewModel = viewModel(factory = OdevViewModel.Factory),
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Rotalar.LISTE,
    ) {
        composable(Rotalar.LISTE) {
            OdevListeEkrani(
                viewModel = viewModel,
                onYeniOdev = { navController.navigate(Rotalar.form()) },
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
