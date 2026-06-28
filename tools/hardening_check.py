#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]

DANGEROUS_PERMISSIONS = [
    "MANAGE_EXTERNAL_STORAGE",
    "READ_EXTERNAL_STORAGE",
    "WRITE_EXTERNAL_STORAGE",
    "READ_MEDIA_",
    "INTERNET",
    "CAMERA",
    "ACCESS_FINE_LOCATION",
    "ACCESS_COARSE_LOCATION",
    "READ_CONTACTS",
    "READ_PHONE_STATE",
    "RECORD_AUDIO",
    "SEND_SMS",
]

SECRET_PATTERNS = [
    re.compile(r"password\s*=", re.IGNORECASE),
    re.compile(r"api[_-]?key", re.IGNORECASE),
    re.compile(r"secret", re.IGNORECASE),
    re.compile(r"token\s*=", re.IGNORECASE),
]


def fail(message):
    print(f"FAIL: {message}")
    return 1


def read(path):
    return (ROOT / path).read_text(encoding="utf-8")


def check_manifest():
    manifest = read("app/src/main/AndroidManifest.xml")
    errors = []
    if 'android:allowBackup="false"' not in manifest:
        errors.append("android:allowBackup must be false for pilot builds")
    for permission in DANGEROUS_PERMISSIONS:
        if permission in manifest:
            errors.append(f"unapproved permission present: {permission}")
    exported = re.findall(r'android:exported="true"', manifest)
    if len(exported) != 1:
        errors.append("only the launcher activity may be exported")
    return errors


def check_backup_xml():
    errors = []
    for rel in ["app/src/main/res/xml/backup_rules.xml", "app/src/main/res/xml/data_extraction_rules.xml"]:
        text = read(rel)
        lowered = text.lower()
        if "sample" in lowered or "todo" in lowered:
            errors.append(f"{rel} still contains sample/TODO content")
        if "<exclude" not in lowered:
            errors.append(f"{rel} does not contain explicit exclusions")
    return errors


def check_raw_uri_patterns():
    errors = []
    for path in (ROOT / "app/src/main/java").rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        rel = path.relative_to(ROOT).as_posix()
        if "uri.toString()" in text and "DiagnosticRedactor" not in text:
            errors.append(f"raw uri.toString() use without redaction in {rel}")
        if "TODO: HARDENING_REQUIRED" in text:
            errors.append(f"hardening marker remains in {rel}")
    return errors


def check_secrets():
    errors = []
    ignored_parts = {".git", ".gradle", "build", ".idea", "Documentation"}
    for path in ROOT.rglob("*"):
        if not path.is_file():
            continue
        parts = set(path.relative_to(ROOT).parts)
        if parts & ignored_parts:
            continue
        if path.as_posix().endswith("tools/hardening_check.py"):
            continue
        if path.suffix.lower() in {".jks", ".keystore"}:
            errors.append(f"keystore file must not be committed: {path.relative_to(ROOT)}")
            continue
        if path.suffix.lower() not in {".java", ".kt", ".kts", ".xml", ".py", ".properties", ".md", ".txt"}:
            continue
        text = path.read_text(encoding="utf-8", errors="ignore")
        for pattern in SECRET_PATTERNS:
            if pattern.search(text):
                errors.append(f"suspicious secret literal in {path.relative_to(ROOT)}")
                break
    return errors


def check_schema():
    database = read("app/src/main/java/uk/co/hsim/assetaudit/data/db/AuditDatabase.java")
    match = re.search(r"DATABASE_VERSION\s*=\s*(\d+)", database)
    if not match:
        return ["DATABASE_VERSION not found"]
    version = match.group(1)
    schema = ROOT / "app/schemas/uk.co.hsim.assetaudit.data.db.AuditDatabase" / f"{version}.json"
    return [] if schema.exists() else [f"Room schema missing for database version {version}"]


def main():
    errors = []
    errors.extend(check_manifest())
    errors.extend(check_backup_xml())
    errors.extend(check_raw_uri_patterns())
    errors.extend(check_secrets())
    errors.extend(check_schema())
    if errors:
        for error in errors:
            fail(error)
        return 1
    print("Hardening check passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
