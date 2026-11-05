# CHANGELOG

## 2026-11-05 — форк NeXX (интеграция в simplex-fork)

Исправлены 2 бага оригинала, найденные при аудите и интеграции:

1. **Мёртвая кнопка `?123`/`ABC`.** `isSymbolsMode` объявлялся, но нигде не
   присваивался — ряд символов был недостижим, а `StdKey.SymbolsToggle` уходил хосту,
   который его игнорировал. Теперь переключение режима символов обрабатывается
   внутри библиотеки через обёртку `emitKey` (хосту `SymbolsToggle` больше не приходит).
2. **Падение `Layout` при неограниченной высоте родителя.**
   `Constraints.fixed(w, constraints.maxHeight)` + `layout(w, constraints.maxHeight)`
   ломают измерение при `maxHeight = Infinity` (типично для Column под полем ввода).
   Теперь высота ряда ограничена: `if (constraints.hasBoundedHeight) maxHeight else 44.dp.roundToPx()`.
   `LocalDensity.current` в measure-ламбде не читается (не @Composable) — `MeasureScope` : Density.
3. **material3 → BasicText.** `androidx.compose.material3.Text` заменён на
   `androidx.compose.foundation.text.BasicText` + `TextStyle`: material3 тянет
   `material-icons-core`, которого нет в offline-кеше форка. Визуально идентично.
4. Модуль приведён к версиям форка: AGP/Kotlin из `pluginManagement` (8.7.0 / 2.1.20),
   compose `ui`/`foundation`/`runtime` 1.8.2, coroutines форснут на 1.11.0 (кеш),
   compileSdk 35, minSdk 26, пустой манифест (namespace в gradle).

Проверено в собранном APK форка: классы `com/narde/keyboard` присутствуют в dex
(classes6.dex, classes14.dex), сборка `:android:assembleFossDebug` зелёная.

## 2026-11-05 — исходная версия (Narde)

- `StandardKeyboard` / `StdKey`: портретная раскладка системной клавиатуры Android,
  RU/EN, shift, звук клика, кнопка сворачивания `▼`.
- Убраны матричный дождь, анимации, неоновая тема и индикаторы состояния.
