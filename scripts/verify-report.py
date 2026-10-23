#!/usr/bin/env python3
"""Verify a report ZIP without extracting it. A checksum manifest is NOT an authenticated signature."""
import argparse, hashlib, json, zipfile
from pathlib import PurePosixPath

def verify(path):
    with zipfile.ZipFile(path) as archive:
        names=archive.namelist()
        if len(names)!=len(set(names)):raise ValueError("Duplicate ZIP entries")
        if len(names)>20000:raise ValueError("Too many ZIP entries")
        if sum(info.file_size for info in archive.infolist())>150*1024*1024:raise ValueError("Package exceeds verifier size bound")
        for name in names:
            if PurePosixPath(name).is_absolute() or ".." in PurePosixPath(name).parts or "\\" in name:raise ValueError("Unsafe path")
        manifest=json.loads(archive.read("manifest.json"))
        if manifest.get("algorithm")!="SHA-256":raise ValueError("Unsupported algorithm")
        expected={"manifest.json"}
        for entry in manifest["files"]:
            name=entry["path"]
            if name in expected:raise ValueError("Duplicate or self-referential manifest entry")
            expected.add(name); data=archive.read(name)
            if len(data)!=entry["bytes"] or hashlib.sha256(data).hexdigest()!=entry["sha256"]:raise ValueError(f"Integrity mismatch: {name}")
        if expected!=set(names):raise ValueError("Unlisted or missing files")
        return len(expected)-1

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument("archive");args=parser.parse_args()
    try:count=verify(args.archive)
    except (ValueError,KeyError,zipfile.BadZipFile,OSError) as error:raise SystemExit(f"FAILED: {error}")
    print(f"PASS: {count} files match the supplied manifest. This does not authenticate the author or certify equipment.")
if __name__=="__main__":main()
