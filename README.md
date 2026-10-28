# AppLock 🔒

Aplikasi pengunci app Android dengan PIN 6 digit.

## Cara ganti ikon

1. Siapkan file `icon.png` ukuran **1024×1024 px**
2. Taruh di folder **root project** (sejajar sama `build.gradle`)
3. Push ke GitHub → Actions otomatis generate semua ukuran ikon

> Kalau tidak ada `icon.png`, GitHub Actions akan pakai ikon default ungu.

## Build APK

1. Push ke branch `main`
2. Buka tab **Actions** di GitHub
3. Tunggu build selesai (~5–10 menit)
4. Download APK dari bagian **Artifacts**

## Fitur

- 🔐 PIN 6 digit dengan konfirmasi
- 📱 Pilih app mana saja yang dikunci
- 🛡️ Background service monitor app aktif
- 🔄 Auto-start saat HP restart
