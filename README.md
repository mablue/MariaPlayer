# 🎵 SmartMusic — Intelligent Android Audio & Podcast Player
> **Bilingual Documentation (English & فارسی)**  
> High-fidelity, offline-first music and podcast player powered by pure Kotlin algorithmic scoring, Camelot harmonic mixing, Liquid Glass design, and context-aware DSP.

---

## 🌟 Key Highlights / نکات برجسته

### English 🇬🇧
- **Pure Algorithmic Layer (A-O)**: Zero heavy ML, microsecond deterministic evaluation for harmonic mixing, fuzzy mood classification, recency, circadian rhythms, skip fatigue, and EBU R128 loudness normalization.
- **Camelot Wheel Harmonic Mixing**: Continuous harmonic distance calculations (1A–12B), adaptive crossfade timing, and seed-based "More Like This" automatic playlist generation.
- **35 Signature Experiences**: Vinyl & Cassette analog warmth, room reverb simulation, smart gapless, smart 3s rewind after pause, weekly replay statistics, and atmospheric soundscapes (Rain, Fireplace, Café).
- **Dedicated Podcasts & Audiobooks Tabs**: Fully separated data structures and UI tabs with voice clarity filters, -16 LUFS dialogue normalization, and per-item speed control.
- **Privacy & Offline First**: 100% on-device processing. No audio or user data leaves the phone.
- **Bilingual & Responsive**: Native English and Persian (فارسی) localization with Material 3 Liquid Glass aesthetics.

### فارسی 🇮🇷
- **هوش الگوریتمی خالص (A تا O)**: بدون مدل‌های سنگین و بدون مصرف باتری؛ محاسبات میکروسانیه‌ای برای هماهنگی گام‌های موسیقی (کملوت)، دسته‌بندی فازی حالات روحی، ریتم شبانه‌روزی و نرمال‌سازی صدا (EBU R128).
- **میکسینگ هارمونیک چرخه کملوت**: محاسبه پیوسته فواصل هارمونیک (1A تا 12B)، فید متقاطع هوشمند و ساخت خودکار پلی‌لیست‌های هم‌گام.
- **۳۵ تجربه انحصاری**: شبیه‌سازی حس گرم گرامافون و نوار کاست، طنین آکوستیک اتاق، حذف فواصل سکوت، برگشت هوشمند ۳ ثانیه‌ای پس از مکث، مرور هفتگی و فضاسازی محیطی (باران، شومینه، کافه).
- **جداسازی کامل پادکست و کتاب صوتی**: تفکیک کامل مدل‌ها و زبانه‌ها با فیلتر شفافیت کلام و تنظیم سرعت مجزا.
- **حریم خصوصی کامل و آفلاین**: کلیه پردازش‌ها بدون اتصال به اینترنت روی دستگاه انجام می‌شود.

---

## 📐 Architecture & Technology Stack
- **Architecture**: MVVM + Clean Domain Model + Reactive Kotlin Flows
- **UI Framework**: Jetpack Compose, Material 3, Dynamic Color & Liquid Glass Glassmorphism
- **Audio Engine**: AndroidX Media3 (ExoPlayer) + MediaSessionService + Version Guarded Bit-Perfect AudioMixerAttributes
- **Testing**: Local JVM Unit Tests & Robolectric test coverage

---

## 🛠️ CI / CD & Keystore Hardening
- **`.github/workflows/build.yml`**: Split jobs (`validate` -> `build` -> `test`), JDK 21 Temurin, Gradle Wrapper validation, single source of truth for keystores via validated `KEY_ALIAS` secret or auto-detection.
- **`.github/workflows/release.yml`**: Strictly follows the Artifact-Reuse Protocol via `workflow_run` and `workflow_dispatch`, downloading previously built APK artifacts without re-compiling or touching keystores.

---

## 📜 License
Distributed under the Apache-2.0 License.
