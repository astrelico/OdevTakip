# Ödev Takip

Android için sade bir ödev takip uygulaması. Ödevini ekle, teslim tarihini
işaretle; gecikenler listede en üstte, günün ödevleri Takvim sekmesinde
görünür. Arayüz tamamen **Türkçe**'dir.

## Özellikler

- Ödev ekleme / düzenleme / silme (ders, başlık, açıklama, teslim tarihi ve saati)
- **Dosya / fotoğraf eki** — formdaki **Ek ekle** düğmesi tek bir menüyle iki
  yolu da açar: **Fotoğraf seç** sistem Photo Picker'ını (izin gerekmez),
  **Dosya seç** de SAF belge seçicisini. Dosya, uygulamanın **özel** deposuna
  kopyalanır — hiçbir izin gerekmez, ekran kapatılsa da okunur — ve ödev
  silinince ek de silinir, değiştirilince eskisi temizlenir. Detay
  ekranının altında **görsel önizleme** görünür (görsel olmayanda ad + tür +
  boyut kartı); fotoğrafın üzerine basılınca **tam ekran** açılır. Ana
  ekrandaki kartta ise ataş ikonuyla **Ekli** etiketi durur
- **Ödevi kopyala** — detay ekranının üst barındaki kopya simgesi kaynağı
  okuyup her şeyi (başlık, açıklama, ders, teslim tarihi, ek) forma doldurur;
  Kaydet **yeni** bir kayıt açar. Kopya taze bir *Bekliyor* ve kendi kimliğiyle
  gelir, ek dosyası ise **çoğaltılır** — iki kayıt aynı adı taşısaydı birini
  silmek ötekinin ekini de silerdi. Kaydettikten sonra liste ekranına dönülür,
  yeni kart orada durur
- Otomatik durum: **Bekliyor → Gecikti → Tamamlandı**. Durum ayrıca bir iş
  karıştırmadan, listenin çizildiği an `sonTarih`'e göre hesaplanır.
- **Dersler**: formda zorunlu ders seçimi (alttan açılan panel), kartın
  üstünde ders etiketi ve Ayarlar → **Ders ekle-değiştir** ekranından
  ekleme / silme. Varsayılan dokuz ders kurulumda hazır gelir; bir dersi
  silmek onu kullanan ödevleri silmez, yalnızca listeden kalkar
- **Aciliyet**: teslim gününe girilmiş ya da **yarın** teslimi olan ve hâlâ
  tamamlanmamış ödevler kırmızı çerçeveli bir kutucuğa dönüşür; durum
  rozetinin soluna **Acil** / **Yarın teslim** rozeti düşer. Gecikenler
  ayrıca kırmızı yazı ve "Gecikti" rozetiyle durur, ikinci uyarı almaz
- Filtre çipleri: Tümü / Geciken / Bugün / Yaklaşan. Çiplerin sağındaki
  **Filtreler** tuşu ödevleri derse göre süzer (yalnızca Ödevler sekmesinde;
  Takvim etkilenmez), açıkken tuşun yazısı seçili ders adına döner
- **Arama**: üst bardaki **büyüteç** başlığın yerine bir metin alana
  dönüştürür ve klavye kendiliğinden açılır; yazarken liste anında süzülür.
  Başlık, açıklama **ve** ders adı taranır — bir ders adı yazıldığında o
  dersteki her ödev çıkar. Karşılaştırma Türkçe'ye duyarlıdır: `ING`
  araması *İngilizce*'yi, `SINIF` araması *Sınıf*'ı bulur. Büyüteç yanındaki
  düğme **sıralamayı** açar: Önerilen / Ders adına göre / Ödev adına göre /
  En yeni eklenen; varsayılan dışına çıkıldığında düğmenin rengi değişir.
  Aramadan çıkarken metin de temizlenir, bir sonraki açılışta sürpriz bir
  filtre kalmaz
