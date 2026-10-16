# Değişiklikler

Ödev Takip'teki tüm önemli değişiklikler bu dosyada belgelenir.

Biçim [Keep a Changelog][keep-a-changelog] düzenindedir; sürüm adları
[Semantic Versioning][semver] ile uyumludur (büyük.küçük.yama).

## [1.5.5] - 2026-10-16

### Eklendi

- **Deneme sıralaması** — Liste ekranındaki sıralama kontrolü sağ üstte
  **yüzen bir baloncuğa** taşındı: kart dilinde daire (yüzey, ince
  çerçeve, hafif gölge), listenin üstünde durur. Varsayılanda gri, başka
  bir sıralama seçiliyken mavinin kendisindedir — ödevler listesindeki
  düğmeyle aynı kural. Baloncuğa dokununca alttan yedi seçenek açılır:
  tarihe göre (en yeni / en eski), eklenme sırasına göre (en yeni / en
  eski), ada göre (A→Z / Z→A) ve nete göre (artan / azalan). Varsayılan
  DAO sırasıdır (tarih, en yeni en üstte) ve sıralama yalnızca düzeni
  değiştirir — **hiçbir kaydı gizlemez**.
- **Rutine ders başlığı** — Çalışma panelinde isteğe bağlı "Ders
  başlığı" alanı: aynı günün aynı dersten ikinci, üçüncü kayıtlarını
  birbirinden ayıran isimdir ("Matematik 2"). Boş bırakılırsa kartta
  yalnız ders adı görünür — tek kayıt giren kullanıcı hiçbir şey yazmak
  zorunda değildir.

### Değişti

- **Aynı dersten birden fazla kayıt** — Rutinde "bir günde bir dersten
  tek kayıt" kuralı kalktı: aynı ders tekrar girildiğinde mevcut satırın
  soruları değişmek yerine **ayrı bir satır** açılır, gün toplamı her iki
  kaydı da sayar. Düzenleme yalnızca o satırın kimliğiyle çalışır, başka
  bir kayda dokunmaz.
- **Rutin panelinde ders seçimi listeden** — Çip satırının yerini ödev
  formundakiyle aynı seçici kart aldı: dokununca alttan ders listesi
  açılır ve form, seçim sırasında arkada açık kalır ki doldurulan sayılar
  kaybolmasın. Düzenlemede ders artık **kilitli değil** — kilidin tek
  gerekçesi (gün, ders) unique indeksiydi, o indeks kalktı.
- **Veritabanı sürümü 8 → 9** — İki SQL: `rutin_kayitlari`'ne
  `baslik TEXT NOT NULL DEFAULT ''` kolonu eklenir, `(tarih, ders)`
  unique indeksi kaldırılır. Yükseltme **hiçbir satıra dokunmaz**: eski
  rutin kayıtları silinmez, taşınmaz, yeniden yazılmaz; yalnız yeni
  kolon belirir ve boş geldiği için kartta eskisi gibi ders adı görünür.
  `exportSchema = true` ve yıkıcı yedek yolu yine kapalıdır — migration
  eksikse uygulama veriyi silmek yerine hata verir.

## [1.5] - 2026-10-14

### Eklendi

- **Deneme sınavları** — Alt menüden açılan yeni bir bölüm: TYT ve AYT
  denemelerini ders ders kaydetmek için. Üç ekran taşır:
  - **Liste** — her kayıt tür etiketi, ad, tarih, doğru/yanlış ve büyük
    net rakamıyla bir kartta durur. Yeni ekleme sağ alttaki düğmeden.
  - **Detay** — üstte net, altında her dersin doğru/yanlış/boş dökümü ve
    en altta "Toplam N / N soru".
  - **Form** — ekleme ve düzenleme tek ekranda; kaydet düğmesi formun
    sonunda tam genişliktedir.
- **Hamburger menü ve yan menü** — Sol üstte yeni bir çizim; "Deneme
  sınavlarım", "Ders Rutinim" ve "Hakkında" burada durur. Ana gezinme
  alttaki dört sekmeyle aynen sürer.
