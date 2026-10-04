@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.ayarlar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odevtakip.app.R
import com.odevtakip.app.data.TemaSecenegi

/**
 * Ayarlar: tema seçimi ve liste görünürlüğü.
 *
 * Değerler anında uygulanır — burada "Kaydet" yoktur. Seçili tema
 * [com.odevtakip.app.ui.theme.OdevTakipTheme]'ı besleyen akışla aynı
 * kaynaktan okunduğu için dönüşte de korunur.
 *
 * Satırlar `selectable` / `toggleable` ile kurulur; işaret kutusunun kendisi
 * tıklanabilir değildir. Böylece TalkBack satırı tek öğe olarak okur,
 * iki ayrı odak oluşmaz.
 */
@Composable
fun AyarlarEkrani(
    viewModel: AyarlarViewModel,
    onGeri: () -> Unit,
) {
    val tema by viewModel.tema.collectAsStateWithLifecycle()
    val gizle by viewModel.tamamlananlariGizle.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ayarlar_baslik)) },
                navigationIcon = {
                    IconButton(onClick = onGeri) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.geri),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TemaKarti(
                secili = tema,
                onSecim = viewModel::temaAyarla,
            )

            ListeKarti(
                gizle = gizle,
                onDegistir = viewModel::tamamlananlariGizleAyarla,
            )

            Text(
                text = stringResource(R.string.ayarlar_alt_ipucu),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---- Tema ----

@Composable
private fun TemaKarti(
    secili: TemaSecenegi,
    onSecim: (TemaSecenegi) -> Unit,
) {
    val renkler = MaterialTheme.colorScheme

    AyarKarti(baslik = stringResource(R.string.ayarlar_tema_baslik),
        ipucu = stringResource(R.string.ayarlar_tema_ipucu)) {
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TemaSecenegi.entries.forEach { secenek ->
                TemaSatiri(
                    secenek = secenek,
                    secili = secenek == secili,
                    onClick = { onSecim(secenek) },
                )
            }
        }
    }
}

@Composable
private fun TemaSatiri(
    secenek: TemaSecenegi,
    secili: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = secili,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = secili, onClick = null)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = stringResource(secenek.etiketRes),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(secenek.aciklamaRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---- Liste görünürlüğü ----

@Composable
private fun ListeKarti(
    gizle: Boolean,
    onDegistir: (Boolean) -> Unit,
) {
    AyarKarti(
        baslik = stringResource(R.string.ayarlar_liste_baslik),
        ipucu = stringResource(R.string.ayarlar_liste_ipucu),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = gizle,
                    onValueChange = { onDegistir(!gizle) },
                    role = Role.Switch,
                )
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.gizleme_baslik),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.gizleme_aciklama),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = gizle, onCheckedChange = null)
        }
    }
}

// ---- Ortak kart ----

/**
 * Ayar grubu kartı.
 *
 * Liste kartlarıyla aynı görsel ayar (beyaz `surface`, 2 dp gölge, çerçevesiz);
 * ayarlar ekranı uygulamanın geri kalanından başka bir dil konuşmasın.
 */
@Composable
private fun AyarKarti(
    baslik: String,
    ipucu: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = baslik,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = ipucu,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
