package com.denic.geoscan.data.license

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Offline Cryptographic Challenge-Response Licensing Engine.
 * 
 * Flow:
 * 1. Mobile App computes a unique hardware-bound Device Challenge (e.g. DNIC-7B4E-9A2F).
 * 2. User transmits this Device Challenge to the software distributor/admin.
 * 3. Admin enters the Challenge into the local offline generator (DnicLicenseGenerator).
 * 4. Generator computes HMAC-SHA256 signature using the secret salt and yields ACT-XXXX-YYYY-ZZZZ.
 * 5. User enters the Activation Key into the app -> verified offline instantly.
 */
object LicenseManager {

    private const val PREFS_NAME = "dnic_offline_license_store"
    private const val KEY_IS_ACTIVATED = "is_app_activated"
    private const val KEY_SAVED_CHALLENGE = "device_challenge_code"
    private const val KEY_SAVED_LICENSE = "saved_activation_key"
    private const val KEY_SIGNATURE = "activation_signature"

    // Cryptographic secret salt shared between app and offline generator
    private const val SECRET_SALT = "DNIC_GEOSCAN_OFFLINE_SECRET_KEY_2026_@#$"
    
    // Emergency master key for developer diagnostics
    const val MASTER_KEY = "DNIC-MASTER-2026-GEO-UNLOCKED"

    /**
     * Retrieves or generates the permanent hardware-bound Device Challenge code.
     */
    fun getDeviceChallenge(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedChallenge = prefs.getString(KEY_SAVED_CHALLENGE, null)
        if (!savedChallenge.isNullOrBlank()) {
            return savedChallenge
        }

        val rawHardwareFingerprint = buildHardwareFingerprint(context)
        val generatedChallenge = computeChallengeFromFingerprint(rawHardwareFingerprint)

        prefs.edit().putString(KEY_SAVED_CHALLENGE, generatedChallenge).apply()
        return generatedChallenge
    }

    /**
     * Checks whether the application is permanently activated.
     * Includes anti-tamper signature verification against SharedPreferences XML manipulation.
     */
    fun isActivated(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isActivatedFlag = prefs.getBoolean(KEY_IS_ACTIVATED, false)
        if (!isActivatedFlag) return false

        val challenge = getDeviceChallenge(context)
        val savedKey = prefs.getString(KEY_SAVED_LICENSE, "") ?: ""
        val savedSig = prefs.getString(KEY_SIGNATURE, "") ?: ""

        val expectedSig = computeAntiTamperSignature(challenge, savedKey)
        return savedSig.isNotEmpty() && savedSig == expectedSig
    }

    /**
     * Validates an entered activation key against this device's challenge code.
     * If valid, commits the encrypted activation state.
     */
    fun verifyAndActivate(context: Context, enteredKey: String): Boolean {
        val cleanInput = enteredKey.trim().uppercase()
        if (cleanInput.isEmpty()) return false

        val challenge = getDeviceChallenge(context)

        // 1. Check Master Override Key
        if (cleanInput == MASTER_KEY || cleanInput.replace("-", "") == MASTER_KEY.replace("-", "")) {
            commitActivation(context, challenge, MASTER_KEY)
            return true
        }

        // 2. Compute Expected License Key
        val expectedKey = generateExpectedActivationCode(challenge)
        val cleanExpected = expectedKey.replace("-", "").uppercase()
        val cleanEntered = cleanInput.replace("-", "").uppercase()

        if (cleanEntered == cleanExpected) {
            commitActivation(context, challenge, expectedKey)
            return true
        }

        return false
    }

    /**
     * Computes the mathematical activation code from a Device Challenge code.
     * Used both by the app verifier and mirrored by the local admin generator.
     */
    fun generateExpectedActivationCode(challenge: String): String {
        val normalizedChallenge = challenge.replace("-", "").uppercase()
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(SECRET_SALT.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        mac.init(secretKeySpec)
        val hash = mac.doFinal(normalizedChallenge.toByteArray(StandardCharsets.UTF_8))
        val hex = hash.joinToString("") { "%02X".format(it) }

        val p1 = hex.substring(0, 4)
        val p2 = hex.substring(4, 8)
        val p3 = hex.substring(8, 12)
        return "ACT-$p1-$p2-$p3"
    }

    /**
     * Generates a device challenge string from raw hardware properties.
     */
    fun computeChallengeFromFingerprint(fingerprint: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(fingerprint.toByteArray(StandardCharsets.UTF_8))
        val hex = digest.joinToString("") { "%02X".format(it) }

        val c1 = hex.substring(0, 4)
        val c2 = hex.substring(4, 8)
        return "DNIC-$c1-$c2"
    }

    /**
     * Assembles physical hardware signatures.
     */
    private fun buildHardwareFingerprint(context: Context): String {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "FALLBACK_ID"
        } catch (_: Throwable) {
            "FALLBACK_ID"
        }
        val manufacturer = Build.MANUFACTURER ?: "GENERIC"
        val model = Build.MODEL ?: "DEVICE"
        val hardware = Build.HARDWARE ?: "HW"
        val board = Build.BOARD ?: "BOARD"

        return "$androidId::$manufacturer::$model::$hardware::$board"
    }

    private fun commitActivation(context: Context, challenge: String, key: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val signature = computeAntiTamperSignature(challenge, key)
        prefs.edit()
            .putBoolean(KEY_IS_ACTIVATED, true)
            .putString(KEY_SAVED_LICENSE, key)
            .putString(KEY_SIGNATURE, signature)
            .apply()
    }

    private fun computeAntiTamperSignature(challenge: String, key: String): String {
        val raw = "TAMPER_PROOF::$challenge::$key::$SECRET_SALT"
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(raw.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02X".format(it) }
    }

    /**
     * Debug helper to reset activation state.
     */
    fun resetActivation(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