- **Kilitli soru kotaları** — Deneme formunda her dersin soru sayısı
  ÖSYM'nin kotasıyla kilitli: TYT 120 (Türkçe 40, Sosyal 20, Matematik
  40, Fen 20), AYT sayısal 80 (Matematik 40, Fizik 14, Kimya 13,
  Biyoloji 13). Girdi aşıldığında satır kırmızıya döner ve kayıt
  engellenir. Kotalar tek kaynaktan gelir — ekranda elle yazılmış ikinci
  bir kopya yoktur.
- **TYT Sosyal ve Fen alt dal girişi** — Bu iki ders tek satır değil,
  **başlıktır**: asıl girdi altlarındaki Tarih / Coğrafya / Felsefe /
  Din Kültürü ve Fizik / Kimya / Biyoloji satırlarıdır. Ana dersin
  toplamı bu satırlardan hesaplanır, iki yerde aynı sayı tutulmaz.
  Detay ekranında ana dallar kalın ve öne çıkarken alt dallar
  girintili ve sönüktür — hangisinin toplam olduğu bakışta anlaşılır.
- **Ders Rutinim** — Yan menüden açılan yeni bir bölüm: kullanıcı her
  gün hangi dersten kaç soru çözdüğünü tutar. Aynı veri iki biçimde
  gösterilir, üst bardaki tek düğme aralarında geçiş yapar ve etiket
  her zaman **hedefi** söyler (Takvim ekranındaki kural aynen):
  - **Haftalık** — haftanın yedi günü kart kart. Her kartta günün adı ve
    tarihi, günün toplam sorusunu taşıyan baloncuk ve ders satırları
    durur; kayıt olmayan gün de karttır ve "Bu gün için kayıt yok" der.
  - **Aylık** — ay ızgarası: çözülen soruyla büyüyen baloncuklar,
    altında seçili günün kartı. Seçili gün iki biçimde ortaktır.
  - **Giriş tek yerdedir:** gün kartındaki ders satırı ya da "+ Satır
    ekle" alttan açılan paneli çağırır; ayrı bir form sayfası yoktur.
    Düzenlemede ders **kilittir** — (gün, ders) çifti değişirse unique
    indeks ikinci satır açar ve gün toplamı iki katına çıkardı;
    düzenlemenin iki eylemi sayıları düzeltmek ve kaydı silmektir.
  - Baloncuk **karekökle** ölçülür: 10 ile 20 soru arasındaki fark
    okunur kalırken 200 ile 210 arasındaki fark ayırt edilemez. Rakam
    daima gerçek toplamdır; baloncuk yalnızca görsel kısayoldur.
- **Deneme ekranlarında rozet, oran şeridi ve not kartı** — Liste ve
  detay ekranı artık aynı görsel dili paylaşır (`DenemeOranSeridi.kt`):
  - **Tür rozeti** — TYT / AYT artık düz metin değil tonlu zeminli
    rozet. Zemin `primaryContainer` / `secondaryContainer`'dan gelir;
    TYT ile AYT aynı mavi ailesinde kalır, yalnızca doygunluk değişir.
    Kartın üst satırı artık sayıyla yarışmıyor, kategori olarak okunuyor.
  - **Oran şeridi** — mavi doğru, kırmızı yanlış, gerekirse gri boş
    parça. "5 yanlış" tek başına iyi ya da kötü değildir; şeridin
    kırmızı oranı hükümü sayıyı okumadan verir. Sayısal olarak küçük ama
    görünmesi gereken parçalar kaybolmaz: sıfırdan büyük her parçanın en
    az **%2 payı** vardır — 3 yanlışın 120 sorudaki payı çizgiye dönerdi.
    Liste kartında şerit doğru/yanlış üzerinden, detayda kota (boş
    sorular dahil) üzerinden ölçeklenir.
  - **Ana dallara kendi şeridi** çizilir; alt testlere şerit çizilmez —
    kart gürültüye boğulurdu. Onun yerine alt testlerin soluna ana dala
    bağlayan **dikey kılavuz çizgisi** eklenir.
  - **Açıklama kartı** — not, tırnak ikonlu ve gövde boyutunda bir
    kartta durur. Sayılar tablodur, not günlüktür; ikisi aynı ağırlıkta
    olmamalı. Not yoksa kart çizilmez, tek satır metin görünür.

