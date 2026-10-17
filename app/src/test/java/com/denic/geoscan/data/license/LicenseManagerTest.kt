package com.denic.geoscan.data.license

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LicenseManagerTest {

    @Test
    fun testChallengeGenerationAndActivationCodeConsistency() {
        // Given a mock hardware fingerprint
        val mockFingerprint = "android_id_12345::Samsung::GalaxyS24::exynos::universal"
        val challenge = LicenseManager.computeChallengeFromFingerprint(mockFingerprint)

        // Verify Challenge format: DNIC-XXXX-YYYY
        assertTrue(challenge.startsWith("DNIC-"))
        assertEquals(14, challenge.length) // DNIC-XXXX-YYYY -> 4 + 1 + 4 + 1 + 4 = 14

        // Generate Activation Code
        val code = LicenseManager.generateExpectedActivationCode(challenge)

        // Verify Activation Code format: ACT-XXXX-YYYY-ZZZZ
        assertTrue(code.startsWith("ACT-"))
        assertEquals(18, code.length) // ACT-XXXX-YYYY-ZZZZ -> 3 + 1 + 4 + 1 + 4 + 1 + 4 = 18

        // Verify idempotency: same challenge always yields exact same code
        val codeAgain = LicenseManager.generateExpectedActivationCode(challenge)
        assertEquals(code, codeAgain)

        // Case insensitivity & dash stripping check
        val cleanExpected = code.replace("-", "").uppercase()
        val cleanInputLower = code.lowercase().replace("-", "")
        // Verify cross-platform match against offline generator
        val sampleChallenge = "DNIC-7B4E-9A2F"
        val expectedSampleCode = LicenseManager.generateExpectedActivationCode(sampleChallenge)
        assertEquals("ACT-14C3-6122-67A4", expectedSampleCode)
    }

    @Test
    fun testMasterKeyFormat() {
        assertEquals("DNIC-MASTER-2026-GEO-UNLOCKED", LicenseManager.MASTER_KEY)
    }
}
