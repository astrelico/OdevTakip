# Ödev Takip

Android için sade bir ödev takip uygulaması. Ödevini ekle, teslim tarihini
işaretle; gecikenler listede en üstte, günün ödevleri Takvim sekmesinde
görünür. Arayüz tamamen **Türkçe**'dir.

## Özellikler

- Ödev ekleme / düzenleme / silme (başlık, açıklama, teslim tarihi ve saati)
- Otomatik durum: **Bekliyor → Gecikti → Tamamlandı**. Durum ayrıca bir iş
  karıştırmadan, listenin çizildiği an `sonTarih`'e göre hesaplanır.
- Filtre çipleri: Tümü / Geciken / Bugün / Yaklaşan
- **Takvim** sekmesi: yatay gün şeridi, seçili günün ödevleri, "Bugün" düğmesi
  şeridi bugüne döndürür
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
- Hatırlatma bildirimine dokununca doğrudan o ödevin **detay** ekranı açılır
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
├── OdevTakipApplication.kt  # yaşam döngüsü kökü: senkron + hatırlatma planı
├── data/                    # Room varlıkları, DAO, repository, tercihler
├── work/                    # WorkManager: durum senkronu + hatırlatma
├── bildirim/                # kanallar, bildirim içeriği, derin bağlantı
├── util/                    # Türkçe tarih biçimleri, dil sabitleme
└── ui/
    ├── OdevViewModel.kt     # liste ve takvimin VM'si
    ├── OdevKarti.kt         # liste/takvim ekranlarının ortak kartı
    ├── liste/  takvim/  form/  detay/  ayarlar/
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
