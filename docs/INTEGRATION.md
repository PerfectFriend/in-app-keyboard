# Интеграция in-app клавиатуры в Android-проект

Цель: отказаться от системной soft-клавиатуры (Gboard) в конкретном поле ввода
и показывать свою Compose-клавиатуру под ним.

## Шаг 1. Модуль в репозиторий

Скопируйте папку `keyboard/` в корень своего проекта.

## Шаг 2. Регистрация модуля

`settings.gradle.kts`:

```kotlin
include(":app")
include(":keyboard")
```

Версия плагина `com.android.library` — через `pluginManagement` (как у корня проекта).

## Шаг 3. Зависимость

В `build.gradle.kts` целевого модуля:

```kotlin
dependencies {
    implementation(project(":keyboard"))
}
```

Зависимости самого модуля — только Compose (`ui`, `foundation`, `runtime`)
и `kotlinx-coroutines` (транзитивно от compose). **material3 не требуется** —
текст рисуется `BasicText` из foundation (это важно для offline-сборок: material3
тянет доп. артефакты, которых может не быть в кеше).

### Pitfall: версии под существующий проект

Все Android-модули одного build должны использовать **одну версию AGP**.
Берите версии плагинов из `pluginManagement` корня, а compose-артефакты —
те же, что уже использует проект (смотрите их в gradle-кеше), иначе
offline-сборка упадёт на "No cached version ... available for offline mode".

### Pitfall: манифест библиотеки

В AGP 8+ атрибут `package=` в `AndroidManifest.xml` запрещён — namespace задаётся
в `build.gradle.kts` (`android { namespace = "com.narde.keyboard" }`), манифест пустой.

## Шаг 4. Использование в поле ввода

```kotlin
import com.narde.keyboard.StandardKeyboard
import com.narde.keyboard.StdKey
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@Composable
fun MyInput() {
    var text by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    var showKeyboard by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            // системная IME не должна всплывать при фокусе:
            keyboardOptions = KeyboardOptions(showKeyboard = false) // для Compose-полей
            // для Android EditText: editText.showSoftInputOnFocus = false
        )
        if (showKeyboard) {
            StandardKeyboard(
                modifier = Modifier.fillMaxWidth(),
                lang = "RU",
                onKey = { key ->
                    when (key) {
                        is StdKey.CharKey -> text += key.text
                        StdKey.Space -> text += " "
                        StdKey.Backspace -> if (text.isNotEmpty()) text = text.dropLast(1)
                        StdKey.Enter -> send()
                        StdKey.Hide -> {
                            showKeyboard = false
                            keyboardController?.hide()
                        }
                        else -> {}
                    }
                }
            )
        }
    }
}
```

## Шаг 5. Критически важно: отключить системную IME

Иначе получите две наложенные клавиатуры.

- **Compose TextField**: `KeyboardOptions(showKeyboard = false)` или
  `LocalSoftwareKeyboardController.current?.hide()` при фокусе.
- **Android EditText (AndroidView)**: `editText.showSoftInputOnFocus = false` в factory
  и `hideKeyboard(editText)` в `onFocusChangeListener`. **Удалите все вызовы
  `imm.showSoftInput(...)` для этого поля** — их вызов = всплытие Gboard.

## Шаг 6. Patтерны ввода в существующее поле (пример: форк SimpleX/NeXX)

В форке поле ввода чата — `AndroidView` с `EditText` (`PlatformTextField.android.kt`).
Клавиатура вставлена в `Column` под `AndroidView`, а ввод делается программно —
тогда `doOnTextChanged` сам зовёт `onMessageChange` и состояние не рассинхронизируется:

```kotlin
val editTextRef = remember { mutableStateOf<EditText?>(null) } // в factory: editTextRef.value = editText

fun onStdKey(key: StdKey) {
    val et = editTextRef.value ?: return
    val s = et.selectionStart; val e = et.selectionEnd
    when (key) {
        is StdKey.CharKey -> et.text.replace(minOf(s, e), maxOf(s, e), key.text)
        StdKey.Space -> et.text.replace(minOf(s, e), maxOf(s, e), " ")
        StdKey.Backspace ->
            if (s != e) et.text.replace(minOf(s, e), maxOf(s, e), "")
            else if (s > 0) et.text.replace(s - 1, s, "")
        StdKey.Enter -> onDone()
        StdKey.Hide -> { showKeyboard = false; et.clearFocus(); hideKeyboard(et) }
        else -> {}
    }
}
```

## Pitfall: Compose Layout внутри библиотеки

Ряды клавиш собраны на кастомном `Layout`:

- `LocalDensity.current` **нельзя** читать в measure-ламбде (не @Composable-контекст).
  `MeasureScope` сам реализует `Density` — пишите `44.dp.roundToPx()` прямо там.
- `Constraints.fixed(w, constraints.maxHeight)` и `layout(w, constraints.maxHeight)`
  ломаются, если родитель не ограничен по высоте (Column под полем ввода даёт
  `maxHeight = Infinity`). Ограничьте: `if (constraints.hasBoundedHeight) constraints.maxHeight else 44.dp.roundToPx()`.

## Проверка после интеграции

1. Сборка: `./gradlew :keyboard:compileDebugKotlin :app:assembleDebug`.
2. В собранном APK классы библиотеки должны быть в dex:
   `for d in classes*.dex; do unzip -p app.apk $d | strings | grep -q com/narde/keyboard && echo "$d OK"; done`
3. На устройстве: тап по полю — Gboard не всплывает, появляется своя клавиатура;
   `▼` скрывает её; RU/EN, ?123, ⇧ работают.
