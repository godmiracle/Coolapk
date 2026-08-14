# Ship Log: topic-product-page-redesign

**Date:** 2026-08-14
**Mode:** push
**Branch:** main
**Commit:** f97559417bb53ac7596c97f629042ac588d009e4
**Tree:** afc8c15c7970d436a3b23743bebf293ca1ae72fc
**Tag:** v1.0.1

## Delivery

- `main` pushed to `origin/main`.
- Tag `v1.0.1` pushed to `origin`.
- GitHub Actions release workflow succeeded: https://github.com/godmiracle/Coolapk/actions/runs/31789010432
- GitHub Release published: https://github.com/godmiracle/Coolapk/releases/tag/v1.0.1
- Release assets include `c001apk_v1.0.1.460.apk` and `SHA256SUMS`.

## Verification

- Rasen validation: passed (`topic-product-page-redesign`).
- Change tasks: 12/12 complete.
- Debug unit tests, debug APK, and lint: passed.
- Release APK build with `-PversionName=v1.0.1`: passed.
- Release APK signature verification: passed with v3 signing.
- Release APK 16 KB alignment verification: passed.
- Local release APK SHA-256: `385635ee04cc44de5ad9140c00ed40092b6d7f36a17e184662a4e8b341164f1b`.

## Real Device

- Local formal release APK installed successfully via `adb install -r`.
- `MainActivity` started successfully on the connected device.
- Installed package reported `versionName=v1.0.1`, `versionCode=460`.
- Startup log scan found no fatal exception or Android runtime crash.
- The device disconnected before the follow-up tap-through of the topic page; page interaction evidence is therefore pending reconnection.
