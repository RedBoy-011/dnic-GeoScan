# نرم‌افزار دنیک ژئواسکن | Dnic-GeoScan (Dnic Visualizer 3D)

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%202.0-purple.svg)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-blue.svg)](https://developer.android.com/jetpack/compose)
[![Visualizer](https://img.shields.io/badge/3D%20Engine-Apache%20ECharts%203D-red.svg)](https://echarts.apache.org)
[![Build Status](https://img.shields.io/badge/GitHub%20Actions-Passing-brightgreen.svg)](https://github.com/RedBoy-011/dnic-GeoScan/actions)
[![Release](https://img.shields.io/badge/Release-v2.0.1-gold.svg)](https://github.com/RedBoy-011/dnic-GeoScan/releases/tag/v2.0.1)

> **Dnic-GeoScan** یک اپلیکیشن حرفه‌ای، مدرن و صنعتی اندروید برای ثبت داده‌ها (Data Logger) و تصویرسازی سه‌بعدی داده‌های مغناطیس‌سنج‌ها و اسکنرهای ژئوفیزیک (Magnetometer / Gradiometer) است. این نرم‌افزار با معماری رسپانسیو تطبیقی برای گوشی‌های هوشمند و تبلت‌های صنعتی، پشتیبانی کامل دو زبانه (فارسی و انگلیسی)، جهت‌بندی پویا (RTL/LTR)، هاب منوی اصلی سه‌گزینه‌ای (Local Analysis, Remote Scan, Settings)، سیستم لاگر پیشرفته وقایع (`denic_debug_log.txt`)، موتور هوشمند پیمایش ماتریس (X->Y Auto-Increment)، انطباق کامل با سیستم مختصات دکارتی تابع `surface3d` در زبان R، باکس جامع تحلیل ژئوفیزیک و برآورد عمق، بازخورد صوتی تله‌متری بلادرنگ (Acoustic Telemetry) و پشتیبانی از فایل‌های `.v3d`، `.csv` و `.txt`، جایگزین کامل و پیشرفته‌ای برای نرم‌افزارهای قدیمی تحت ویندوز است.

---

## 📥 لینک‌های دانلود آخرین نسخه تایید شده (Download Links)

برای دریافت آخرین نسخه رسمی، رسپانسیو و تایید شده برنامه (**Version 2.0.1**) می‌توانید از لینک‌های مستقیم زیر استفاده کنید:

* **[🚀 دانلود مستقیم فایل نصبی رسمی Dnic-GeoScan-v2.0.1.apk](https://github.com/RedBoy-011/dnic-GeoScan/releases/download/v2.0.1/Dnic-GeoScan-v2.0.1.apk)**
* **[🏷️ صفحه رسمی انتشار نسخه جدید در گیت‌هاب (GitHub Releases v2.0.1)](https://github.com/RedBoy-011/dnic-GeoScan/releases/tag/v2.0.1)**
* **[⛏️ دانلود دمو معدن چندعمقی CSV (demo_mine_multidepth.csv)](https://github.com/RedBoy-011/dnic-GeoScan/releases/download/v2.0.1/demo_mine_multidepth.csv)**
* **[💎 دانلود دمو معدن چندعمقی V3D (demo_mine_multidepth.v3d)](https://github.com/RedBoy-011/dnic-GeoScan/releases/download/v2.0.1/demo_mine_multidepth.v3d)**
* **[📊 دانلود دمو گرادیان ۱۰×۱۰ CSV (demo_10x10_anomaly.csv)](https://github.com/RedBoy-011/dnic-GeoScan/releases/download/v2.0.1/demo_10x10_anomaly.csv)**
* **[🌐 دانلود دمو گرادیان ۱۰×۱۰ V3D (demo_10x10_anomaly.v3d)](https://github.com/RedBoy-011/dnic-GeoScan/releases/download/v2.0.1/demo_10x10_anomaly.v3d)**
* **[📦 دانلود از آرتیفکت‌های پایپ‌لاین تایید شده GitHub Actions](https://github.com/RedBoy-011/dnic-GeoScan/actions)**
* **مسیر فایل محلی (Local APK Path):**
  ```text
  release/Dnic-GeoScan-v2.0.1.apk
  app/build/outputs/apk/release/app-release.apk
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
   - فایل `Dnic-GeoScan-v1.5.1.apk` را از لینک بالای صفحه دانلود کرده و به حافظه دستگاه منتقل کنید.
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

## 🔌 راهنمای جامع اتصال سخت‌افزار و برنامه‌نویسان دستگاه (Hardware & Firmware Developer Guide)

> 📘 برای مطالعه داکیومنت کامل فنی به همراه جزییات پایه به پایه‌ها، به فایل [docs/HARDWARE_FIRMWARE_GUIDE.md](docs/HARDWARE_FIRMWARE_GUIDE.md) مراجعه فرمایید.

دستگاه‌های سنسور مگنتومتر، گرادیومتر یا فلزیاب می‌توانند از طریق پروتکل سریال بلوتوث (**Bluetooth Classic SPP**) و با استفاده از ماژول‌های **HC-05** یا چیپ‌های **ESP32** داده‌های اسکن را به اپلیکیشن مخابره کنند.

### ۱. مشخصات پروتکل سریال (Bluetooth SPP Specs)
* **پروفایل بلوتوث:** Bluetooth Classic Serial Port Profile (RFCOMM)
* **شناسه سرویس (UUID):** `00001101-0000-1000-8000-00805F9B34FB`
* **بادریت پیشنهادی (Baud Rate):** `9600` یا `115200` bps (تنظیمات 8-N-1)
* **پین جفت‌سازی پیش‌فرض (PIN):** `1234` یا `0000`

### ۲. ساختار پکت‌های ارسالی (Packet Protocol)
فرمت استاندارد پکت‌های دیتای سنسور با شروع `$` و پایان `#\n` ارسال می‌شود:
$$\$<Payload>\#\backslash n$$

| حالت ارسال | فرمت پکت | مثال | نحوه پردازش در اپلیکیشن |
| :--- | :--- | :--- | :--- |
| **تک‌مقداری (توصیه شده)** | `$<Z>#\n` | `$1045.2#\n` | اپلیکیشن با گرید خودکار، $X$ را یکی‌یکی اضافه کرده و در انتهای ردیف، خودکار $Y$ را شیفت می‌دهد. |
| **مختصات کامل** | `$<X>,<Y>,<Z>#\n` | `$0,0,1045.5#\n` | سخت‌افزار صراحتاً مختصات سطر، ستون و مقدار را تعیین می‌کند. |
| **جفت $X, Z$** | `$<X>,<Z>#\n` | `$2,1050.0#\n` | مقدار $X$ و سنسور $Z$ ارسال می‌شود. |

### ۳. کدهای نمونه آماده برای میکروکنترلرها

<details>
<summary><b>🔹 نمونه کد آردوینو (Arduino Uno / Nano + HC-05)</b></summary>

```cpp
#include <SoftwareSerial.h>

// RX آردوینو به TX ماژول بلوتوث (پایه ۲)
// TX آردوینو به RX ماژول بلوتوث با تقسیم مقاومتی (پایه ۳)
SoftwareSerial BTSerial(2, 3); 
const int BUTTON_PIN = 4; // کلید ثبت گام
int lastState = HIGH;

void setup() {
  Serial.begin(115200);
  BTSerial.begin(9600); // بادریت HC-05
  pinMode(BUTTON_PIN, INPUT_PULLUP);
}

void sendPoint(float zValue) {
  BTSerial.print("$");
  BTSerial.print(zValue, 2);
  BTSerial.println("#"); // ارسال # و خط جدید
}

void loop() {
  int currentState = digitalRead(BUTTON_PIN);
  if (currentState == LOW && lastState == HIGH) {
    float reading = 1024.50; // مقدار سنسور گرادیومتر
    sendPoint(reading);
    delay(200);
  }
  lastState = currentState;
}
```
</details>

<details>
<summary><b>🔹 نمونه کد ESP32 با بلوتوث داخلی سریال</b></summary>

```cpp
#include "BluetoothSerial.h"
BluetoothSerial SerialBT;
const int TRIGGER_PIN = 0; // کلید BOOT
int lastState = HIGH;

void setup() {
  Serial.begin(115200);
  SerialBT.begin("DNIC-GEOSCAN-DEVICE"); // نام بلوتوث در جستجوی گوشی
  pinMode(TRIGGER_PIN, INPUT_PULLUP);
}

void loop() {
  int currentState = digitalRead(TRIGGER_PIN);
  if (currentState == LOW && lastState == HIGH) {
    if (SerialBT.hasClient()) {
      float z = 1052.80; // سنسور
      SerialBT.printf("$%.2f#\n", z); // ارسال پکت استاندارد دنیک
    }
    delay(200);
  }
  lastState = currentState;
}
```
</details>

<details>
<summary><b>🔹 تخلیه یک‌جای اسکن ذخیره‌شده از حافظه (Batch Scan Dump)</b></summary>

```cpp
void dumpEntireScan(int totalX, int totalY) {
  for (int y = 0; y < totalY; y++) {
    for (int x = 0; x < totalX; x++) {
      float z = readStoredMemory(x, y);
      SerialBT.printf("$%.2f#\n", z);
      delay(30); // تاخیر حداقل ۳۰ میلی‌ثانیه بین پکت‌ها الزامی است
    }
  }
}
```
</details>

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
│   │   │       │   ├── hub/                     # داشبورد هاب منوی اصلی (۳ گزینه اصلی)
│   │   │       │   ├── remote/                  # صفحه اسکن زنده با بلوتوث SPP
│   │   │       │   ├── local/                   # صفحه آنالیز فایل‌های محلی
│   │   │       │   └── settings/                # صفحه تنظیمات، تغییر زبان و لاگر
│   │   │       ├── theme/                       # پالت رنگ نئونی، تایپوگرافی و تم تاریک
│   │   │       ├── util/
│   │   │       │   └── GeoLogger.kt             # سیستم ثبت لاگ پایدار در فیزیکال فایل
│   │   │       └── visualizer/
│   │   │           ├── EChartsVisualizer.kt     # کنترلر WebView و پل ارتباطی WebGL/Canvas
│   │   │           └── VisualizerViewMode.kt    # وضعیت‌های نمایش سه‌بعدی و دوبعدی
│   │   └── res/
│   │       ├── drawable/                        # وکتور آیکون اختصاصی دنیک (Mesh + PCB)
│   │       ├── values/                          # متن‌های انگلیسی و استایل‌ها
│   │       └── values-fa/                       # متن‌های بومی فارسی (i18n)
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

- [x] **تکمیل فاز ۱ (v2.0.0):** رفع اساسی باگ صفحه ۳بعدی WebGL، موتور دوگانه Canvas Fallback، شتاب سخت‌افزاری و همگام‌سازی چرخه حیات Compose.
- [x] **تکمیل فاز ۲ (v2.0.0):** پیاده‌سازی سیستم لاگ پایدار فایل‌محور (`GeoLogger`) در فایل `denic_debug_log.txt` همراه با اشتراک‌گذاری و ذخیره در پوشه دانلودها.
- [x] **تکمیل فاز ۳ (v2.0.0):** معماری ناوبری هاب سه‌گزینه‌ای (Local Analysis, Remote Scan, Settings) و بومی‌سازی کامل دوزبانه (فارسی و انگلیسی) با RTL.
- [x] **تکمیل فاز ۴ (v2.0.0):** نوسازی رابط کاربری Glassmorphism ۲۰۲۶ با طیف رنگ نئونی مغناطیسی و حالات صوتی تله‌متری بلادرنگ.
- [x] **تایید خودکار بیلد (Self-Healing Build):** پایپ‌لاین یکپارچه‌سازی مداوم GitHub Actions، اجرای تست‌های واحد و تست خودکار نصب و اجرای APK روی ایمولاتور اندروید ۱۲ و تبلت.
