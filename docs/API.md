# API библиотеки `com.narde.keyboard`

## Публичный API

Единственный файл: `keyboard/src/main/java/com/narde/keyboard/StandardKeyboard.kt`

### `StdKey` — sealed interface событий клавиатуры

| Событие | Смысл | Кто обрабатывает |
|---|---|---|
| `StdKey.CharKey(text: String)` | буквенный/символьный ввод | хост (добавить в текст) |
| `StdKey.Space` | пробел | хост |
| `StdKey.Backspace` | ⌫ удалить | хост |
| `StdKey.Enter` | ⏎ ввод | хост (обычно отправка) |
| `StdKey.Shift` | ⇧ — заглавные | **внутри библиотеки** (не приходит хосту) |
| `StdKey.LangToggle` | RU/EN | **внутри библиотеки** (не приходит хосту) |
| `StdKey.SymbolsToggle` | ?123/ABC | **внутри библиотеки** (не приходит хосту, см. CHANGELOG) |
| `StdKey.Hide` | ▼ свернуть клавиатуру | **хост обязан обработать** |

### `StandardKeyboard(onKey, lang, modifier)`

```kotlin
@Composable
fun StandardKeyboard(
    onKey: (StdKey) -> Unit,
    lang: String = "RU",          // "RU" или "EN" — начальная раскладка
    modifier: Modifier = Modifier
)
```

- `onKey` — единственный канал событий наружу. Никакого другого выхода данных нет.
- Внутреннее состояние (язык, shift, режим символов) — локальные `mutableStateOf`,
  никуда не сохраняются и не читаются.

## Раскладки

- EN ряд1: `q w e r t y u i o p` + `⌫`; ряд2: `a s d f g h j k l` + `⏎`; ряд3: `⇧ z x c v b n m , . ?123`
- RU ряд1: `й ц у к е н г ш щ з х ъ` + `⌫`; ряд2: `ф ы в а п р о л д ж э` + `⏎`; ряд3: `⇧ я ч с м и т ь б ю , . ?123`
- Symbols ряд1: `1..0` + `⌫`; ряд2: `@ # $ % & * - + = (` + `⏎`; ряд3: `) ! ? " ' : ; / \ ABC`
- Нижний ряд: `RU/EN` + широкий `Пробел` + `▼`

## Размеры

Клавиша: высота фиксирована `44.dp`, скругление `8.dp`, равная ширина в ряду
(через кастомный `Layout`, без `Row(weight)` — веса в Compose internal).
Ряды отстоят друг от друга на `4.dp`, внешний padding `4.dp`/`8.dp`.
