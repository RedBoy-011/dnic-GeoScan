# 📡 راهنمای جامع اتصال سخت‌افزار و برنامه‌نویسی میکروکنترلر (Dnic-GeoScan Hardware & Firmware Guide)

این مستند فنی راهنمای برنامه‌نویسان فریم‌ورک و طراحان سخت‌افزار برای اتصال دستگاه‌های گرادیومتر، مگنتومتر، فلزیاب پالسی و اسکنرهای ژئوفیزیک به اپلیکیشن اندروید **Dnic-GeoScan** از طریق پروتکل سریال بلوتوث (Bluetooth SPP) است.

---

## ۱. مشخصات پروتکل ارتباطی (Bluetooth SPP Specification)

| پارامتر | مقدار استاندارد | توضیحات |
| :--- | :--- | :--- |
| **پروفایل بلوتوث** | Bluetooth Classic 2.0 / 3.0 / 4.x SPP | Serial Port Profile (RFCOMM) |
| **شناسه سرویس (UUID)** | `00001101-0000-1000-8000-00805F9B34FB` | UUID استاندارد پورت سریال بلوتوث |
| **ماژول‌های پیشنهادی** | HC-05, HC-06, JDY-30/31, SPP-CA | یا چیپ‌های با بلوتوث داخلی نظیر ESP32 |
| **بادریت (Baud Rate)** | `9600` یا `115200` bps | 8 دیتا بیت، بدون توازن (No Parity)، 1 استاپ بیت (8-N-1) |
| **کد اتصال پیش‌فرض (PIN)** | `1234` یا `0000` | در زمان Pair اولیه در سیستم‌عامل اندروید |

---

## ۲. ساختار پکت‌های تله‌متری (Packet Protocol)

اپلیکیشن دنیک مجهز به یک استریم‌پارسر هوشمند ضد نویز (`ScanDataParser.kt`) است که بر اساس نشانگر شروع `$` و نشانگر پایان `#` پکت‌ها را فریم‌بندی و بازسازی می‌کند.

### فرمت کلی پکت:
```text
$<Payload>#\n
```

### حالات معتبر ارسال داده:

#### حالت الف: ارسال تک‌مقداری سنسور (توصیه شده - با گرید خودکار اپلیکیشن)
```text
$Z#\n
```
* **مثال:** `$1045.20#\n`
* **منطق عملکرد:** در این حالت، کاربر در تب `Remote Scan` برنامه، تعداد نقاط هر ردیف را تعیین می‌کند (مثلاً $X = 10$). با رسیدن هر پکت، اپلیکیشن به صورت خودکار مقدار پله‌های $X$ (از ۰ تا ۹) را افزایش داده و پس از پایان ردیف، خودکار یک واحد به شماره خط $Y$ اضافه می‌کند. اگر گزینه «اسکن زیگزاگ» فعال باشد، جهت مسیر بازگشت نیز به طور هوشمند اصلاح می‌شود.

#### حالت ب: ارسال کامل سه‌تایی مختصات و سنسور ($X, Y, Z$)
```text
$X,Y,Z#\n
```
* **مثال:** `$0,0,1045.50#\n` یا `$4,2,1180.25#\n`
* **منطق عملکرد:** اگر سخت‌افزار شما خودش مجهز به سنسور قدم‌شمار یا انکودر چرخ است و مختصات دقیق شبکه را مدیریت می‌کند، می‌توانید مختصات سطر، ستون و عدد سنسور را مستقیماً به اپلیکیشن تحمیل نمایید.

#### حالت ج: ارسال دوتایی ($X, Z$)
```text
$X,Z#\n
```
* **مثال:** `$3,1050.80#\n`

---

## ۳. کدهای نمونه آماده برای میکروکنترلرها

### نمونه ۱: کد آردوینو (Arduino Uno/Nano/Mega) با ماژول HC-05

```cpp
/**
 * Dnic-GeoScan Arduino Firmware Example
 * Hardware: Arduino Uno/Nano + HC-05 Bluetooth Module + Button
 */

#include <SoftwareSerial.h>

// پایه‌های سریال مجازی آردوینو به ماژول بلوتوث
// Arduino RX (Pin 2) -> HC-05 TX
// Arduino TX (Pin 3) -> تقسیم مقاومتی (1k + 2k) -> HC-05 RX
SoftwareSerial BTSerial(2, 3); 

const int STEP_BUTTON_PIN = 4; // کلید ارسال نقطه هنگام قدم زدن روی شبکه
int lastButtonState = HIGH;

void setup() {
  Serial.begin(115200);   // دیباگ از طریق پورت USB کامپیوتر
  BTSerial.begin(9600);    // باودریت استاندارد کارخانه‌ای HC-05
  
  pinMode(STEP_BUTTON_PIN, INPUT_PULLUP);
  Serial.println("=== Dnic-GeoScan Transmitter Initialized ===");
}

/**
 * ارسال یک نقطه سنسور به فرمت استاندارد دنیک: $Z#\n
 */
void sendPointToApp(float sensorReading) {
  BTSerial.print("$");
  BTSerial.print(sensorReading, 2); // ارسال عدد اعشاری با ۲ رقم دقت
  BTSerial.println("#");           // ارسال کاراکتر پایان (#) و پایان خط (\r\n)
  
  Serial.print("Dispatched: $");
  Serial.print(sensorReading, 2);
  Serial.println("#");
}

void loop() {
  int buttonState = digitalRead(STEP_BUTTON_PIN);
  
  // لبه پایین‌رونده: با فشردن کلید، یک نقطه ثبت و ارسال می‌شود
  if (buttonState == LOW && lastButtonState == HIGH) {
    float zValue = readMagnetometerSensor();
    sendPointToApp(zValue);
    delay(200); // تاخیر De-bounce کلید
  }
  
  lastButtonState = buttonState;
}

// تابع خواندن نمونه سنسور مگنتومتر (یا شبیه‌ساز)
float readMagnetometerSensor() {
  // دیتای واقعی سنسور بر حسب نانوتسلا یا گرادیان
  return 1024.0 + (random(-40, 40) / 10.0);
}
```

