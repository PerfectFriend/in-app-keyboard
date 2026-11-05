# Аудит приватности: перехватывает ли клавиатура ввод

Дата аудита: 2026-11-05. Аудит проводился перед интеграцией в форк SimpleX (NeXX).

## Что проверяется

Клавиатурная библиотека безопасна, только если **весь** ввод уходит единственным
каналом — через callback `onKey` хоста — и больше никуда.

## Чек-лист

| Проверка | Как | Результат |
|---|---|---|
| Сетевые вызовы | `grep -rn "http\|socket\|HttpClient\|okhttp" src/` | нет |
| Логирование ввода | `grep -rn "Log\.\|println\|Timber" src/` | нет |
| Персистентность | `SharedPreferences\|SQLite\|DataStore\|File(` | нет |
| Доступ к буферу обмена | `ClipboardManager` | нет |
| IME / Accessibility сервисы | манифест: нет `<service>` с `BIND_INPUT_METHOD`/`ACCESSIBILITY` | нет (манифест пустой) |
| Permissions | манифест пустой | ни одного permission |
| InputConnection-хитрости | переопределения `onCreateInputConnection` | нет — компонент Compose, не View-IME |
| Callback — единственный выход | весь `onKey(...)` уходит хосту | да |

## Как перепроверить самостоятельно

```bash
grep -rn -i "network\|http\|socket\|send\|upload\|Log\.\|println\|SharedPreferences\|SQLite\|ContentProvider\|AccessibilityService\|keylog" keyboard/src/
cat keyboard/src/main/AndroidManifest.xml   # должен быть пустым
```

Любой hit в тексте клавиши = отказ от интеграции.

## Почему это важно здесь

Системная IME (Gboard) — это отдельный процесс с доступом ко всему вводу во всех
приложениях. In-app Compose-клавиатура живёт в процессе приложения: текст не
покидает процесс, пока хост сам его не отправит. Для мессенджера это единственный
способ гарантированно исключить клавиатурного кейлоггера уровня системы.

Единственное, за что отвечает хост: куда уходит текст после `onKey`.
В форке NeXX текст идёт только в `composeState` чата (локально + отправка в SimpleX-сеть).
