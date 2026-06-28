# TC21 Phase 7 Pilot Script

Use this checklist on the exact commit used to build the APK.

1. Install the release APK on a controlled Zebra TC21.
2. Open About / Diagnostics and record app version, database version, Android version, model, DataWedge profile name, and scanner action.
3. Confirm diagnostics do not show raw `content://` URIs, full filesystem paths, or full asset tags.
4. Import the audit workbook through Android document access. Confirm row counts, department counts, blank department handling, and leading-zero tags.
5. Open a high-count department and confirm the remaining list renders and scrolls.
6. Configure the DataWedge profile from the app or manually:
   - profile `AssetAudit_TC21`
   - package `uk.co.hsim.assetaudit`
   - barcode input enabled
   - keystroke output disabled
   - intent output enabled
   - action `uk.co.hsim.assetaudit.SCAN`
   - category `android.intent.category.DEFAULT`
   - broadcast delivery
7. Scan a known expected asset. Confirm the result, count update, remaining list update, and event evidence.
8. Scan the same asset again after the transport debounce window. Confirm duplicate result and no count increment.
9. Scan an asset from another department. Cancel movement, then repeat and confirm movement. Confirm previous department is preserved.
10. Scan an unknown barcode. Try an invalid new asset form, then a valid form. Confirm new asset count and report output.
11. Finish a department with remaining assets. Mark missing in one test case and skip with reason in another.
12. Open Reports, review readiness warnings, and generate an export package through Android document access.
13. Open the ZIP on a PC. Validate `export_manifest.json`, `updated_assets.csv`, `department_summary.csv`, exception reports, hashes, and row counts.
14. Force close and reopen the app. Confirm active session and recent export runs persist.
15. Run `python tools/hardening_check.py` on the same commit used for the APK.
