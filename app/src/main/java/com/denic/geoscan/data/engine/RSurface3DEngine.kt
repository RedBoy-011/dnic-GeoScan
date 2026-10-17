package com.denic.geoscan.data.engine

import java.io.File
import kotlin.math.abs

data class RPoint3D(val x: Double, val y: Double, val z: Double)

data class RSurfacePoint(
    val point: RPoint3D,
    val deltaZ: Double,      // اختلاف با خط مبنا (میانه)
    val neonColor: String,   // رنگ ژئوفیزیکی نئون
    val renderX: Float,      // مختصات اصلاح شده برای پیش‌نمایش اندروید
    val renderY: Float,      // مختصات اصلاح شده برای پیش‌نمایش اندروید
    val renderZ: Float       // ارتفاع نرمالایز شده [0,1]
)

data class RSurfaceReport(
    val median: Double,
    val maxMetalPeak: Double,
    val deepestCavity: Double,
    val gridDimensions: Pair<Int, Int>, // تعداد (Rows x Columns) منطبق بر R
    val surfacePoints: List<RSurfacePoint>,
    val theoreticalAnomalyRatio: Double = 0.0,
    val estimatedDepthMeters: Double = 0.0,
    val targetClassificationFa: String = "",
    val targetClassificationEn: String = "",
    val interpretationFa: String = "",
    val interpretationEn: String = ""
)

/**
 * R surface3d matrix & Cartesian geophysical engine.
 * Calibrates row-major/column-major inversion, flips Y axis to match R's bottom-left Cartesian (0,0),
 * and normalizes Z-height to [0.0, 1.0] to prevent extreme spike distortion.
 */
class RSurface3DEngine {

    /**
     * پارس و کالیبره کردن ماتریس دو بعدی دقیقاً بر اساس فرمت surface3d در R از روی فایل
     */
    fun processSurface3D(file: File): RSurfaceReport {
        if (!file.exists()) {
            return RSurfaceReport(0.0, 0.0, 0.0, Pair(0, 0), emptyList())
        }
        val lines = file.readLines()
        return processSurface3DLines(lines)
    }

    /**
     * پردازش خطوط متنی ماتریس یا مختصات بر اساس استاندارد دکارتی R surface3d
     */
    fun processSurface3DLines(rawLines: List<String>): RSurfaceReport {
        val lines = rawLines
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("//") && !it.startsWith(";") }

        if (lines.isEmpty()) {
            return RSurfaceReport(0.0, 0.0, 0.0, Pair(0, 0), emptyList())
        }

        // بررسی اینکه آیا فایل به صورت سه‌تایی X, Y, Z است یا ماتریس خالص
        val firstTokens = lines.first().split("[,;\\s\\t#|]+".toRegex()).filter { it.isNotEmpty() }
        val isTripletFormat = lines.size > 2 && firstTokens.size in 3..6 &&
                lines.take(10).all { l ->
                    val toks = l.split("[,;\\s\\t#|]+".toRegex()).filter { it.isNotEmpty() }
                    toks.size >= 3 && toks[0].toDoubleOrNull() != null && toks[1].toDoubleOrNull() != null && toks[2].toDoubleOrNull() != null
                }

        if (isTripletFormat) {
            return processTripletsAsR(lines)
        }

        // استخراج ماتریس اولیه Z
        val zMatrix = lines.map { line ->
            line.split("[,;\\s\\t#|]+".toRegex())
                .filter { it.isNotEmpty() }
                .mapNotNull { it.toDoubleOrNull() }
        }.filter { it.isNotEmpty() }

        if (zMatrix.isEmpty() || zMatrix.first().isEmpty()) {
            return RSurfaceReport(0.0, 0.0, 0.0, Pair(0, 0), emptyList())
        }

        val rowsCount = zMatrix.size
        val colsCount = zMatrix.first().size

        // ۱. تبدیل به فرمت خطی و محاسبه میانه (Ground Zero)
        val allZValues = zMatrix.flatten()
        val sortedZ = allZValues.sorted()
        val median = if (sortedZ.size % 2 == 0) {
            (sortedZ[sortedZ.size / 2 - 1] + sortedZ[sortedZ.size / 2]) / 2.0
        } else {
            sortedZ[sortedZ.size / 2]
        }

        val minZ = sortedZ.first()
        val maxZ = sortedZ.last()
        val zRange = if (maxZ - minZ == 0.0) 1.0 else maxZ - minZ

