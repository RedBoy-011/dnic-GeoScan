# 🔐 Dnic-GeoScan Offline Cryptographic Licensing Engine Architecture

This document provides technical documentation on the Offline Hardware Challenge-Response Licensing System implemented in **Dnic-GeoScan**.

---

## 1. Quick Master Switch (Enable / Disable Licensing)

To facilitate rapid feature updates, debugging, and continuous delivery, a single global boolean toggle controls license enforcement:

📁 **File Location:**
`app/src/main/java/com/denic/geoscan/data/license/LicenseManager.kt`

```kotlin
object LicenseManager {
    /**
     * Master toggle for offline license enforcement:
     * - false: Activation barrier bypassed; all screens (Hub, Local, Remote, Settings) open immediately.
     * - true: Strict cryptographic activation enforced on launch.
     */
    const val IS_LICENSE_ENFORCEMENT_ENABLED = false
```

- When set to `false`, `LicenseManager.isActivated(context)` immediately returns `true`.
- The device challenge computation and UI card in Settings remain fully functional so developers can inspect the hardware fingerprint at any time.
- To re-enable strict commercial enforcement before a retail deployment, change this constant back to `true`.

---

## 2. Cryptographic Protocol & Workflow

The licensing protocol operates 100% offline using standard HMAC-SHA256:

1. **Device Fingerprint Extraction:**
   - Aggregates hardware indicators: `Android ID`, `Manufacturer`, `Model`, `Hardware Board`, `Product`.
   - Computes SHA-256 digest and formats a concise 14-character code:
     `DNIC-XXXX-YYYY`
2. **Offline Challenge Transmission:**
   - The user shares their `DNIC-XXXX-YYYY` challenge code with the system administrator.
3. **Cryptographic Key Derivation:**
   - The administrator runs the offline generator with the secret salt `DNIC_GEOSCAN_OFFLINE_SECRET_KEY_2026_@#$`.
   - The generator computes `HMAC-SHA256(SecretSalt, CleanChallenge)` and formats an 18-character key:
     `ACT-XXXX-YYYY-ZZZZ`
4. **Offline App Verification:**
   - The user inputs the key into the app.
   - The app independently computes the expected HMAC-SHA256.
   - If matched, an anti-tamper signature `SHA256("TAMPER_PROOF::" + challenge + "::" + key + "::" + salt)` is written to secure preferences.
5. **Emergency Master Override:**
   - `DNIC-MASTER-2026-GEO-UNLOCKED` unlocks any device for support situations.

---

## 3. Local Offline Generator Tools

The administrator tools reside on the developer machine at:
`C:\Users\arfaghani\Desktop\Dnic_License_Generator\`

- `DnicLicenseGenerator.bat` (1-click HTML5 GUI launcher)
- `DnicLicenseGenerator.html` (Standalone offline responsive Web app)
- `GenerateLicense.ps1` (PowerShell CLI script)
- `README_راهنمای_جنریتور.txt` (Text guide)

These files are protected by `.gitignore` and are never committed to public version control.