### Değişti

- **Deneme kaydı yalnızca sorular ve netlerden oluşur.** Toplam doğru,
  yanlış ve net ders sonuçlarından hesaplanır; ayrıca girilmez, dolayısıyla
  iki yer arasındaki tutarsızlık doğamaz. Net dört seçenekli varsayımla
  (doğru − yanlış ÷ 4) her okumada yeniden türetilir.
- **Ders rutini baloncukları mavi ölçekte koyulaşır** — alfa artık
  0.14–0.95. Eski aralık 0.12–0.30 idi ve koyu zeminde 20 ile 60 soru
  aynı soluk daire gibi duruyordu; boyut (karekök) ile koyuluk artık
  aynı yönde çalışır, renk körü olmayan kullanıcı sıralamayı okur. 0.6
  eşiğinin üstünde baloncuk metni `onPrimary`'a döner — dolu mavinin
  üstünde `primary` yazılmaz.
- **Bugün olan gün daha belirgin** — haftalık kartta bugünün kartı
  solunda 4 dp mavi şerit taşır ve zemini çevresinden daha açıktır;
  şerit kartın yuvarlatılmış köşesinde kesilir. Aylık izgarada bugün,
  **seçili değilken** ince bir halka ile belirginleşir (seçili hücre
  zaten dolu mavi olduğundan halka orada okunmaz).
- **Veritabanı sürümü 7 → 8**: `rutin_kayitlari` tablosu açıldı.
  Yükseltme yalnızca yeni tabloyu ve unique indeksini ekler; mevcut
  ödev, ders ve deneme kayıtlarına dokunmaz. `exportSchema = true` ve
  yıkıcı yedek yol kapalıdır — migration eksikse uygulama veriyi silmek
  yerine hata verir.

### Düzeltildi

- **Üç alıcıdaki `PendingResult.finish()` NPE'si giderildi** — Gün
  değişimi, widget güncelleme ve bildirim/widget hızlı tamamla
  alıcılarının üçünde de `finish()` artık null güvenli. Üretimde bu
  null gelmezdi, ama aynı desen üç yerde tekrar ediyordu ve hepsi aynı
  riski taşıyordu. Hızlı tamamla için NPE olsaydı ödül işi yarım kalır,
  kullanıcı bildirim düğmesine yeniden basamazdı.
- **Bir WorkManager testindeki durum yarışı** — Test, var olan bir işin
  `ENQUEUED → RUNNING` geçişine yakalanıp "durum değişti" diye
  düşüyordu. Ölçüt kimliğe çevrildi: iş tetiklense bile yeni bir UUID
  alır, kimlik değişimi bunu tam yakalar.

## [1.4] - 2026-10-11

### Eklendi

- **Üç yeni widget** — Artık dört ana ekran widget'ı var, her biri farklı
  bir işe yarar ve boyutu ona göre seçilir:
  - **Bugünün Özeti** (4 × 2, öncekinden kalan) — başlık, tarih, "N geciken ·
    N bugün" ve en fazla üç ödev satırı.
  - **Sayaçlar** (2 × 1) — geciken / bugün / toplam sayılarını üç büyük rakamla
    gösterir; bakışta anlaşılan tek şey buysa doğru seçim.
  - **Günün Listesi** (4 × 3) — geciken ve bugün teslim edilecek ödevleri
    **teslim süresiyle** listeler; satırlar daha uzun olduğu için okunması
    kolaydır.
  - **Sonraki Teslim** (2 × 2) — en yakın teslim tarihine sahip ödevi ve
    **kalan süreyi** gösterir ("2 gün", "Gecikti").

  Dördü de aynı çizim yolunu paylaşır: tek bir sorgu okunur, dört RemoteViews
  üretilir. Compose kullanılmaz — launcher'a şişme bindirmesin diye.
- **Widget satırında hızlı tamamla** — Ödev Özeti, Günün Listesi ve Sonraki
  Teslim satırlarının başındaki kutucuk, ödevi **uygulamayı açmadan**
  tamamlar ve widget'ı o anda tazeler. Boş halka dolu tik olur. Bildirimdeki
  "Tamamla" ile aynı davranış.