---

### نمونه ۲: کد ESP32 با بلوتوث داخلی (بدون نیاز به ماژول خارجی)

```cpp
/**
 * Dnic-GeoScan ESP32 Firmware Example
 * Uses Built-in Classic Bluetooth Serial (No External Hardware Needed)
 */

#include "BluetoothSerial.h"

BluetoothSerial SerialBT;

const int TRIGGER_PIN = 0; // استفاده از کلید پیش‌فرض BOOT روی برد ESP32
int lastTriggerState = HIGH;

void setup() {
  Serial.begin(115200);
  
  // آغاز به کار بلوتوث با نام قابل جستجو در اندروید
  SerialBT.begin("DNIC-GEOSCAN-DEVICE");
  Serial.println("Bluetooth Broadcast Started: Search for 'DNIC-GEOSCAN-DEVICE'");
  
  pinMode(TRIGGER_PIN, INPUT_PULLUP);
}

void sendPoint(float zValue) {
  if (SerialBT.hasClient()) {
    // ارسال پکت استاندارد دنیک
    SerialBT.printf("$%.2f#\n", zValue);
    Serial.printf("Sent to Phone: $%.2f#\n", zValue);
  } else {
    Serial.println("Warning: Phone is not connected yet!");
  }
}

void loop() {
  int triggerState = digitalRead(TRIGGER_PIN);
  
  if (triggerState == LOW && lastTriggerState == HIGH) {
    float sensorZ = 1045.75; // جایگزین با تابع خواندن ADC یا I2C سنسور
    sendPoint(sensorZ);
    delay(250);
  }
  
  lastTriggerState = triggerState;
}
```

---

### نمونه ۳: تخلیه کامل یک اسکن ذخیره‌شده در حافظه دستگاه (Batch Dump)

اگر دستگاه شما دارای حافظه SD Card، فلش یا EEPROM است و می‌خواهید پس از اتمام اسکن میدانی کل ماتریس را یک‌جا با فشردن کلید ارسال به اپلیکیشن بفرستید:

```cpp
void dumpStoredScanToApp(int gridX, int gridY) {
  Serial.println("Starting batch dump to Dnic-GeoScan...");
  
  for (int y = 0; y < gridY; y++) {
    for (int x = 0; x < gridX; x++) {
      float storedZ = readFromMemory(x, y);
      
      // ارسال پکت تک مقداری یا مختصات کامل
      SerialBT.printf("$%.2f#\n", storedZ);
      
      // تاخیر ضروری بین هر پکت جهت جلوگیری از سرریز پشته بلوتوث اندروید
      delay(30); 
    }
  }
  
  Serial.println("Batch dump completed.");
}
```

---

## ۴. الزامات و نکات طلایی برای پایداری ۱۰۰٪

1. **تاخیر بین پکت‌ها (Inter-packet Delay):**
   - در حالت ارسال نقطه به نقطه (با قدم زدن اپراتور)، ارسال در هر `100ms` الی `500ms` کاملاً ایده‌آل است.
   - در حالت تخلیه یک‌باره کل فایل (Batch Dump)، حتماً حداقل **`25ms` الی `50ms`** بین ارسال هر پکت تاخیر ایجاد کنید تا پشته RFCOMM سیستم‌عامل اندروید داده‌ها را بدون افت فریم تحویل ویجت‌های Compose و ECharts دهد.
2. **پایان خط (`\n`):** بعد از کاراکتر `#` حتماً `\n` یا `\r\n` بفرستید.
3. **فیلتر نویز خودکار:** در صورتی که سیم‌کشی سریال دچار لرزش باشد یا بایت‌های تصادفی تولید شود، پارسر اپلیکیشن هر بایتی قبل از `$` را خودکار حذف کرده و تا زمان دریافت `#` بافر را کنترل می‌کند.
4. **تراز رنگ‌ها در اپلیکیشن بر اساس مقدار Z:**
   - مقادیر نزدیک به میانه داده‌ها: خط مبنای زمین (**سبز نئون `#00FF9D`**)
   - مقادیر با جهش مثبت (فلزات / ناهنجاری مثبت): قله مغناطیسی (**قرمز نئون `#FF2A55`**)
   - مقادیر با افت شدید منفی (حفرات، تونل‌ها و خاک سست): دره مغناطیسی (**آبی فیروزه‌ای `#00E5FF`**)
