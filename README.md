# Ödev Takip

Android için sade bir ödev takip uygulaması. Ödevini ekle, teslim tarihini
işaretle; gecikenler listede en üstte, günün ödevleri Takvim sekmesinde
görünür. Arayüz tamamen **Türkçe**'dir.

## Özellikler

- Ödev ekleme / düzenleme / silme (başlık, açıklama, teslim tarihi ve saati)
- Otomatik durum: **Bekliyor → Gecikti → Tamamlandı**. Durum ayrıca bir iş
  karıştırmadan, listenin çizildiği an `sonTarih`'e göre hesaplanır.
- Filtre çipleri: Tümü / Geciken / Bugünün / Yaklaşan
- **Takvim** sekmesi: yatay gün şeridi, seçili günün ödevleri, "Bugün" düğmesi
  şeridi bugüne döndürür
- Kart üzerindeki onay kutusuyla tek dokunuşta tamamla / geri al
- Açık ve koyu tema (sistem ayarını izler), dinamik renk kapalı
- WorkManager ile durum senkronu (arka planda gecikenlerin güncellenmesi)

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
├── MainActivity.kt          # gezinme grafiği + alt menü
├── data/                    # Room varlıkları, DAO, repository
├── work/                    # WorkManager işçisi
├── util/                    # Türkçe tarih biçimleri, dil sabitleme
└── ui/
    ├── OdevViewModel.kt     # tek ViewModel
    ├── OdevKarti.kt         # liste/takvim ekranlarının ortak kartı
    ├── liste/  takvim/  form/  detay/
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
