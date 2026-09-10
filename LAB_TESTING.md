# WhatsApp observe-only lab testing

[انتقل إلى التعليمات العربية](#اختبار-مختبر-whatsapp-بوضع-المراقبة-فقط)

Version `0.3.0-alpha.1` is an experiment for learning which network flows differ
between a WhatsApp call that connects and one that fails. It does **not** apply
the Discord preamble or any other WhatsApp-specific packet change, and it is
not expected to unblock a call yet. Stable `0.2.0` remains the Discord-only
release.

## What the trace contains

The bounded `drover-lab-trace.jsonl` file contains elapsed time, TCP/UDP,
IPv4/IPv6, destination IP address and port, plus the length and direction of up
to the first 32 UDP packets in each direction per session. The trace is limited
to 1 MiB and stays on the device until you explicitly export or share it.

It never records packet payloads, DNS question names, messages, contacts, phone
numbers, microphone data, or call audio. Destination IP addresses and call
timing are nevertheless private network metadata. Inspect the file, share it
privately only with the project maintainers, and delete exported copies after
the comparison. Do not attach a personal trace to a public GitHub issue.

“Observe-only” means Drover adds no WhatsApp-specific preamble and does not
intentionally change application datagram payload bytes. It is still a
userspace VPN, not a passive packet sniffer: forwarding can change source ports,
transport timing, and some resolver behavior. For that reason, run both the
working and failing captures through the same Drover build.

## Paired test protocol

Use the same device, WhatsApp version, account, test contact, and WhatsApp
**Protect IP address in calls** setting for both runs. Record the device model,
OS version, WhatsApp version, Wi-Fi/mobile carrier, network location, that
setting, time, and result next to each exported file.

1. On a **control network** where an ordinary WhatsApp call already works,
   select WhatsApp or WhatsApp Business in Drover, start observation, and open
   WhatsApp.
2. Attempt one agreed test call for 15–30 seconds. Do not discuss private
   information during the experiment.
3. Return to Drover, stop it completely, then export the JSONL as
   `working-<device>-<network>-<date>.jsonl`.
4. Repeat the same steps on the **failing/restricted network**, without changing
   the settings above, and export
   `failing-<device>-<network>-<date>.jsonl`.
5. Send both files together with the recorded test notes. A lone failing trace
   is much less useful than a matched working/failing pair.

If you want to compare WhatsApp's **Protect IP address in calls** option, make
that a second, separately labelled pair; do not toggle it between the two runs
of one pair. Stop another VPN before testing because Android and iOS allow only
one active VPN tunnel at a time.

## Platform notes

- **Android:** install a `0.3.0-alpha.1` lab APK, select WhatsApp, run the paired
  protocol, stop Drover, and use **Export latest metadata trace**. The lab
  package installs separately from the stable app. GitHub Actions debug APKs
  may be signed by a different temporary debug key on each run. Export the
  trace first; if Android reports an incompatible update, uninstall
  `app.drover.android.lab` and install the new artifact. Uninstalling deletes
  the lab's internal trace and settings.
- **iOS:** the GitHub CI artifact is an unsigned build for compilation review;
  it is not an installable IPA. A tester needs a Development-signed build,
  registered device, Packet Tunnel and App Group entitlements, and the
  development per-app VPN profile described in [`ios/BUILDING.md`](ios/BUILDING.md).
  Development per-app mapping is ignored by Ad Hoc, TestFlight, and App Store
  provisioning unless WhatsApp and the device are managed through MDM.

---

# اختبار مختبر WhatsApp بوضع المراقبة فقط

الإصدار `0.3.0-alpha.1` تجربة هدفها معرفة الفرق بين اتصالات شبكة مكالمة
WhatsApp ناجحة وأخرى فاشلة. لا يضيف تسلسل Discord ولا يجري أي تعديل خاص على
حزم WhatsApp، ولذلك **لا يُتوقع منه فتح المكالمات حاليًا**. يبقى الإصدار
المستقر `0.2.0` مخصصًا لـDiscord فقط.

## ماذا يحتوي السجل؟

يحتوي الملف المحدود `drover-lab-trace.jsonl` على الزمن المنقضي، ونوع TCP أو
UDP، وIPv4 أو IPv6، وعنوان IP للوجهة والمنفذ، إضافة إلى حجم واتجاه أول 32 حزمة
UDP كحد أقصى لكل اتجاه في كل جلسة. الحد الأقصى للملف 1 MiB، ويبقى على الجهاز
إلى أن تصدّره أو تشاركه بنفسك.

لا يسجل محتوى الحزم، ولا أسماء استعلامات DNS، ولا الرسائل أو جهات الاتصال أو
أرقام الهواتف أو الميكروفون أو صوت المكالمة. لكن عناوين IP وتوقيت المكالمة
تظل بيانات شبكة خاصة؛ راجع الملف وشاركه بصورة خاصة مع مشرفي المشروع فقط، ثم
احذف النسخ المصدرة بعد المقارنة. لا ترفق سجلك الشخصي في Issue عامة على GitHub.

تعني «المراقبة فقط» أن Drover لا يضيف مقدمة خاصة بـWhatsApp ولا يغيّر عمدًا
بايتات حمولة حزم التطبيق. لكنه يظل VPN يمرر الاتصال في userspace وليس أداة
التقاط سلبية؛ لذلك قد تتغير منافذ المصدر وتوقيت النقل وبعض سلوك DNS. لهذا يجب
تشغيل النسخة نفسها من Drover في تجربتي الشبكة الناجحة والفاشلة.

## بروتوكول الاختبار المزدوج

استخدم الجهاز نفسه وإصدار WhatsApp نفسه والحساب وجهة الاتصال التجريبية نفسها،
واترك خيار WhatsApp المسمى **حماية عنوان IP في المكالمات** بالقيمة نفسها في
التجربتين. دوّن طراز الجهاز، وإصدار النظام وWhatsApp، وشبكة Wi-Fi أو شركة
الاتصال، ومكان الشبكة، وقيمة الخيار، والوقت، ونتيجة الاتصال مع كل ملف.

1. على **شبكة مرجعية** تعمل فيها مكالمة WhatsApp العادية، اختر WhatsApp أو
   WhatsApp Business في Drover، وابدأ المراقبة، ثم افتح WhatsApp.
2. نفّذ مكالمة اختبار متفقًا عليها لمدة 15–30 ثانية، ولا تتحدث عن معلومات خاصة.
3. ارجع إلى Drover، وأوقفه بالكامل، ثم صدّر JSONL باسم
   `working-<device>-<network>-<date>.jsonl`.
4. كرر الخطوات نفسها على **الشبكة الفاشلة أو المقيدة** من دون تغيير الإعدادات،
   وصدّر الملف باسم `failing-<device>-<network>-<date>.jsonl`.
5. أرسل الملفين مع ملاحظات الاختبار المسجلة. سجل فاشل منفرد أقل فائدة بكثير من
   زوج متطابق لشبكة ناجحة وأخرى فاشلة.

إذا أردت مقارنة خيار **حماية عنوان IP في المكالمات** فأنشئ زوجًا ثانيًا بعناوين
واضحة؛ لا تغيّر الخيار بين تجربتي الزوج نفسه. أوقف أي VPN آخر قبل الاختبار لأن
Android وiOS يسمحان عادةً بنفق VPN نشط واحد فقط.

## ملاحظات المنصات

- **Android:** ثبّت APK المختبر `0.3.0-alpha.1`، واختر WhatsApp، ونفّذ الاختبار
  المزدوج، ثم أوقف Drover واضغط **تصدير آخر سجل بيانات وصفية**. حزمة المختبر
  منفصلة عن التطبيق المستقر. قد تستخدم ملفات debug من GitHub Actions مفتاحًا
  مؤقتًا مختلفًا في كل تشغيل. صدّر السجل أولًا؛ إذا رفض Android التحديث بسبب
  اختلاف التوقيع، احذف `app.drover.android.lab` ثم ثبّت الملف الجديد. حذف
  التطبيق يحذف السجل الداخلي وإعدادات المختبر.
- **iOS:** ملف GitHub CI غير موقّع ومخصص للتحقق من البناء، وليس IPA قابلًا
  للتثبيت. يحتاج المختبر إلى Development build موقّع، وجهاز مسجل، وصلاحيتي
  Packet Tunnel وApp Group، وملف VPN التجريبي المشروح في
  [`ios/BUILDING.md`](ios/BUILDING.md). لا يعمل تعيين VPN لكل تطبيق مع توقيع
  Ad Hoc أو TestFlight أو App Store إلا إذا كان الجهاز وWhatsApp مُدارين عبر MDM.
