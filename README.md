<div align="center">

# RecepyBook · Рецептник

**Бесплатное Android-приложение для рецептов: сохраняй рецепты из Instagram, TikTok и YouTube одним нажатием, планируй меню и собирай список покупок.**

[![CI](https://github.com/MrIsrak/RecepyBook/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/MrIsrak/RecepyBook/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/MrIsrak/RecepyBook?label=%D0%B2%D0%B5%D1%80%D1%81%D0%B8%D1%8F)](https://github.com/MrIsrak/RecepyBook/releases/latest)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)

[**⬇ Скачать APK**](https://github.com/MrIsrak/RecepyBook/releases/latest) · [Возможности](#возможности) · [Установка](#установка) · [Сборка](#сборка-из-исходников) · [English](#english)

</div>

---

## Зачем

Рецепты сейчас живут в соцсетях: короткое видео, а ингредиенты где-то в подписи между эмодзи и хэштегами. Платные приложения вроде ReciMe решают это за подписку. **RecepyBook делает то же самое бесплатно**, без рекламы, без аккаунта: все данные хранятся только на телефоне.

## Возможности

| | |
|---|---|
| 🔗 **Импорт по ссылке** | Instagram, TikTok, YouTube (и Shorts), Facebook, Pinterest и любые сайты с разметкой schema.org/Recipe. Нажимаешь «Поделиться» → «Рецептник», и приложение достаёт подпись, разбирает ингредиенты и шаги, сохраняет обложку |
| 👀 **Превью перед сохранением** | Видно, что распозналось: число ингредиентов и шагов, время, калории. Всё можно поправить |
| 📷 **С фото** | Скриншот или страница кулинарной книги (распознавание текста на устройстве) |
| 🔥 **Калории и БЖУ** | Встроенная таблица ~130 продуктов с названиями на русском, английском и иврите; расчёт на порцию |
| ⚖️ **Порции и единицы** | Пересчёт количеств под любое число порций; «как в рецепте» / граммы и мл / US (cups, oz) |
| 📚 **Кулинарные книги** | Собирай рецепты в подборки с эмодзи и обложками |
| 📅 **План питания** | Неделя: завтрак, обед, ужин, перекус; калории за день; вся неделя в покупки одной кнопкой |
| 🛒 **Список покупок** | По отделам магазина или по рецептам; одинаковые продукты складываются; отправка в WhatsApp или Telegram |
| 👨‍🍳 **Режим готовки** | Крупный текст по шагам, экран не гаснет, таймер из шага одним нажатием |
| 🔎 **Поиск и сортировка** | По названию, ингредиентам, тегам; новые, по алфавиту, быстрые, менее калорийные, недавно готовил |
| 💾 **Резервная копия** | Экспорт всего (вместе с фото) в один файл, восстановление на другом телефоне |

### Умный разбор через ИИ (необязательно)

Встроенный парсер понимает подписи со списком ингредиентов. Если добавить **бесплатный** ключ Google Gemini (*Настройки → Умный разбор*), приложение будет:
- разбирать подписи без явного списка;
- понимать фото на любом языке;
- переводить рецепты на русский;
- точнее оценивать калорийность.

Ключ хранится только на устройстве. Как его получить, описано на экране настроек.

## Установка

1. Открой [последний релиз](https://github.com/MrIsrak/RecepyBook/releases/latest) с телефона.
2. Скачай `RecepyBook-x.y.z.apk`.
3. Открой файл и разреши установку из этого источника, если Android спросит.

Требуется Android 8.0 или новее. Контрольные суммы лежат в `SHA256SUMS.txt` рядом с APK.

## Сборка из исходников

Нужны JDK 17 и Android SDK (проще всего через Android Studio Ladybug или новее).

```bash
git clone https://github.com/MrIsrak/RecepyBook.git
cd RecepyBook
./gradlew testDebugUnitTest   # unit-тесты
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```

## Архитектура

```
app/src/main/java/app/receptnik/
├── core/       чистый Kotlin без Android: разбор ингредиентов, единицы, таблица КБЖУ, разбор подписей
├── importer/   загрузка ссылок по платформам, schema.org JSON-LD, фото (ML Kit), клиент Gemini
├── data/       Room (рецепты, книги, план, покупки), DataStore-настройки, хранилище картинок, бэкап
└── ui/         Jetpack Compose, Material 3, Navigation
```

Подробнее см. [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

**Стек:** Kotlin 2.0 · Jetpack Compose · Material 3 · Room · DataStore · OkHttp · Jsoup · Coil · ML Kit · kotlinx.serialization

## Разработка

- `main`: стабильная ветка, из неё выпускаются релизы. Прямые коммиты запрещены, только через PR.
- `develop`: текущая разработка; фичи вливаются сюда через PR из `feature/*`.
- Релиз: PR `develop → main`, затем тег `vX.Y.Z`. GitHub Actions соберёт APK и опубликует релиз.

Правила описаны в [CONTRIBUTING.md](CONTRIBUTING.md), история изменений в [CHANGELOG.md](CHANGELOG.md).

## Ограничения

- Часть постов Instagram и Facebook закрыта для неавторизованного доступа. Тогда берётся текст из превью ссылки (обычно это вся подпись).
- Если рецепт только озвучен в видео, а в подписи его нет, ингредиенты нужно вписать вручную.
- Калорийность — оценка по таблице продуктов, а не лабораторные данные.

## Конфиденциальность

Аккаунтов, аналитики и рекламы нет. Подробнее см. [PRIVACY.md](PRIVACY.md).

## Лицензия

[MIT](LICENSE) © 2026 MrIsrak

---

## English

**RecepyBook** is a free, ad-free Android recipe manager, an open alternative to subscription apps like ReciMe. Share a post from Instagram, TikTok, YouTube, Facebook or Pinterest (or any recipe website) to the app. It extracts the caption, parses ingredients and steps, saves the cover image and estimates calories and macros. Also included: cookbooks, a weekly meal planner, a smart grocery list grouped by store aisle, serving scaling, metric/US unit conversion, a hands-free cook mode with timers, and full backup/restore. Optional AI parsing uses your own free Google Gemini key. All data stays on the device.

[Download the latest APK](https://github.com/MrIsrak/RecepyBook/releases/latest) · MIT licensed
