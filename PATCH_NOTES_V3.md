# Aether Manager v3.0 Patch Notes

## UI / Layout
- HomeScreen dirombak menjadi dashboard v3 dengan hero card, CPU/GPU realtime gauge, thermal strip, memory/storage progress, dan power status card.
- SettingsScreen memakai toggle pill ON/OFF baru agar tidak hanya switch standar.
- TweakScreen toggle feature diganti menjadi pill ON/OFF yang lebih clean.
- AppProfileScreen toggle utama, per-app enable, editor enable, dan extra tweak diganti menjadi toggle pill v3.
- Color palette theme digeser ke gaya Aether v3 biru/cyan modern.
- Launcher icon adaptive baru untuk Android 8+ dan notification icon baru.

## Monitor CPU/GPU
- CPU frequency membaca `policy*/scaling_cur_freq` + `cpu*/cpufreq` dengan fallback `/proc/cpuinfo`.
- CPU governor membaca policy cpufreq terlebih dahulu.
- GPU usage Qualcomm KGSL `gpubusy` sekarang memakai delta counter agar persentase lebih akurat.
- GPU monitor ditambah fallback untuk Mali/MediaTek/Exynos devfreq, mfg, g3d, ged, dan kgsl.
- Root fallback monitor GPU juga diperluas.

## Tweak Engine / Root Script
- Devfreq GPU helper dibuat lebih universal: `available_frequencies`, `available_freqs`, governor fallback, min/max freq.
- GPU performance dan GPU frequency lock sekarang apply ke semua path devfreq GPU/Mali/KGSL/G3D/MFG yang tersedia.
- Boot service menginisialisasi RootManager sebelum reapply tweak.
- Boot rapid counter direset setelah apply sukses supaya safe mode tidak salah trigger.
- MainViewModel bug duplikasi `readLocalTweaks()` diperbaiki.
- Monitor loop dibuat sedikit lebih responsif.

## Catatan Build
- Gradle build belum bisa dijalankan di sandbox ini karena wrapper perlu download Gradle 8.13 dari `services.gradle.org`, sedangkan environment tidak punya akses internet.
- Jalankan build di Termux/PC dengan internet: `./gradlew :app:assembleRelease --no-daemon --stacktrace`.
