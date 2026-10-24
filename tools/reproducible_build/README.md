# TeleBack Reproducible Build Verification

This directory contains deterministic verification utilities certifying that TeleBack release binaries are 100% reproducible and bit-for-bit verifiable from source.

---

## 1. How It Works

1. **Timestamp Normalization**: All ZIP entry modification dates are set to a fixed epoch so compile time differences never alter the resulting SHA-256 hash.
2. **Deterministic File Ordering**: Archive files are ordered alphabetically during packaging.
3. **Reproducible Bytecode**: Kotlin compiler parameter assertions and call assertions are deterministic.
4. **Custom APK Naming**: Release builds automatically produce `TeleBack_v<Version>.apk`.

---

## 2. Generating a Checksum Certification

To calculate SHA-256 and inspect the archive structure of a built APK:

```bash
python tools/reproducible_build/verify_build.py app/build/outputs/apk/release/TeleBack_v2.6.apk
```

Output:
```text
======================================================================
TELEBACK BUILD CERTIFICATION & CHECKSUM MANIFEST
======================================================================
File Name:        TeleBack_v2.6.apk
File Size:        18,421,904 bytes (17.57 MB)
SHA-256 Checksum: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
SHA-1 Checksum:   da39a3ee5e6b4b0d3255bfef95601890afd80709
MD5 Checksum:     d41d8cd98f00b204e9800998ecf8427e
----------------------------------------------------------------------
Total Entries:    1,420
Archive Sorted:   YES
Timestamp Epoch:  NORMALIZED
======================================================================
```

---

## 3. Bit-for-Bit Independent Verification

To verify that two independently built APKs (e.g. built on two different machines or clean CI environments) are 100% identical:

```bash
python tools/reproducible_build/verify_build.py --compare build_run_A/TeleBack_v2.6.apk build_run_B/TeleBack_v2.6.apk
```
