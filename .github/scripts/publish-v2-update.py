#!/usr/bin/env python3
"""Builds the Pocket Client 2 update payload.

Usage: publish-v2-update.py <new.apk> <out_dir> [<prev_code>=<prev.apk> ...]
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
previous = []
for arg in sys.argv[3:]:
    prev_code, prev_path = arg.split("=", 1)
    previous.append((int(prev_code), prev_path))

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

# One patch per recent build, so a phone that skipped an update still gets a small download.
for prev_code, prev_apk in previous:
    if prev_code >= code or not os.path.isfile(prev_apk):
        continue
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
        print(f"Patch from {prev_code}: {size / 1048576:.1f} MB (full APK {info['apk_size'] / 1048576:.1f} MB)")
    else:
        os.remove(patch_out)
        print(f"Patch from {prev_code} too large ({size} bytes), dropped")

with open(os.path.join(out_dir, "pc_update.json"), "w", encoding="utf-8") as f:
    json.dump(info, f, ensure_ascii=False, indent=2)
    f.write("\n")
print(json.dumps(info, indent=2))