- **Takvim** sekmesi: **iki biçim**, üst bardaki tek düğmeyle geçiş. Varsayılan
  **gün şeridi** — bugünün çevresinde ±30 kutu yan yana, altındaki nokta o
  günde ödev olduğunu söyler, "Bugün" şeridi bugüne kaydırır. **Aylık**
  düğmesi aylık takvim ızgarasına geçer: ayın tamamı tek bakışta, başlıktaki
  oklarla ay gezinilir ve seçim de yeni aya taşınır (gün numarası korunur,
  uzunluğuna göre kırpılır) — böylece ızgaradaki ay ile alttaki liste hep
  aynı ayda kalır. Düğme **hedefe** ad verir: ızgara açıkken "Günlük" der.
  Biçim tercih olarak saklanır, seçili gün ise iki biçimde ortaktır; aşağıda
  her ikisinde de o günün ödevleri aynı kartlarla listelenir
- **Ders Programı** sekmesi (alt bardaki üçüncü sekme): haftanın gününü çiplerden
  seç, o günün **8 ders saatinin** her birine elle ders ata. Satıra dokununca
  mevcut dersler alttan açılır; dolu bir saatte **"Bu dersi boşalt"** seçeneği
  de görünür. Kartın altındaki **"Ders ekle"** o güne 9., 10., … satırını açar,
  **"Son dersi kaldır"** geri indirir — **0 satıra kadar**, yani dersi olmayan
  bir gün (hafta sonu) tümüyle boşaltılabilir; gün bu noktaya inince kartta
  yalnızca **"Ders ekle"** kalır. Satır sayısı güne
  özeldir — bir gün uzatıldığında diğerleri etkilenmez — ve ayrı bir yerde
  tutulmaz: ekran satırları doğrudan veritabanından sayar. Dolu bir satır
  kaldırılırken ders adıyla birlikte onay sorulur. Program haftalık tekrarlanır,
  veritabanında kalıcıdır ve yalnızca kendini etkiler — ödev listesine ya da
  takvime dokunmaz
- **İstatistik** sekmesi (alt bardaki dördüncü sekme): duruma göre **halka
  (pasta) grafiği** — merkezde tamamlama yüzdesi, sağında renk + adet + yüzde
  göstergeleri —, Toplam / Tamamlanan / Geciken / Bekleyen sayı taşları, **son
  7 günün** tamamlama sütunları ve derslere göre **yığılı çubuklar**. Grafikler
  uygulamanın kendisi tarafından Canvas ile çizilir, dışarıdan grafik
  kütüphanesi alınmaz. Özet filtrelerden **bağımsızdır**: listede hangi çip
  seçili olursa olsun tüm kayıtları sayar
- Kartı **sağa kaydırarak** tamamla, tamamlanmışken **sola kaydırarak** geri
  al; detay ekranındaki büyük düğme de aynen çalışır
- **Ayarlar** ekranı: tema seçimi (Sistem / Açık / Koyu), "Tamamlananları
  gizle" ve teslim öncesi hatırlatmalar; tercihler cihazda kalıcı
  saklanır, değişiklik anında uygulanır. Bildirim izni kapalıysa hatırlatma
  kartında uyarı satırı ve sistem ayarlarına kısayol belirir
- **Teslim öncesi hatırlatma**: 3 saat önce / teslim günü sabahı / 1 gün önce
  aralıklarından **birden fazlası seçilebilir** (ör. hem 1 gün önce hem teslim
  günü sabahı). İşaretlerin tamamı kaldırıldığında hatırlatma kapanır ve arayüz
  bunu kartta ayrıca yazar. Her aralık için ödev başına tek seferlik iş kurulur
  ve hepsi aynı bildirim kimliğine yazar; böylece sırayla gelen hatırlatmalar
  üst üste yığılmaz, yenisi eskisinin yerini alır. Ödev tamamlanınca ya da
  silinince hem bekleyen işler hem gölgedeki bildirim kaldırılır
- Hatırlatma bildirimine dokununca doğrudan o ödevin **detay** ekranı açılır;
  bildirimdeki **"Tamamla"** düğmesi ise ödevi uygulamayı açmadan tamamlayıp o
  bildirimi kendiliğinden kaldırır. Eylem yalnızca **tek** ödevi anlatan
  bildirimlerde belirir (yaklaşan teslim ve tek ödevlik gecikme); birden çok
  ödevin sayıldığı gecikme bildiriminde hangisinin kastedildiği belirsiz
  olurdu