### Değişti

- **Arayüz "premium" düzene geçti** — işlevsellik aynı, yalnızca görünüm:
  - Üst barlara ayrı zemin ve altında ince bir çizgi (dokuz ekranda ortak).
  - Kartlarda duruş gölgesi + hairline çerçeve; ayırt edicilik artık çizgiden
    geliyor.
  - Alt menüye ayırıcı ve ekranla arasına boşluk eklendi.
  - Yeni ödev düğmesi mavi degradeli.
  - Temada köşe yarıçapları yumuşatıldı (6 / 10 / 14 / 20 / 28), harf aralığı
    sıfırlandı.
  - Boş durumlarda (liste, gün, istatistik) ikon artık bir dairenin içinde.
- **Kart gölgeleri koyu temada tamamen kalktı.** Koyu zeminde zaten
  görünmüyorlardı; karşılığında her kart için RenderNode bir blur katmanı
  çiziyordu. Ayırt ediciliği hairline zaten taşıyor. Açık temada 2 dp gölge
  aynen duruyor.
- **Sekme geçişleri kısaldı.** Varsayılan hareketli geçişte eski ve yeni
  ekran bir süre **aynı anda** çiziliyordu — hem kompozisyon iki kez ödeniyor
  hem de karede bindirim ikiye katlanıyordu. Geçiş 110 ms'lik kısa bir
  soldurmaya çevrildi.

### Performans

- **Soğuk açılış ~2900 ms'den ~545 ms'ye indi** (Galaxy A51, ölçüm). İki
  parça halinde geldi:
  - Release derlemesine geçiş tek başına açılışı ~750 ms'ye düşürdü; debug
    derlemede ART yorumlayarak çalışır ve ilk kompozisyon JIT ile derlenir.
  - **Baseline profile** eklendi: `:benchmark` modülü uygulamayı gerçek bir
    senaryoyla gezer ve yürütülen sıcak yolların profilini çıkarır. Profil
    APK'ya gömülür, Android kurulumda dexopt'a verir. `startup-prof.txt` de
    üretilir — yalnızca açılış sırasında dokunulan yolları kapsar.
- Kare çiziminde de iki kazanç: ölçü/düzenleme 0.18 ms'de kalırken sekme
  geçişlerinde kare p90 **80.7 ms'den 54.4 ms'ye** düştü.

## [1.3] - 2026-10-08

### Eklendi

- **Ana ekran widget'ı** — Widget seçicisinde "Ödev Takip · 4 × 2" olarak
  görünür: üstte uygulama adı ve bugünün tarihi, altında "N geciken ·
  N bugün" özeti, sonra en fazla üç ödev satırı ve kalanı söyleyen
  "+N daha". Sıra öncelik sırasıdır — geciken önce, sonra bugün, sonra
  ileri tarihli; her grupta en eski teslim üstte. Geciken satır,
  uygulamadaki rozet gibi kırmızıya döner. Kutuya tıklamak ana listeyi
  açar. Widget üç yerde kendini tazeler: ödev yazılınca, gün değişince
  ve kendisi eklenince. `updatePeriodMillis` bilerek sıfırdır — sistem
  en az 30 dakikada bir haber verir ve bu aralık geciken sayacını taze
  tutmaya yetmez.
- **İstatistik sayfası** — Alt gezinmede dördüncü sekme: toplam,
  yapılan, geciken sayıları ve **grafik kütüphanesi kullanılmadan**
  Canvas ile çizilmiş halka / pasta grafikleri. Renkler yalnızca
  **durum**dan gelir, grafik ders renklerinden bağımsızdır.
- **Arama ve sıralama** — Üst bardaki **büyüteç**, başlığın yerine açılan
  yuvarlak bir metin alanına dönüşür; başlık, açıklama ve ders adında
  arar (Türkçe `I`/`ı`/`i` eşleşmesi dahil). Yanındaki **sıralama**
  simgesi alttan açılan paneli çağırır: önerilen, ders adı, ada göre,
  yeni eklenen.
