# TiktokPatchXposed

LSPosed-модуль с набором исправлений и дополнительных функций для TikTok.

Текущая версия модуля: **3.14** (`com.golda.patchertiktok`). Работа проверена с TikTok **47.0.3** (`com.zhiliaoapp.musically`).

Модуль имеет экран настроек (launcher activity) для включения/выключения функций. Значения по умолчанию сохраняют прежнее поведение Vietnam-патча.

## Возможности

- Подмена SIM-карты и оператора на Вьетнам для доступа к региональным функциям при сохранении вьетнамского языка приложения.
- Вьетнамский профиль запросов ленты рекомендаций (`VN`, `45204`, `vi-VN`).
- Скачивание видео без водяного знака.
- Скрытие рекламы в ленте и при запуске TikTok, включая TopView, псевдорекламу и рекламу, добавленную после загрузки страницы.
- Фильтрация LIVE-трансляций и скрытие кнопки LIVE в левом верхнем углу.
- Постоянно доступная полоса перемотки для обычных видео, включая короткие ролики и переходы между роликами.
- Скрытие видео с серверной пометкой «Ваши вероятные знакомые» без отключения обычных новых рекомендаций.
- Исправление входа через Google на основе идеи патча ReVanced для TikTok.
- Оптимизированные UI-хуки работают только в основном процессе TikTok. Постоянного сервиса, фонового опроса, обработчика загрузки системы и требования отключать экономию батареи нет.

## Региональный профиль

- Страна и SIM: `VN` / `vn`
- MCC/MNC: `45204`
- Оператор: `Viettel`
- Язык приложения: `vi-VN`
- Язык рекомендаций: `vi`
- Сигналы запросов ленты: `VN` / `45204`; используется локальное время устройства
- Остальные региональные сигналы TikTok: `VN` / `45204` / `Viettel`
- Часовой пояс приложения: локальный часовой пояс устройства

## Совместимость

Работа проверена с TikTok 47.0.3.

После обновления TikTok внутренние обфусцированные классы могут измениться.

## Установка

1. Установите APK модуля.
2. Включите модуль для TikTok в LSPosed.
3. Принудительно остановите TikTok и откройте его заново.

## Важно

- Использование Xposed-модулей может привести к нестабильной работе приложения или блокировке аккаунта. Все изменения применяются на ваш риск.
- Фильтрация рекламы/LIVE включена по умолчанию.
- Экспериментальный перевод комментариев удалён из стабильной сборки.
- Для новых рекомендаций подмена применяется только к запросам ленты. На рекомендации также могут влиять история аккаунта, IP-адрес и действия пользователя.
- Изменение скорости воспроизведения не добавлено: для него пока нет стабильного Xposed-хука, не зависящего от версии TikTok.

---

## English

An LSPosed module with fixes and additional features for TikTok.

Current module version: **3.14** (`com.golda.patchertiktok`). Verified with TikTok **47.0.3** (`com.zhiliaoapp.musically`).

The module ships a settings screen (launcher activity) to toggle features. Defaults preserve the previous hardcoded Vietnam-patch behavior.

### Features

- Settings screen (Activity + ContentProvider): Vietnam profile, feed filters, seekbar, download, playback speed, keyword blacklist, views/likes range, page purification.
- Hide photo posts, AI-generated posts and long videos (configurable threshold).
- Keyword blacklist for captions/hashtags and view/like count ranges.
- Optional playback speed 1.0x–2.0x (best-effort on TikTok 47.0.3 player hooks).
- Vietnam SIM/operator spoof for regional feature availability with Vietnamese app language.
- Vietnamese recommendation-feed request profile (`VN`, `45204`, `vi-VN`).
- Download videos without a watermark.
- Startup and in-feed ad filtering, including TopView, pseudo-ad markers and ads inserted after a page is loaded.
- LIVE feed filtering and removal of the top-left LIVE button.
- An always-available seekbar for normal videos, including short clips and feed transitions.
- Filtering for videos marked as "People you may know" without disabling ordinary fresh recommendations.
- Google Auth Fix based on the ReVanced TikTok Google login patch idea.
- UI hooks run only in TikTok's main process. The module does not use a persistent service, polling daemon, boot receiver or battery-optimization exemption.

### Region profile

- Country and SIM: `VN` / `vn`
- MCC/MNC: `45204`
- Operator: `Viettel`
- App locale: `vi-VN`
- Recommendation language: `vi`
- Recommendation-feed signals: `VN` / `45204`; the device's local time is used
- Other TikTok market signals: `VN` / `45204` / `Viettel`
- App time zone: the device's local time zone

### Compatibility and installation

Verified with TikTok 47.0.3.

1. Install the module APK.
2. Enable it for TikTok in LSPosed.
3. Force-stop and reopen TikTok.

Internal obfuscated classes may change after a TikTok update.

### Important

- Xposed modules may cause account restrictions or unstable app behavior. Use this module at your own risk.
- Ad/LIVE filtering is enabled by default.
- Experimental comment translation was removed from the stable build.
- Region spoofing applies only to recommendation-feed requests. Account history, IP location and interactions may still affect recommendations.
- Playback-speed controls were not added because there is no stable version-independent Xposed hook yet.