        var maxMetalPeak = 0.0
        var deepestCavity = 0.0

        val surfacePoints = mutableListOf<RSurfacePoint>()

        // ۲. نگاشت سطر و ستون با اصلاح سیستم مختصات دکارتی زبان R
        // در R تابع surface3d محور Y را از پایین به بالا ترسیم می‌کند (Inverted Y برای سیستم‌های گرافیکی معمولی)
        for (r in 0 until rowsCount) {
            val rowValues = zMatrix[r]
            for (c in 0 until colsCount) {
                val zValue = if (c < rowValues.size) rowValues[c] else median
                val deltaZ = zValue - median

                if (deltaZ > 0 && deltaZ > maxMetalPeak) maxMetalPeak = deltaZ
                if (deltaZ < 0 && deltaZ < deepestCavity) deepestCavity = deltaZ

                // تعیین رنگ‌های نئونی ژئوفیزیکی بر اساس کالیبراسیون میانه
                val color = when {
                    deltaZ > 0.01 -> "#FF2A55"   // آنومالی مثبت (متالیک نئون قرمز)
                    deltaZ < -0.01 -> "#00E5FF"  // آنومالی منفی (حفره نئون فیروزه‌ای)
                    else -> "#00FF9D"            // زمین نرمال (بستر نئون سبز)
                }

                // دیتای خام برای لاگ و خروجی متنی
                val rawPoint = RPoint3D(c.toDouble(), r.toDouble(), zValue)

                // ۳. فرمول‌های حیاتی اصلاح پیش‌نمایش گرافیکی:
                // معکوس کردن ماتریس روی محور Y برای همگام‌سازی رفتار دکارتی R با ساختار اندروید
                val renderX = c.toFloat()
                val renderY = (rowsCount - 1 - r).toFloat() // معکوس‌سازی جهت جلوگیری از برعکس شدن نقشه
                val renderZ = ((zValue - minZ) / zRange).toFloat() // فشرده‌سازی سه‌بعدی برای جلوگیری از کشیدگی مفرط Z

                surfacePoints.add(
                    RSurfacePoint(rawPoint, deltaZ, color, renderX, renderY, renderZ)
                )
            }
        }

