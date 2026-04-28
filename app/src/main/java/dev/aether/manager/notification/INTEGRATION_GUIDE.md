# Panduan Integrasi Sistem Notifikasi

## File yang ditambahkan

```
notification/
├── NotificationHelper.kt          ← Core: buat channel + kirim semua jenis notifikasi
├── LicenseNotificationChecker.kt  ← Cek expiry lisensi → showLicenseExpired/Expiring
├── UpdateNotificationHelper.kt    ← Cek update GitHub → showUpdateAvailable
└── NotificationScheduler.kt       ← WorkManager job harian (background)
```

---

## 1. AetherApplication.kt

Tambahkan `NotificationHelper.createChannels(this)` dan `NotificationScheduler.schedule(this)`
di `onCreate()` **setelah** init lainnya:

```kotlin
override fun onCreate() {
    super.onCreate()
    // ... existing code ...

    NotificationHelper.createChannels(this)     // wajib sebelum notifikasi pertama
    NotificationScheduler.schedule(this)         // jadwal cek harian (background)
}
```

---

## 2. MainActivity.kt — cek lisensi saat app dibuka

Di `onResume()` atau `onCreate()`:

```kotlin
override fun onResume() {
    super.onResume()
    // Cek status lisensi setiap kali app kembali ke foreground
    LicenseNotificationChecker.check(this)
}
```

---

## 3. UpdateViewModel.kt — notifikasi saat update ditemukan

Tambah trigger notifikasi di `checkForUpdate()`:

```kotlin
fun checkForUpdate() {
    viewModelScope.launch {
        _state.value = UpdateUiState.Checking
        _dismissed.value = false

        val currentCode = getCurrentVersionCode()
        val result = UpdateChecker.check(currentCode)

        _state.value = when (result) {
            is UpdateResult.UpdateAvailable -> {
                // Kirim notifikasi ke status bar
                UpdateNotificationHelper.checkAndNotify(getApplication())
                UpdateUiState.UpdateAvailable(result.info)
            }
            is UpdateResult.UpToDate        -> UpdateUiState.UpToDate
            is UpdateResult.Error           -> UpdateUiState.CheckError(result.message)
        }
    }
}
```

---

## 4. AetherService / BootReceiver — notifikasi tweak applied

Di `BootReceiver.applyOnBoot()` setelah apply berhasil:

```kotlin
val result = TweakApplier.apply(tweaks)
if (result.success) {
    NotificationHelper.showTweakApplied(context)  // muncul di status bar, silent
}
```

---

## 5. build.gradle — tambah WorkManager dependency

```kotlin
dependencies {
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
```

---

## Ringkasan notifikasi & channel

| Notifikasi          | Channel           | Importance | Suara  | Auto-dismiss |
|---------------------|-------------------|------------|--------|--------------|
| Update tersedia     | aether_update     | HIGH       | ✅ Ya  | ✅ Ya        |
| Lisensi expired     | aether_license    | HIGH       | ✅ Ya  | ✅ Ya        |
| Lisensi hampir habis| aether_license    | HIGH       | ✅ Ya  | ✅ Ya        |
| Tweak applied       | aether_general    | DEFAULT    | ❌ Tidak| 8 detik     |
| General/custom      | aether_general    | DEFAULT    | Opsional| ✅ Ya       |

Semua suara pakai `RingtoneManager.TYPE_NOTIFICATION` = suara notifikasi **default bawaan hp**.
