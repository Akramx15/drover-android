# JNI resolves this stable upstream class name at runtime. It intentionally
# remains unchanged even though the application package is app.drover.android.
-keep class com.github.shadowsocks.bg.Tun2proxy { *; }
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
