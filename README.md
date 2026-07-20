# Агенты — Android

Примитивный чат с агентами Das Experten.  
Живой backend = **тот же** Cloudflare Worker / D1 / board chat, что и `org.dasexperten.com` (и терминал/доска).

## Что есть

1. **Экран агентов** — только фото + имя. Тап = multi-select (нажатое состояние).
2. **Чат** — один общий тред: одно сообщение шефа → **все выбранные** агенты отвечают.
3. **Буквы (LS / MN / ML…)** — фокус и фильтр по агенту в этом же чате (не другой backend).
4. Фон чата — портрет активного агента + белый semi-transparent scrim.
5. Ввод ~4 строки, отправка справа.

## Backend

| Действие | API |
|---|---|
| Roster | `GET https://org.dasexperten.com/api/agents` |
| История | `GET …/api/agents/{slug}/chat/history` |
| Сообщение | `POST …/api/agents/{slug}/chat` (`channel=board`, `use_history=true`, `source=android`) |
| Фото | `…/assets/agents/{slug}.png` · full: `{slug}-full.png` |

История **общая с board**.

## Сборка

Нужны **Android Studio** (Ladybug+) и JDK 17.

```bash
cd /Users/dasexperten/Projects/das-agents-android
# Открыть папку в Android Studio → Sync → Run
```

Или из CLI (если настроен SDK):

```bash
./gradlew :app:assembleDebug
```

Если wrapper-jar отсутствует: **File → New → Import** в Android Studio подтянет Gradle wrapper.

Package: `com.dasexperten.agents` · label: **Агенты**.

Опционально API key: в `app/build.gradle.kts` поле `ORG_API_KEY` (сейчас Worker открыт, ключ не обязателен).

## Smoke

1. Список агентов грузится с org.
2. Выбрать **Mina Rutunya + Marika Nowicka** → Чат.
3. Одно сообщение → два ответа с инициалами.
4. Чипы `MN` / `ML` — фокус; второй тап — фильтр.
5. История совпадает с board после refresh.

## Вне v1

Voice, digest, tasks, offline LLM, iOS, Play Store.
