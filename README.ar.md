# Drover for Android

[Read this guide in English](README.md)

تطبيق **Drover for Android** مرافق يعمل بالكامل على جهاز Android مع تطبيق
Discord الرسمي. يطبق مصافحة Drover لحزم UDP محليًا عن طريق `VpnService`؛ لا
يحتاج كمبيوترًا أو DNS host أو خادم VPN خارجيًا أو proxy.

هذا مشروع مجتمعي غير رسمي، ولا يتبع Discord أو Meta ولا يحظى باعتماد منهما.

## التنزيل

تُوزع النسخة 0.2.0 من صفحة GitHub Releases لهذا المستودع فقط. لا توجد نسخة
على Google Play ولا ملف AAB.

| الملف | متى تختاره؟ |
| --- | --- |
| `Drover-for-Android-v0.2.0-universal.apk` | إذا لم تعرف معمارية جهازك. هذا هو الخيار الموصى به ويدعم ARM64 وARMv7 وx86_64. |
| `Drover-for-Android-v0.2.0-arm64-v8a.apk` | إذا كنت تستخدم Meta Quest 3 أو جهاز Android حديثًا بمعمارية ARM64 وتريد تنزيلًا أصغر. |

يتطلب الملفان Android 10 أو أحدث. قبل التثبيت، طابق SHA-256 للملف وبصمة
SHA-256 لشهادة التوقيع مع القيم الموجودة في وصف إصدار GitHub.
يحمّل أندرويد مكتبة Native واحدة توافق معمارية الجهاز من ملف Universal، ولا
يحمّل المعماريات الثلاث في الذاكرة. راجع
[توثيق أندرويد لحزم ABI](https://developer.android.com/build/configure-apk-splits).

بصمة SHA-256 الدائمة لشهادة توقيع الإصدار 0.2.0 هي:

```text
aa7835a298807cde50c272419bf88230f05b044c1a5c32591de307b7aa9731d1
```

لا تثبّت أي APK يُقدَّم كتحديث رسمي إذا كانت بصمة شهادته مختلفة.

### الترقية من Drover Quest 0.1.0

معرّف التطبيق في 0.2.0 هو `app.drover.android`، ولذلك يعتبره Android تطبيقًا
مختلفًا عن `app.drover.quest`. احذف النسخة القديمة أولًا، ثم ثبّت 0.2.0 وامنح
إذني VPN والإشعارات من جديد:

```powershell
adb uninstall app.drover.quest
adb install Drover-for-Android-v0.2.0-universal.apk
```

حذف النسخة القديمة يحذف إعداداتها. لا تستخدم `adb install -r` لاستبدال 0.1.0
لأن معرّفي الحزمة مختلفان.

## ماذا يفعل التطبيق؟

- ينشئ Android VPN محليًا يقتصر على نسخة Discord المختارة.
- يدعم Discord Stable (`com.discord`) وPTB (`com.discord.ptb`) وCanary
  (`com.discord.canary`). إذا وجدت عدة نسخ، يطلب Drover اختيار واحدة ويحفظها.
- يمرر TCP وDNS وIPv4 وIPv6 وحزم UDP العادية مباشرة إلى الإنترنت.
- عند أول حزمة Discord UDP بحجم 74 بايت، يرسل تسلسل Drover ‏`00` ثم `01`
  وينتظر 50ms قبل تمرير الحزمة الأصلية. يطابق ذلك Direct mode في
  [discord-drover](https://github.com/hdrover/discord-drover).
- لا يفك تشفير اتصال Discord ولا يحتوي تحليلات أو telemetry، ولا يصل إلى
  الحساب أو الرسائل أو الميكروفون أو الصوت.
- يتضمن شاشة **تراخيص المصادر المفتوحة** تعمل دون إنترنت وتحفظ إشعارات مجموعة
  اعتماد البناء المباشر المدققة كاملة.

Drover ليس VPN جغرافيًا أو VPN خصوصية، ولا يخفي عنوان IP. يسمح Android باتصال
VPN واحد فقط؛ تشغيل VPN آخر يوقف Drover.

## الاستخدام

1. ثبّت تطبيق Discord الرسمي وأحد ملفي APK أعلاه.
2. افتح **Drover for Android**، واختر نسخة Discord إذا طُلب منك ذلك.
3. اضغط **تشغيل وفتح Discord**، ثم وافق على طلب Android VPN في المرة الأولى.
   يفتح Drover نسخة Discord المختارة بعد جاهزية النفق.
4. اترك Drover يعمل أثناء استخدام صوت Discord. تستطيع إيقافه من إشعار Android
   الدائم من دون إعادة فتح التطبيق.

افتح **تراخيص المصادر المفتوحة** من الشاشة الرئيسية لقراءة جميع إشعارات
الأطراف الثالثة المضمنة. لا يُحمّل النص الكبير إلا عند فتح هذه الشاشة، ولذلك
لا يضيف استهلاك CPU أثناء الخمول.

عندما يكون النفق عاملًا يتحول الزر الرئيسي إلى **فتح Discord**. إذا حُذفت نسخة
Discord المحفوظة، يكتشف Drover ذلك ويطلب تثبيت نسخة أو اختيار نسخة أخرى.

## التشغيل بعد إعادة التشغيل وAlways-on VPN

خيار **تشغيل Drover بعد إعادة تشغيل الجهاز** اختياري ومتوقف افتراضيًا. فعّله
بعد منح إذن VPN فقط. بعد إعادة التشغيل يبدأ Drover خدمة VPN الأمامية بهدوء من
دون إجبار واجهة على الظهور؛ قد يعرض Android الإشعار أو ينتظر اكتمال إقلاع
الجهاز. إذا سُحب إذن VPN، اضغط الإشعار لفتح Drover ومنح الإذن من جديد.
في Android 13 والأحدث، اسمح بالإشعارات كي يصلك تنبيه الإعداد ويظهر زر
**إيقاف** في إشعار الخدمة. إذا رفضتها فسيظل VPN يعمل، لكن يجب أن تفتح Drover
بنفسك عندما يحتاج الإعداد إلى تدخل.

يدعم Drover أيضًا إعداد Android المسمى **Always-on VPN** على الأجهزة التي
تُظهره. هذا إعداد اختياري ومستقل عن خيار التشغيل بعد الإقلاع داخل التطبيق.
يتبع التنفيذ توثيق أندرويد الخاص
[باستثناءات تشغيل الخدمة الأمامية بعد الإقلاع](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)،
[وقيود فتح الواجهات من الخلفية](https://developer.android.com/guide/components/activities/secure-bal)،
[وسلوك `VpnService`](https://developer.android.com/reference/android/net/VpnService).

> اترك **حظر الاتصالات بدون VPN**، أو Lockdown، **متوقفًا**. Drover هو VPN
> مخصص لـDiscord عمدًا، وقد يمنع Lockdown وصول التطبيقات الموجودة خارج قائمة
> Drover إلى الشبكة.

## استهلاك الموارد

لا يمكن لأي VPN عامل أن يستهلك صفر CPU أو RAM حرفيًا؛ يجب على Android إبقاء
الخدمة الأمامية وحلقة تمرير الحزم الأصلية في الذاكرة. يقلل Drover الاستهلاك
أثناء الخمول باستخدام تحديثات واجهة مبنية على الأحداث وعمليات شبكة حاجبة،
وخيطَي Tokio غير متزامنين محددين صراحة، ومن دون Wake Locks أو alarms أو مهام
مجدولة أو polling في الخلفية أو telemetry. خيط Java الذي يستدعي المحرك منفصل
عن الخيطين. لا يراقب Drover عملية Discord باستمرار، ويبقى عاملًا حتى توقفه
أو يوقف Android اتصال VPN.

قبل ترقية النسخة من Pre-release، ينبغي أن تشمل اختبارات القبول قياس CPU وعدد
الخيوط وPSS على Quest 3، إضافة إلى مكالمة صوتية طويلة ودورات تشغيل وإيقاف
متكررة. يوضح وصف الإصدار ما اكتمل من التحقق حتى الآن. ثبات الاتصال أهم من أي
تحسين يسبب فقدان حزم أو تقطع الصوت.

## البناء من المصدر

المتطلبات:

- JDK 17
- Android SDK Platform 36 وBuild Tools 36
- Android NDK بالإصدار `26.1.10909125`
- Rust 1.97.1 وأهداف Android المستخدمة في `native/build-android.ps1`

تتوافق إصدارات AGP 8.11.1 وGradle 8.13 وJDK 17 وAPI 36 مع
[جدول توافق AGP 8.11 الرسمي](https://developer.android.com/build/releases/agp-8-11-0-release-notes).

على Windows، أعد بناء المحرك المثبت بإعداد Direct-only، ثم ابنِ نسختي debug:

```powershell
$env:ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\Sdk"
$Tun2proxySource = Join-Path ([IO.Path]::GetTempPath()) ("tun2proxy-" + [Guid]::NewGuid().ToString("N"))
git init $Tun2proxySource
git -C $Tun2proxySource remote add origin https://github.com/tun2proxy/tun2proxy.git
git -C $Tun2proxySource fetch --depth=1 origin fc77ca3182b3a63b84266bb0a5d24c096e022765
git -C $Tun2proxySource checkout --detach FETCH_HEAD
.\native\build-android.ps1 `
    -SourceDir $Tun2proxySource `
    -NdkRoot "$env:ANDROID_SDK_ROOT\ndk\26.1.10909125"
$Cargo197 = (rustup which --toolchain 1.97.1 cargo).Trim()
$Rustc197 = (rustup which --toolchain 1.97.1 rustc).Trim()
python .\.github\scripts\generate-third-party-licenses.py `
    --inventory .\DEPENDENCY_LICENSES.md `
    --tun2proxy-manifest "$Tun2proxySource\Cargo.toml" `
    --project-license .\LICENSE `
    --cargo $Cargo197 `
    --rustc $Rustc197 `
    --output .\app\src\main\assets\third_party_licenses.txt `
    --check
.\gradlew.bat lintUniversalDebug lintArm64Debug assembleUniversalDebug assembleArm64Debug
```

يستخدم بناء المحرك `--frozen --no-default-features` لـARM64 وARMv7 وx86_64،
ثم ينسخ المكتبات إلى `app/src/main/jniLibs/`. راجع
[`native/README.md`](native/README.md) لمعرفة المصدر والـpatch المثبتين وخطوات
إعادة الإنتاج.

تقرأ نسخ release بيانات التوقيع من متغيرات البيئة فقط:

```text
ANDROID_KEYSTORE_FILE
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

يحتفظ سكربت المحرك بملفات DWARF منفصلة داخل `app/build/native-symbols/`. نوع
البناء `profile` قابل للتوقيع بمفتاح debug ويفعّل JNI debugging للقياسات
المحلية. يحتفظ workflow الإصدار بالرموز كـActions artifact منفصل قصير العمر،
ولا يضعها ضمن ملفات الإصدار العامة. تحتوي GitHub Releases على ملفي Universal
وARM64 فقط بعد التحقق من التوقيع والمعماريات ومحاذاة 16KiB، وفق
[إرشادات أندرويد لحجم الصفحة 16KiB](https://developer.android.com/guide/practices/page-sizes).

## تنبيه التوقيع للمشرفين

تتوقع أتمتة إصدار GitHub ملف المفتاح الدائم مشفرًا بـBase64 وبياناته ضمن
GitHub Actions Secrets بالأسماء التالية:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

لا يسمح GitHub بقراءة قيمة السر بعد حفظها. وفق سياسة المشروع بعدم الاحتفاظ
بنسخة محلية، فإن حذف هذه الأسرار أو فقدها يلغي نهائيًا القدرة على إصدار تحديث
موقّع بهوية التطبيق نفسها. لا تطبع المفتاح أو كلمات المرور أو Base64 في سجل CI.

## أصحاب المشروع والمساهمون

1. [Akramx15](https://github.com/Akramx15) — صاحب المشروع والفكرة والتوجيه.
2. [AhmadotEng](https://github.com/AhmadotEng) — مساهم في تطوير المشروع.
3. OpenAI Codex — المساعدة في التصميم والتنفيذ والمراجعة التقنية.

المشروع مرخص بـMIT. راجع [`LICENSE`](LICENSE) و
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) و
[`DEPENDENCY_LICENSES.md`](DEPENDENCY_LICENSES.md).