        return generateReport(median, maxMetalPeak, deepestCavity, rowsCount, colsCount, surfacePoints)
    }

    private fun processTripletsAsR(lines: List<String>): RSurfaceReport {
        val triplets = mutableListOf<Triple<Int, Int, Double>>()
        var maxX = 0
        var maxY = 0

        for (line in lines) {
            val tokens = line.split("[,;\\s\\t#|]+".toRegex()).filter { it.isNotEmpty() }
            if (tokens.size >= 3) {
                val x = tokens[0].toIntOrNull() ?: continue
                val y = tokens[1].toIntOrNull() ?: continue
                val z = tokens[2].toDoubleOrNull() ?: continue
                triplets.add(Triple(x, y, z))
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
            }
        }

        if (triplets.isEmpty()) {
            return RSurfaceReport(0.0, 0.0, 0.0, Pair(0, 0), emptyList())
        }

        val rowsCount = maxY + 1
        val colsCount = maxX + 1

        val allZ = triplets.map { it.third }.sorted()
        val median = if (allZ.size % 2 == 0) {
            (allZ[allZ.size / 2 - 1] + allZ[allZ.size / 2]) / 2.0
        } else {
            allZ[allZ.size / 2]
        }

        val minZ = allZ.first()
        val maxZ = allZ.last()
        val zRange = if (maxZ - minZ == 0.0) 1.0 else maxZ - minZ

        var maxMetalPeak = 0.0
        var deepestCavity = 0.0
        val surfacePoints = mutableListOf<RSurfacePoint>()

        for (item in triplets) {
            val c = item.first
            val r = item.second
            val zValue = item.third
            val deltaZ = zValue - median

            if (deltaZ > 0 && deltaZ > maxMetalPeak) maxMetalPeak = deltaZ
            if (deltaZ < 0 && deltaZ < deepestCavity) deepestCavity = deltaZ

            val color = when {
                deltaZ > 0.01 -> "#FF2A55"
                deltaZ < -0.01 -> "#00E5FF"
                else -> "#00FF9D"
            }

            val rawPoint = RPoint3D(c.toDouble(), r.toDouble(), zValue)
            val renderX = c.toFloat()
            val renderY = (rowsCount - 1 - r).toFloat()
            val renderZ = ((zValue - minZ) / zRange).toFloat()

            surfacePoints.add(
                RSurfacePoint(rawPoint, deltaZ, color, renderX, renderY, renderZ)
            )
        }

        return generateReport(median, maxMetalPeak, deepestCavity, rowsCount, colsCount, surfacePoints)
    }

    private fun generateReport(
        median: Double,
        maxMetalPeak: Double,
        deepestCavity: Double,
        rowsCount: Int,
        colsCount: Int,
        surfacePoints: List<RSurfacePoint>
    ): RSurfaceReport {
        val totalDeltaRange = maxMetalPeak + abs(deepestCavity)
        val anomalyRatio = if (median != 0.0) totalDeltaRange / abs(median) else totalDeltaRange

        // تخمین عمق تجربی بر اساس نرخ افت گرادیان مگنتی (1/r^3 dipole gradient decay)
        val estimatedDepth = when {
            maxMetalPeak > 500.0 -> 0.8  // آنومالی سطحی شدید
            maxMetalPeak > 150.0 -> 1.5  // عمق متوسط ۱ الی ۲ متر
            maxMetalPeak > 30.0 -> 2.8   // عمق ۲ الی ۳.۵ متر
            maxMetalPeak > 5.0 -> 4.2    // عمق عمیق > ۴ متر
            else -> 0.0
        }

        val (classFa, classEn) = when {
            maxMetalPeak > 20.0 && abs(deepestCavity) > 20.0 ->
                Pair("ساختار دوگانه: آنومالی فرومغناطیس و حفرۀ زیرسطحی (تارگت ترکیبی)", "Dipolar Composite: Ferromagnetic Anomaly & Cavity Void")
            maxMetalPeak > 15.0 ->
                Pair("آنومالی مثبت فرومغناطیس (فلز/توده با رسانایی یا نفوذپذیری بالا)", "Positive Ferromagnetic Peak (Metal Anomaly / High Susceptibility)")
            abs(deepestCavity) > 15.0 ->
                Pair("آنومالی منفی شدید (فضای خالی / چاه / خاک سست و دست‌خورده)", "Negative Magnetic Dip (Underground Void / Cavity / Disturbed Soil)")
            else ->
                Pair("زمین یکنواخت و بستر همگن طبیعی", "Homogeneous Natural Subsurface (Uniform Ground)")
        }

        val interpFa = buildString {
            append("تحلیل ژئوفیزیکی بر پایه R surface3d:\n")
            append("• ابعاد اسکن: $colsCount ستون در $rowsCount سطر (${colsCount * rowsCount} ایستگاه قرائت)\n")
            append("• خط مبنای زمین (Ground Zero Median): ${String.format("%.2f", median)} µT\n")
            append("• بیشینه پیک متالیک: +${String.format("%.2f", maxMetalPeak)} µT | عمیق‌ترین افت فضا: ${String.format("%.2f", deepestCavity)} µT\n")
            if (estimatedDepth > 0) {
                append("• عمق تخمینی منبع آنومالی: تقریبی ~$estimatedDepth متر با قانون افت گرادیان دو‌قطبی\n")
            }
            append("• نتیجه‌گیری: $classFa")
        }

        val interpEn = buildString {
            append("Geophysical Interpretation (R surface3d Standard):\n")
            append("• Grid Matrix: $colsCount Columns × $rowsCount Rows (${colsCount * rowsCount} survey stations)\n")
            append("• Ground Zero Baseline (Median): ${String.format("%.2f", median)} µT\n")
            append("• Max Metal Peak: +${String.format("%.2f", maxMetalPeak)} µT | Deepest Cavity Dip: ${String.format("%.2f", deepestCavity)} µT\n")
            if (estimatedDepth > 0) {
                append("• Estimated Dipole Target Depth: ~$estimatedDepth meters\n")
            }
            append("• Diagnostic Conclusion: $classEn")
        }

        return RSurfaceReport(
            median = median,
            maxMetalPeak = maxMetalPeak,
            deepestCavity = deepestCavity,
            gridDimensions = Pair(rowsCount, colsCount),
            surfacePoints = surfacePoints,
            theoreticalAnomalyRatio = anomalyRatio,
            estimatedDepthMeters = estimatedDepth,
            targetClassificationFa = classFa,
            targetClassificationEn = classEn,
            interpretationFa = interpFa,
            interpretationEn = interpEn
        )
    }
}