- **Ödevi kopyala** — Detay ekranının üst barında üçüncü simge. Kaynağın
  dersi, açıklaması, **teslim tarihi** ve **eki** yeni ödevde tekrar
  üretilir (ek kopyalanır, dosya adı çakışmaz); durum **bekliyor**
  olarak başlar, düzenlemeye gerek yoktur.
- **Aylık takvim görünümü** — Takvim'in üst barındaki tek düğme iki
  biçimi açıp kapatır. Varsayılan **gün şeridi**dir (±30 gün), **Aylık**
  dediğinde hafta başı pazartesi olan aylık ızgara gelir, **Günlük**
  dediğinde şeride döner. Seçim her iki biçimde ortaktır ve tercih
  olarak saklanır: uygulama kapatılıp açılınca seçili kalır.
- **Bildirimden hızlı tamamla** — Yaklaşan teslim ve tek ödevlik gecikme
  bildirimlerindeki **"Tamamla"** düğmesi, ödevi uygulamayı açmadan
  tamamlar ve o bildirimi kendiliğinden kaldırır.

### Değişti

- **Yeni ödev düğmesi** yenilendi: alt çubuğun üstünde, sağ alt köşede
  **genişletilmiş** yuvarlak buton olarak durur ve çizimini değiştirdi.
  Alt çubuk artık yalnızca dört sekmeyi taşır.
- **Takvim** ekrandaki varsayılan biçimi tam ekran aylık ızgaraydı;
  kullanıcı geri bildirimiyle önceki "gün gün" sıralı kutular geri
  getirildi ve ızgara tek düğmenin ardına alındı.
- Arka plan izlemeleri tek kapsam altında toplandı: ödevler her
  değiştiğinde hatırlatma planı **ve** ana ekran widget'ı birlikte
  tazelenir, hata yutulur ve akış yaşamaya devam eder.

## [1.2] - 2026-10-06

### Eklendi

- **Ders Programı** — Alt gezinmede üçüncü sekme: haftanın yedi günü için
  ders saatleri ızgarası. Gün çipleriyle gezinilir, satıra basınca alttan
  açılan panelden ders seçilir. Saat yok; satırlar "1. ders", "2. ders"
  … diye numaralanır ve seçilen ders haftanın her günü aynı yerde
  tekrar eder.
- **Programa ders ekleme / kaldırma** — Program ekranının altındaki
  **+ Ders ekle** satırı 9., 10., … dersi ekler. Yanındaki **Son dersi
  kaldır** satırı da 8. satıra kadar inebilir ve gün **0'a kadar
  boşaltılabilir**; boş gün artık metinle uyarılır. Dolu bir satır
  kaldırılacaksa önce onay istenir. Ders kaldırma **ilk 8 ders için de**
  geçerlidir.
- **Dosya / fotoğraf eki** — Ödev formundaki **Ek ekle** düğmesi tek bir
  menüyle iki yolu da açar: **Fotoğraf seç** sistem Photo Picker'ını
  (izin gerektirmez), **Dosya seç** de belge seçicisini. Dosya
  uygulamanın **özel** deposuna kopyalanır — ekran kapatılsa da okunur —
  ve ödev silinince ek de silinir, değiştirilince eskisi temizlenir.
  Detay ekranının altında **görsel önizleme** görünür (görsel olmayanda
  ad + tür + boyut kartı), fotoğrafın üzerine basılınca **tam ekran**
  açılır. Ana ekrandaki kartta ise ataş ikonuyla **Ekli** etiketi durur.

### Değişti

- **+ (Yeni ödev) düğmesi** yalnızca Ödevler ve Takvim'de görünür; Ders
  Programı ekranında gizlenir ki program satırlarıyla çakışmasın.
- Veritabanı sürümü **2 → 4**: `program` tablosu açıldı ve `odevler`
  tablosuna `ek` kolonu eklendi. Yükseltme mevcut ödev ve ders verisini
  olduğu gibi korur.

## [1.1] - 2026-10-05

### Eklendi

- **Ders seçimi** — Ödev ekleme/düzenleme formunda "Ders" kartına basınca
  açılan alt panelden ders seçilir. Ders zorunludur: seçilmeden kaydetme
  çalışmaz, hata satırı görünür.
