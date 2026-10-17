package com.denic.geoscan.data.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class RSurface3DEngineTest {

    @Test
    fun testMatrixConversionAlignsWithRSurface3DPhysics() {
        val engine = RSurface3DEngine()

        // ایجاد یک ماتریس ۳ سطر در ۳ ستون ژئوفیزیکی
        // سطر اول (بالاترین سطر فایل) در گرافیک اندروید بالاست، اما در دکارتی R پایین است!
        val mockText = """
            100  100  500
            100  100  100
            20   100  100
        """.trimIndent()

        val tempFile = File.createTempFile("surface_grid", ".txt").apply {
            writeText(mockText)
            deleteOnExit()
        }

        val report = engine.processSurface3D(tempFile)

        // تایید ابعاد شبکه ماتریسی (3x3)
        assertEquals(3, report.gridDimensions.first)
        assertEquals(3, report.gridDimensions.second)

        // محاسبه میانه داده‌ها: [20, 100, 100, 100, 100, 100, 100, 100, 500] -> میانه = 100
        assertEquals(100.0, report.median, 0.001)

        // بررسی آنومالی مثبت (500) در سطر 0 و ستون 2 فایل متنی
        // در پیش‌نمایش اصلاح شده، به دلیل معکوس شدن دکارتی Y، سطر 0 باید به رندر Y=2 منتقل شود تا چارت برعکس نشان داده نشود.
        val metalPoint = report.surfacePoints.find { it.point.z == 500.0 }
        assertNotNull(metalPoint)
        assertEquals(2.0f, metalPoint!!.renderY, 0.001f) // اصلاح موقعیت گرافیکی Y
        assertEquals(2.0f, metalPoint.renderX, 0.001f)
        assertEquals("#FF2A55", metalPoint.neonColor) // تخصیص رنگ قرمز نئون
        assertEquals(1.0f, metalPoint.renderZ, 0.001f) // بالاترین نقطه باید سقف ارتفاع (1.0) را داشته باشد

        // بررسی آنومالی منفی (20) در سطر 2 و ستون 0 فایل متنی
        // سطر آخر فایل متنی در دکارتی R کف چارت یعنی Y=0 است.
        val cavityPoint = report.surfacePoints.find { it.point.z == 20.0 }
        assertNotNull(cavityPoint)
        assertEquals(0.0f, cavityPoint!!.renderY, 0.001f) // کالیبره شده به کف پیش‌نمایش
        assertEquals(0.0f, cavityPoint.renderX, 0.001f)
        assertEquals("#00E5FF", cavityPoint.neonColor) // تخصیص رنگ فیروزه‌ای نئون
        assertEquals(0.0f, cavityPoint.renderZ, 0.001f) // عمیق‌ترین نقطه چارت در کف ارتفاع (0.0)
    }
}
