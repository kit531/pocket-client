#!/usr/bin/env python3
"""Builds the Pocket Client 2 update payload.

Usage: publish-v2-update.py <new.apk> <out_dir> [<prev_info.json> <prev.apk>]
Environment: VERSION_CODE, VERSION_NAME, NOTES, REPO (owner/name), ABI

Writes into out_dir:
  PocketClient2-<abi>.apk                       the full APK (copied)
  PocketClient2-<abi>-from-<prev>.patch         bsdiff patch from the previous build, if one is given
  pc_update.json                                what the app reads (see PocketUpdater.kt)
"""
import hashlib
import json
import os
import shutil
import sys

import bsdiff4


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


new_apk, out_dir = sys.argv[1], sys.argv[2]
prev_info = sys.argv[3] if len(sys.argv) > 4 else None
prev_apk = sys.argv[4] if len(sys.argv) > 4 else None

code = int(os.environ["VERSION_CODE"])
name = os.environ["VERSION_NAME"]
repo = os.environ["REPO"]
abi = os.environ.get("ABI", "arm64-v8a")
notes = os.environ.get("NOTES", "").strip() or "New build."
tag = f"v2-{code}"
base = f"https://github.com/{repo}/releases/download/{tag}"

os.makedirs(out_dir, exist_ok=True)
apk_name = f"PocketClient2-{abi}.apk"
apk_out = os.path.join(out_dir, apk_name)
shutil.copyfile(new_apk, apk_out)

info = {
    "code": code,
    "name": name,
    "notes": notes,
    "apk_url": f"{base}/{apk_name}",
    "apk_size": os.path.getsize(apk_out),
    "apk_sha256": sha256(apk_out),
    "patches": [],
}

if prev_info and prev_apk and os.path.isfile(prev_apk):
    with open(prev_info, encoding="utf-8") as f:
        prev = json.load(f)
    prev_code = int(prev["code"])
    if sha256(prev_apk) != prev["apk_sha256"]:
        print(f"Previous APK does not match its checksum, skipping the patch from {prev_code}")
    elif prev_code < code:
        patch_name = f"PocketClient2-{abi}-from-{prev_code}.patch"
        patch_out = os.path.join(out_dir, patch_name)
        print(f"Creating patch {prev_code} -> {code} ...", flush=True)
        bsdiff4.file_diff(prev_apk, apk_out, patch_out)
        size = os.path.getsize(patch_out)
        # A patch that is not much smaller than the APK is not worth it.
        if size < info["apk_size"] * 0.7:
            info["patches"].append({
                "from": prev_code,
                "url": f"{base}/{patch_name}",
                "size": size,
                "sha256": sha256(patch_out),
            })
            print(f"Patch size: {size / 1048576:.1f} MB (full APK {info['apk_size'] / 1048576:.1f} MB)")
        else:
            os.remove(patch_out)
            print(f"Patch too large ({size} bytes), dropped")

with open(os.path.join(out_dir, "pc_update.json"), "w", encoding="utf-8") as f:
    json.dump(info, f, ensure_ascii=False, indent=2)
    f.write("\n")
print(json.dumps(info, indent=2))
