#!/usr/bin/env python3
"""Writes launcher_version.json in the format Zalith's built-in updater reads.

Usage: make-update-json.py <output-path>
Environment: VERSION_CODE, VERSION_NAME, APK_SIZE (bytes), ABI (e.g. arm64-v8a), NOTES (markdown)
"""
import datetime
import json
import os
import sys

code = int(os.environ["VERSION_CODE"])
name = os.environ["VERSION_NAME"]
size = int(os.environ["APK_SIZE"])
abi = os.environ.get("ABI", "arm64-v8a")
notes = os.environ.get("NOTES", "").strip() or "New build."

size_keys = {"armeabi-v7a": "arm", "arm64-v8a": "arm64", "x86": "x86", "x86_64": "x86_64"}
file_size = {"all": 0, "arm": 0, "arm64": 0, "x86": 0, "x86_64": 0}
file_size[size_keys.get(abi, "all")] = size

# The updater picks the text by system language and does not handle missing keys, so fill all three.
def text(value):
    return {"en_us": value, "zh_cn": value, "zh_tw": value}

data = {
    "version_code": code,
    "version_name": name,
    "title": text("Pocket Client " + name),
    "description": text(notes),
    "published_at": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
    "file_size": file_size,
    "pre_release": False,
}

with open(sys.argv[1], "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
    f.write("\n")