- **Filtreler düğmesi** — Ödevler sayfasında çiplerin sağında daima görünür.
  Açılan alt panelden "Tüm dersler" ya da tek bir ders seçilerek hem liste
  hem üstteki sayaçlar süzülür; seçilen ders silinirse filtre
  kendiliğinden sıfırlanır.
- **Dersler ekranı** — Ayarlar → *Ders ekle, değiştir*. Ders ekleme,
  büyük/küçük harf duyarsız yinelenen adı reddetme ve onay diyaloğuyla
  silme. Silinen dersin ödevleri etkilenmez; varsayılan 9 ders ilk açılışta
  eklenir.
- **Kartta ders etiketi** — Ödev kartında başlığın altında ders adı görünür.
- **Aciliyet rozeti ve kırmızı çerçeve** — Teslim gününe girilmiş ya da
  **yarın** teslimi olan ve hâlâ tamamlanmamış ödev, kırmızı çerçeveli bir
  kutucuğa dönüşür ve durum rozetinin soluna **Acil** / **Yarın teslim**
  rozeti düşer. Teslim tarihi geçmiş (geciken) ödev ikinci uyarı almaz —
  onun zaten kırmızı tarih metni ve "Gecikti" rozeti vardır. Tamamlanan
  ödev asla acil sayılmaz. Rozet Ödevler ve Takvim kartlarında ortaktır.

### Değişti

- Veritabanı sürümü **1 → 2**: `odevler` tablosuna `ders` kolonu eklendi
  (eski kayıtlar boş bırakılır) ve `dersler` tablosu açıldı. Yükseltme mevcut
  ödev verisini olduğu gibi korur.
- Ders filtresi yalnızca Ödevler listesini etkiler; **Takvim**, seçilen
  günün tüm ödevlerini göstermeye devam eder.

## [1.0] - 2026-10-04

İlk sürüm.

### Eklendi

- Ödev ekleme, düzenleme ve silme; **bekliyor / gecikti / tamamlandı**
  durumu teslim tarihinden otomatik hesaplanır.
- Filtre çipleri: **Tümü / Geciken / Bugün / Yaklaşan**.
- **Takvim** ekranı: güne göre ödev listesi ve günlük sayaçlar.
- **Room** ile yerel veritabanı, **WorkManager** ile arka plan durum
  güncellemesi.
- **Geciken ödev bildirimleri**; aynı ödev 12 saatte bir tekrar bildirilmez.
- **Türkçe yerel ayarı** sabit: tarih ve saat biçimleri cihaz dilinden
  bağımsız olarak Türkçe.
- **"Odak Mavisi" teması**; sistem dinamik rengi kapalı.
- **Arayüz yenilemesi**: alt gezinme, kart / çip / rozet / form yeniden
  tasarımı ve kaydırılabilir tarih şeridi.
- **Ayarlar**: tema seçici (Açık / Koyu / Sistem) ve "tamamlananları gizle".
- **Kaydırarak tamamla**: sağa = tamamla, sola = geri al; aynı eylem
  TalkBack erişilebilirlik menüsünden de verilebilir.
- **Teslim öncesi hatırlatma**: 3 saat önce / teslim günü sabahı / 1 gün
  önce — bunlardan birden fazlası aynı anda seçilebilir.
- Ayarlarda **bildirim izni durumu** satırı ve sistem ayarlarına kısayol.
- **Uygulama ikonu** (hiçbir hazır varlık içe aktarılmadan çizildi).
- README'ye tasarım atfı (Figma topluluk dosyası, **CC BY 4.0**).

[keep-a-changelog]: https://keepachangelog.com/tr/1.1.0/
[semver]: https://semver.org/lang/tr/
[1.5.5]: https://github.com/astrelico/OdevTakip/releases/tag/v1.5.5
[1.5]: https://github.com/astrelico/OdevTakip/releases/tag/v1.5
[1.4]: https://github.com/astrelico/OdevTakip/releases/tag/v1.4
[1.3]: https://github.com/astrelico/OdevTakip/releases/tag/v1.3
[1.2]: https://github.com/astrelico/OdevTakip/releases/tag/v1.2
[1.1]: https://github.com/astrelico/OdevTakip/releases/tag/v1.1
