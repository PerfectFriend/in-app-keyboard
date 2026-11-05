# in-app-keyboard

In-app Compose-клавиатура для Android, заменяющая системную soft-клавиатуру (Gboard и т.п.).
Чистая UI-библиотека: **никакого перехвата ввода** — каждое нажатие уходит только в callback хоста.

- Пакет: `com.narde.keyboard`
- Модуль: `:keyboard` (Android Library, Compose)
- Источник: `keyboard/src/main/java/com/narde/keyboard/StandardKeyboard.kt`
- Документация: `docs/API.md`, `docs/INTEGRATION.md`, `docs/PRIVACY-AUDIT.md`, `docs/CHANGELOG.md`

## Возможности

- Точная копия портретной раскладки системной клавиатуры Android: 3 ряда букв + `⌫`/`⏎`/`⇧`, нижний ряд `RU/EN` + `Пробел` + `▼` (свернуть).
- Раскладки RU (ЙЦУКЕН) и EN (QWERTY), переключение внутри библиотеки.
- Режим символов `?123` / `ABC` (цифры + спецсимволы), переключение внутри библиотеки.
- Shift: заглавные после нажатия, автосброс после одной буквы.
- Звук нажатия (`SoundEffectConstants.CLICK`, локально).
- Кнопка `▼` (`StdKey.Hide`) — **хост обязан сам скрыть клавиатуру**.

## Быстрый старт

```kotlin
import com.narde.keyboard.StandardKeyboard
import com.narde.keyboard.StdKey

@Composable
fun MyInput() {
    var text by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    StandardKeyboard(
        modifier = Modifier.fillMaxWidth(),
        lang = "RU",
        onKey = { key ->
            when (key) {
                is StdKey.CharKey -> text += key.text
                StdKey.Space -> text += " "
                StdKey.Backspace -> if (text.isNotEmpty()) text = text.dropLast(1)
                StdKey.Enter -> { /* отправить */ }
                StdKey.Hide -> keyboardController?.hide() // обязательно!
                else -> {} // Shift/LangToggle/SymbolsToggle уже обработаны внутри
            }
        }
    )
}
```

Подключение модуля и пошаговая интеграция — в `docs/INTEGRATION.md`.

## Приватность

Библиотека не содержит сети, логирования, персистентности, сервисов и permissions
(пустой манифест). Весь текст ввода остаётся в процессе приложения-хоста.
Чек-лист аудита и как его перепроверить — в `docs/PRIVACY-AUDIT.md`.

## Лицензия

Внутренний проект — используйте по правилам репозитория-владельца.