- **Ana ekran widget'ı**: üstte uygulama adı ve bugünün tarihi, altında
  "N geciken · N bugün" özeti, sonra en fazla üç ödev satırı ve kalanı söyleyen
  "+N daha". Sıra öncelik sırasıdır — geciken önce, sonra bugün, sonra ileri
  tarihli; her grupta en eski teslim üstte. Geciken satır, uygulamadaki rozet
  gibi kırmızıya döner. Kutuya tıklamak ana listeyi açar. Widget üç yerde
  kendini tazeler: ödev yazılınca, gün değişince ve kendisi eklenince;
  `updatePeriodMillis` bilerek sıfırdır, çünkü sistem en az 30 dakikada bir
  haber verir ve bu aralık geciken sayacını taze tutmaya yetmez. Widget'ı
  launcher şişirdiği için renkler XML'e kopyalanmış tema tonlarıdır ve sistem
  koyu temasını izler
- Açık ve koyu tema, dinamik renk kapalı
- WorkManager ile durum senkronu (arka planda gecikenlerin güncellenmesi)
  ve teslim öncesi hatırlatmalar

## Teknoloji

| Katman | Seçim |
| --- | --- |
| Arayüz | Jetpack Compose + Material 3 |
| Mimari | Tek modül, MVVM + Repository |
| Saklama | Room |
| Gezinme | navigation-compose |
| Arka plan | WorkManager |
| Widget | RemoteViews (`res/layout`) — launcher şişirdiği için Compose değil |
| Test | JUnit (birim) + Room instrumented testleri |

## Derleme

```bash
./gradlew assembleDebug      # hata ayıklama APK
./gradlew test               # birim testleri
./gradlew connectedDebugAndroidTest   # cihaz/emülatör testleri
```

## Proje yapısı

```
app/src/main/java/com/odevtakip/app/
├── MainActivity.kt          # gezinme grafiği + alt menü + tema seçimi
├── OdevTakipApplication.kt  # yaşam döngüsü kökü: senkron + hatırlatma + widget
├── data/                    # Room varlıkları, DAO, repository, tercihler
├── work/                    # WorkManager: durum senkronu + hatırlatma
├── bildirim/                # kanallar, bildirim içeriği, derin bağlantı
├── widget/                  # ana ekran widget'ı: sistem sağlayıcısı + çizim
├── util/                    # Türkçe tarih biçimleri, dil sabitleme
└── ui/
    ├── OdevViewModel.kt     # liste ve takvimin VM'si
    ├── OdevKarti.kt         # liste/takvim ekranlarının ortak kartı
    ├── liste/  takvim/  form/  detay/  ayarlar/
    ├── dersler/           # ders seçim paneli + ders ekle-değiştir ekranı
    ├── program/           # haftalık ders programı: gün çipleri + ders saatleri
    └── theme/               # Odak Mavisi paleti (açık/koyu)
```

## Tasarım atıfı

Arayüzün düzeni ve hiyerarşisi şu referans tasarımdan esinlenilmiştir:

- **Task management & to-do list app** — [Neser U. (@neseru)](https://www.figma.com/community/file/1143575071825582037/task-management-to-do-list-app)
- Lisans: [CC BY 4.0 — Creative Commons Attribution 4.0](https://creativecommons.org/licenses/by/4.0/)

Referans yalnızca **düzen** (üst bar, filtre çipleri, kart yapısı, alt gezinme,
gün şeridi) için kullanılmıştır. Renk paleti mavi tonda tutulmuş; tüm ikon,
çizim ve bileşenler uygulama içinde Jetpack Compose ile **native** olarak
yazılmıştır. Tasarımdan hiçbir SVG, PNG veya diğer bir varlık içe
aktarılmamıştır.

Uygulama ikonu da aynı kuraldadır. `drawable/ic_launcher_background.xml`,
`ic_launcher_foreground.xml` ve `ic_launcher_monochrome.xml` içindeki çizimler
(beyaz ödev kartı, onay işareti, "Odak Mavisi" zemin) elle yazılmış
vektörlerdir. `minSdk` 24 olduğundan adaptive icon'un kullanamadığı API 24/25
için gereken `mipmap-*/ic_launcher*.png` dosyaları ise `tools/uret-ikon.ps1`
tarafından **aynı ölçülerden** üretilir — yine içe aktarılmış tek bir görsel
yoktur.
