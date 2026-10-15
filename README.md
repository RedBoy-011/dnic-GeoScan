# نرم‌افزار دنیک ژئواسکن | Dnic-GeoScan (Dnic Visualizer 3D)

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%202.0-purple.svg)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-blue.svg)](https://developer.android.com/jetpack/compose)
[![Visualizer](https://img.shields.io/badge/3D%20Engine-Apache%20ECharts%203D-red.svg)](https://echarts.apache.org)
[![Build Status](https://img.shields.io/badge/GitHub%20Actions-Passing-brightgreen.svg)](https://github.com/RedBoy-011/dnic-GeoScan/actions)
[![Release](https://img.shields.io/badge/Release-v1.1.8-gold.svg)](https://github.com/RedBoy-011/dnic-GeoScan/releases/tag/v1.1.8)

> **Dnic-GeoScan** یک اپلیکیشن حرفه‌ای، مدرن و صنعتی اندروید برای ثبت داده‌ها (Data Logger) و تصویرسازی سه‌بعدی داده‌های مغناطیس‌سنج‌ها و اسکنرهای ژئوفیزیک (Magnetometer / Gradiometer) است. این نرم‌افزار جایگزین کامل و پیشرفته‌ای برای نرم‌افزارهای قدیمی تحت ویندوز بوده و به صورت زنده یا آفلاین، داده‌های سطحی و زیرسطحی را اسکن و آنالیز می‌کند.

---

## 📥 لینک‌های دانلود آخرین نسخه تایید شده (Download Links)

برای دریافت آخرین نسخه رسمی و تایید شده برنامه (**Version 1.1.8**) می‌توانید از لینک‌های مستقیم زیر استفاده کنید:

* **[🚀 دانلود مستقیم فایل نصبی رسمی Dnic-GeoScan-v1.1.8.apk](https://github.com/RedBoy-011/dnic-GeoScan/releases/download/v1.1.8/Dnic-GeoScan-v1.1.8.apk)**
* **[🏷️ صفحه رسمی انتشار نسخه جدید در گیت‌هاب (GitHub Releases v1.1.8)](https://github.com/RedBoy-011/dnic-GeoScan/releases/tag/v1.1.8)**
* **[📦 دانلود از آرتیفکت‌های پایپ‌لاین تایید شده GitHub Actions](https://github.com/RedBoy-011/dnic-GeoScan/actions)**
* **مسیر فایل محلی (Local APK Path):**
  ```text
  release/Dnic-GeoScan-v1.1.8.apk
  app/build/outputs/apk/debug/app-debug.apk
  ```

---

## 🧭 هویت برند و مفهوم طراحی (Brand & Visual Identity)

* **نام برند:** **denic** مخفف *Digital Electronic* (الکترونیک دیجیتال).
* **سبک بصری:** طراحی مینیمال سوییسی (Swiss Minimalist) ترکیب‌شده با جلوه‌های شیشه‌ای مدرن (Glassmorphism) بر روی پس‌زمینه عمیق تیره فضا (`#070A12`)، بهینه‌سازی‌شده برای کار در محیط‌های باز و نور شدید روز در تبلت‌های میدانی.
* **پالت رنگی طیف مغناطیسی (Magnetic Spectrum Neon):**
  * 🟢 **سبز نئونی (`#00FF9D` - Nominal):** خط مبنای میدان مغناطیسی خاک و نقطه تعادل زمین (Ground Zero).
  * 🔵 **آبی فیروزه‌ای نئونی (`#00E5FF` - Cavity):** ناهنجاری‌های منفی، حفرات، دالان‌ها، چاه‌ها و افت شار مغناطیسی.
  * 🔴 **قرمز نئونی (`#FF2A55` - Metal):** ناهنجاری‌های مثبت، اهداف فرومغناطیسی، فلزات باارزش و بیشینه شار مغناطیسی.
* **مفهوم نشان‌واره (Logo Concept):** ترکیب مینیمال یک شبکه سه‌بعدی ژئوفیزیکی موج‌دار با خطوط مدار چاپی (PCB) الکترونیک دیجیتال و پدهای تماسی سنسور.

---

## 🚀 خلاصه امکانات و معماری سیستم (System Features)

### ۱. اتصال پایدار بلوتوث با سخت‌افزار (Remote Scan Mode)
* ارتباط مستقیم از طریق **Bluetooth Classic (SPP - Serial Port Profile)** با ماژول‌های سخت‌افزاری نظیر **HC-05** با شناسه استاندارد UUID: `00001101-0000-1000-8000-00805F9B34FB`.
* مدیریت استریم بلادرنگ با استفاده از کاتلین **Coroutines** و **SharedFlow** بدون هیچ‌گونه افت فریم یا هدررفت پکت.
* پارسر قدرتمند داده با قابلیت پاک‌سازی نویز و بازسازی بسته‌ها بر اساس قالب پروتکل `$X,Y,Z#\n` (مانند `$1,2,1045#\n`).

### ۲. آنالیز آفلاین فایل‌ها (Local Analysis Mode)
* یکپارچگی با فریم‌ورک استاندارد دسترسی به حافظه اندروید (**Storage Access Framework - SAF**) جهت خواندن فایل‌های اسکن با فرمت‌های `CSV` و `TXT`.
* پشتیبانی هوشمند از انواع فرمت‌های داده (داده‌های سه‌تایی خطی `X,Y,Z` و ماتریس‌های جدولی سطر-ستونی).
* **نرمال‌سازی میانه (Median Ground Zero Normalization):** محاسبه خودکار میانه کل نقاط اسکن شده به عنوان خط مبنای زمین جهت تفکیک دقیق فلزات و فضاهای خالی.

### ۳. موتور پردازش و تصویرسازی سه‌بعدی ECharts 3D
* استفاده از کتابخانه‌های آفلاین و فوق‌سریع **Apache ECharts 5.5.0** و **ECharts-GL 2.0.9** در محیط WebView با شتاب‌دهنده سخت‌افزاری (`LAYER_TYPE_HARDWARE`).
* چرخش ۳۶۰ درجه آزاد، زوم چندانگشتی، زاویه‌دید سه‌بعدی و نمایش بلادرنگ توپوگرافی سنسورها.
* امکان سوییچ آنی میان نمای پلات سه‌بعدی مسطح (**3D Surface Plot**) و نمای حرارتی دوبعدی (**2D Heatmap View**).

### ۴. فیلتر هموارسازی ریاضیاتی دو مکعبی (Bicubic Interpolation)
* اعمال فیلتر هوشمند درون‌یابی دو مکعبی با اسپلاین Catmull-Rom ($a = -0.5$) بر روی نقاط گسسته اسکن.
* تبدیل ماتریس‌های مشبک فاصله‌دار به رویه‌های نرم، پیوسته و باکیفیت بدون اعوجاج داده‌های اصلی.

### ۵. ذخیره‌سازی و برون‌بری نشست‌های اسکن (Session Export)
* قابلیت ذخیره‌سازی داده‌های اسکن زنده به صورت فایل استاندارد CSV در پوشه `Documents/denic-GeoScan` دستگاه جهت بایگانی، چاپ یا ایمپورت در نرم‌افزارهای دسکتاپ (مانند Voxler یا Surfer).

---

## 📱 راهنمای راه‌اندازی و نصب اولیه (Quick Start Guide)

برای اجرای برنامه بر روی تبلت یا گوشی‌های اندرویدی مراحل زیر را دنبال فرمایید:

1. **دانلود فایل APK:**
   - فایل `denic-GeoScan-v1.0.0.apk` را از لینک بالای صفحه دانلود کرده و به حافظه دستگاه منتقل کنید.
2. **مجوز نصب (Install from Unknown Sources):**
   - در صورت نیاز در تنظیمات اندروید، اجازه نصب برنامه از مرورگر یا مدیریت فایل را تایید نمایید (`Allow from this source`).
3. **اعطای مجوزها (Permissions):**
   - پس از اولین اجرای برنامه، مجوزهای موقعیت مکانی و بلوتوث دستگاه‌های مجاور (`Nearby Devices / Bluetooth Scan & Connect`) را تایید کنید.
4. **شروع اسکن زنده (Remote Scan):**
   - ماژول بلوتوث سخت‌افزار (مانند HC-05) را از تنظیمات بلوتوث اندروید جفت (Pair) کنید (کد پیش‌فرض معمولاً ۱۲۳۴ یا ۰۰۰۰ است).
   - وارد تب **Remote Scan** شده، دستگاه را انتخاب و روی دکمه **Connect & Scan** کلیک نمایید.
5. **تحلیل فایل آفلاین (Local Analysis):**
   - به تب **Local Analysis** رفته و دکمه **Open File** را لمس کرده و فایل `.csv` یا `.txt` مورد نظر را انتخاب نمایید تا آنالیز سه‌بعدی فوری نمایش داده شود.

---

## 🏗 ساختار مهندسی پوشه‌ها و کدهای پروژه

```
denic-GeoScan/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml          # تنظیمات دسترسی‌های بلوتوث SPP و حافظه SAF
│   │   ├── assets/
│   │   │   └── echarts/
│   │   │       ├── echarts.min.js       # کتابخانه آفلاین آپاچی ای‌چارتز
│   │   │       ├── echarts-gl.min.js    # افزونه گرافیک سه‌بعدی وب‌جی‌ال
│   │   │       └── index.html           # هسته رندرر گراف و پل ارتباطی جاوااسکریپت
│   │   ├── java/com/denic/geoscan/
│   │   │   ├── MainActivity.kt          # اکتیویتی اصلی Compose Edge-to-Edge
│   │   │   ├── data/
│   │   │   │   ├── analysis/
│   │   │   │   │   ├── BicubicInterpolator.kt   # موتور ریاضیاتی هموارسازی سطوح
│   │   │   │   │   └── DatasetNormalizer.kt     # ماژول نرمال‌سازی میانه مبنای زمین
│   │   │   │   ├── bluetooth/
│   │   │   │   │   ├── BluetoothConnectionState.kt
│   │   │   │   │   ├── BluetoothDeviceModel.kt
│   │   │   │   │   └── BluetoothService.kt      # سرویس مدیریت سوکت SPP با SharedFlow
│   │   │   │   ├── model/
│   │   │   │   │   ├── ScanPoint.kt
│   │   │   │   │   └── NormalizedScanPoint.kt
│   │   │   │   ├── parser/
│   │   │   │   │   └── ScanDataParser.kt        # پردازشگر پکت‌های استاندارد $X,Y,Z#
│   │   │   │   └── storage/
│   │   │   │       ├── LocalFileParser.kt       # پارسر فایل‌های CSV و متنی
│   │   │   │       └── SessionExporter.kt       # خروجی‌گیر استاندارد سشن‌ها
│   │   │   └── ui/
│   │   │       ├── components/
│   │   │       │   └── GlassmorphicCard.kt      # المان‌های شیشه‌ای بلور و مدرن
│   │   │       ├── navigation/
│   │   │       │   ├── Screen.kt
│   │   │       │   └── AppNavigation.kt
│   │   │       ├── screens/
│   │   │       │   ├── remote/                  # صفحه اسکن زنده با بلوتوث
│   │   │       │   └── local/                   # صفحه آنالیز فایل‌های ذخیره شده
│   │   │       ├── theme/                       # پالت رنگ نئونی، تایپوگرافی و تم تاریک
│   │   │       └── visualizer/
│   │   │           ├── EChartsVisualizer.kt     # کنترلر WebView و پل ارتباطی ECharts
│   │   │           └── VisualizerViewMode.kt    # وضعیت‌های نمایش سه‌بعدی و دوبعدی
│   │   └── res/
│   │       ├── drawable/                        # وکتور آیکون اختصاصی دنیک (Mesh + PCB)
│   │       └── values/                          # مقادیر نام‌ها، تم‌ها و استایل‌ها
│   └── build.gradle.kts
├── .github/
│   └── workflows/
│       └── build-apk.yml                        # پایپ‌لاین بیلد خودکار CI/CD گیت‌هاب اکشنز
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── gradlew
└── settings.gradle.kts
```

---

## 🛡 وضعیت تایید نسخه‌ها (Verification Status)

- [x] **تکمیل فاز ۱:** پیاده‌سازی معماری Jetpack Compose، تم تاریک Glassmorphism و ناوبری صفحات.
- [x] **تکمیل فاز ۲:** پیاده‌سازی درایور سوکت بلوتوث SPP و پارسر خطاناپذیر `$X,Y,Z#\n`.
- [x] **تکمیل فاز ۳:** پیاده‌سازی لایه انتخاب فایل محلی (SAF) و نرمال‌سازی بر پایه میانه مبنای زمین.
- [x] **تکمیل فاز ۴:** پیاده‌سازی رندرر سه‌بعدی Apache ECharts-GL با شتاب‌دهنده گرافیکی و نگاشت رنگی.
- [x] **تکمیل فاز ۵:** پیاده‌سازی الگوریتم درون‌یابی دوبعدی-دومکعبی و صدور گزارش‌های CSV.
- [x] **تکمیل ریفکتور جامع:** تغییر کامل نام برند به **denic** و شناسه پکیج به `com.denic.geoscan`.
- [x] **تایید خودکار بیلد (Self-Healing Build):** پیکربندی کامل اسکریپت رپر گریدل و گردش کار GitHub Actions جهت تولید فایل نصبی رسمی.
