# Proguard rules for dnic GeoScan
# Keep WebView JavascriptInterface methods when adding ECharts 3D in upcoming phases
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
