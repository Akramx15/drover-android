# Drover Quest

تطبيق مرافق محلي لـMeta Quest 3 يشغّل تعديل Drover مع تطبيق Discord الرسمي
على النظارة نفسها. لا يحتاج PC أو DNS host أو سيرفر خارجي.

مشروع غير رسمي، وغير تابع أو معتمد من Discord أو Meta.

## ماذا يفعل؟

- ينشئ Android `VpnService` محليًا ومحصورًا في حزمة Discord فقط.
- يمرر TCP وUDP مباشرة إلى الإنترنت؛ ليس VPN جغرافيًا ولا يفك تشفير Discord.
- عند أول إرسال من سوكت UDP، إذا كانت الحزمة 74 بايت، يرسل `00` ثم `01`،
  ينتظر 50ms، ثم يرسل الحزمة الأصلية. هذا يطابق Direct mode في
  [discord-drover](https://github.com/hdrover/discord-drover).
- لا يجمع بيانات، ولا يرسل telemetry، ولا يقرأ الحساب أو الرسائل أو الصوت.

## التثبيت على Quest 3

الـAPK الحالي موقّع بمفتاح Android debug ومخصص للتجربة الشخصية على ARM64.
فعّل Developer Mode وUSB debugging، ثم من الكمبيوتر:

```powershell
adb install -r Drover-Quest-v0.1.0-alpha-debug-arm64-v8a.apk
```

بعد التثبيت ستجد التطبيق ضمن مكتبة التطبيقات المثبتة من مصادر خارجية.

## الاستخدام

1. افتح **Drover Quest**.
2. اضغط **تشغيل دروفر** ووافق على طلب Android VPN في أول مرة.
3. افتح تطبيق Discord الرسمي وادخل الروم الصوتي.
4. اترك Drover يعمل أثناء المكالمة. اضغط **إيقاف دروفر** عند الانتهاء.

Android يسمح بخدمة VPN واحدة في الوقت نفسه؛ تشغيل VPN آخر سيوقف Drover.
التطبيق لا يعمل تلقائيًا بعد إعادة تشغيل النظارة.

## بناء تطبيق Android

المتطلبات: JDK 17 أو 21، Android SDK Platform 34، ونسخة ARM64 من المحرك
موجودة في `app/src/main/jniLibs/arm64-v8a/libtun2proxy.so`.

```powershell
$env:ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat clean lintDebug assembleDebug
```

الناتج:
`app/build/outputs/apk/debug/app-debug.apk`

تفاصيل إعادة بناء محرك Rust موجودة في
[`native/README.md`](native/README.md). المحرك مبني من tun2proxy 0.8.3 عند
commit `fc77ca3182b3a63b84266bb0a5d24c096e022765` مع patch قابل لإعادة الإنتاج.
النسخة المضمّنة مبنية بوضع Direct فقط (`--no-default-features`) ولا تحتوي
محركات SOCKS/HTTP الاختيارية.

## حدود النسخة 0.1.0

- هذه نسخة تجريبية debug وليست إصدار متجر.
- تطبق تسلسل Drover الأساسي فقط؛ ملف `drover-packet.bin` الاختياري غير مدعوم.
- تم اختبار إنشاء النفق، DNS، TCP/UDP، وحصره في Discord على Quest 3 / Android
  14. يبقى اختبار الدخول لمكالمة صوتية على شبكتك هو اختبار القبول النهائي.

## أصحاب المشروع والمساهمون

1. [Akramx15](https://github.com/Akramx15) — صاحب المشروع والفكرة والتوجيه.
2. OpenAI Codex — المساعدة في التصميم والتنفيذ والمراجعة التقنية.

المشروع مرخص بـMIT. راجع `LICENSE` و`THIRD_PARTY_NOTICES.md` و
`DEPENDENCY_LICENSES.md`.
