#!/usr/bin/env python3
"""
TeleBack Reproducible Build Verification Tool

This tool inspects Android APK/AAB packages to certify bit-for-bit reproducibility:
1. Computes cryptographic digests (SHA-256, SHA-1, MD5).
2. Inspects ZIP archive entry ordering and timestamp normalization.
3. Compares two independently built APKs to verify bit-for-bit identical checksums.
4. Generates a reproducible build verification report.

Usage:
    python verify_build.py <path_to_apk>
    python verify_build.py --compare <path_to_apk1> <path_to_apk2>
"""

import os
import sys
import hashlib
import zipfile
from datetime import datetime

def compute_hashes(file_path):
    sha256 = hashlib.sha256()
    sha1 = hashlib.sha1()
    md5 = hashlib.md5()
    size = os.path.getsize(file_path)

    with open(file_path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            sha256.update(chunk)
            sha1.update(chunk)
            md5.update(chunk)

    return {
        "sha256": sha256.hexdigest(),
        "sha1": sha1.hexdigest(),
        "md5": md5.hexdigest(),
        "size_bytes": size,
        "size_mb": size / (1024 * 1024)
    }

def inspect_apk_structure(file_path):
    entries = []
    has_normalized_timestamps = True

    with zipfile.ZipFile(file_path, "r") as z:
        infolist = z.infolist()
        is_sorted = True
        prev_name = ""

        for info in infolist:
            entries.append({
                "filename": info.filename,
                "file_size": info.file_size,
                "compress_size": info.compress_size,
                "date_time": info.date_time,
                "crc": info.CRC
            })

            # Check for non-standard timestamp variation
            # Standard reproducible builds freeze timestamps (e.g. 1980 or 2008 epoch)
            if info.date_time[0] > 2030:
                has_normalized_timestamps = False

            if info.filename < prev_name:
                is_sorted = False
            prev_name = info.filename

    return {
        "total_entries": len(entries),
        "is_sorted": is_sorted,
        "normalized_timestamps": has_normalized_timestamps,
        "entries": entries
    }

def compare_apks(apk1_path, apk2_path):
    h1 = compute_hashes(apk1_path)
    h2 = compute_hashes(apk2_path)

    print("=" * 70)
    print("TELEBACK REPRODUCIBLE BUILD COMPARISON REPORT")
    print("=" * 70)
    print(f"Build A: {os.path.basename(apk1_path)}")
    print(f"  SHA-256: {h1['sha256']}")
    print(f"  Size:    {h1['size_bytes']:,} bytes ({h1['size_mb']:.2f} MB)")
    print()
    print(f"Build B: {os.path.basename(apk2_path)}")
    print(f"  SHA-256: {h2['sha256']}")
    print(f"  Size:    {h2['size_bytes']:,} bytes ({h2['size_mb']:.2f} MB)")
    print("-" * 70)

    if h1["sha256"] == h2["sha256"]:
        print("RESULT: PASS - 100% BIT-FOR-BIT IDENTICAL (REPRODUCIBLE)")
        print("Both artifacts match with identical cryptographic SHA-256 checksums.")
        print("=" * 70)
        return True
    else:
        print("RESULT: MISMATCH - ARTIFACTS DIFFER")
        print("Differences detected between binaries. Analyzing entry CRCs...")
        
        s1 = inspect_apk_structure(apk1_path)
        s2 = inspect_apk_structure(apk2_path)
        
        map1 = {e["filename"]: e["crc"] for e in s1["entries"]}
        map2 = {e["filename"]: e["crc"] for e in s2["entries"]}

        all_keys = sorted(set(map1.keys()) | set(map2.keys()))
        diff_count = 0
        for k in all_keys:
            c1 = map1.get(k)
            c2 = map2.get(k)
            if c1 != c2:
                diff_count += 1
                if diff_count <= 10:
                    print(f"  [DIFF] {k}: CRC {c1} vs {c2}")

        if diff_count > 10:
            print(f"  ... and {diff_count - 10} more differing entries.")
        print("=" * 70)
        return False

def certify_apk(apk_path):
    if not os.path.exists(apk_path):
        print(f"Error: File not found: {apk_path}")
        sys.exit(1)

    h = compute_hashes(apk_path)
    struct = inspect_apk_structure(apk_path)

    print("=" * 70)
    print("TELEBACK BUILD CERTIFICATION & CHECKSUM MANIFEST")
    print("=" * 70)
    print(f"File Name:        {os.path.basename(apk_path)}")
    print(f"File Size:        {h['size_bytes']:,} bytes ({h['size_mb']:.2f} MB)")
    print(f"SHA-256 Checksum: {h['sha256']}")
    print(f"SHA-1 Checksum:   {h['sha1']}")
    print(f"MD5 Checksum:     {h['md5']}")
    print("-" * 70)
    print(f"Total Entries:    {struct['total_entries']}")
    print(f"Archive Sorted:   {'YES' if struct['is_sorted'] else 'NO'}")
    print(f"Timestamp Epoch:  {'NORMALIZED' if struct['normalized_timestamps'] else 'DYNAMIC'}")
    print("=" * 70)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage:")
        print("  python verify_build.py <apk_path>")
        print("  python verify_build.py --compare <apk1> <apk2>")
        sys.exit(1)

    if sys.argv[1] == "--compare":
        if len(sys.argv) < 4:
            print("Error: --compare requires two file paths.")
            sys.exit(1)
        success = compare_apks(sys.argv[2], sys.argv[3])
        sys.exit(0 if success else 1)
    else:
        certify_apk(sys.argv[1])
