# Как участвовать

Спасибо, что хочешь помочь RecepyBook!

## Ветки

| Ветка | Назначение |
|---|---|
| `main` | Стабильная версия. Только через PR из `develop` или `hotfix/*`. Каждый релиз — тег `vX.Y.Z` на `main` |
| `develop` | Интеграционная ветка, сюда вливаются фичи |
| `feature/<кратко>` | Новая возможность, ответвляется от `develop` |
| `fix/<кратко>` | Исправление, ответвляется от `develop` |
| `hotfix/<кратко>` | Срочное исправление релиза, ответвляется от `main`, вливается в `main` и `develop` |

## Коммиты

[Conventional Commits](https://www.conventionalcommits.org/ru/):

```
feat(import): поддержка Threads
fix(parser): «1 1/2 стакана» разбиралось как 1
docs: инструкция по подписи
chore(deps): обновить Compose BOM
```

Типы: `feat`, `fix`, `perf`, `refactor`, `test`, `docs`, `build`, `ci`, `chore`.

## Pull request

1. Ответвись от `develop`.
2. Пиши тесты для логики в `core/` — она на чистом Kotlin и тестируется без эмулятора.
3. `./gradlew testDebugUnitTest assembleDebug` должен проходить локально.
4. Добавь строку в `CHANGELOG.md` в раздел `[Unreleased]`.
5. Открой PR в `develop` и заполни шаблон. CI обязан быть зелёным.

## Стиль кода

- Официальный стиль Kotlin (`kotlin.code.style=official`), отступ 4 пробела, см. `.editorconfig`.
- Строки интерфейса пишутся на русском.
- Логика разбора живёт в `core/` и не зависит от Android.

## Выпуск релиза (для мейнтейнера)

1. PR `develop → main`, merge.
2. В `CHANGELOG.md` переименуй `[Unreleased]` в `[X.Y.Z] - ГГГГ-ММ-ДД` и добавь новый пустой `[Unreleased]`.
3. `git tag vX.Y.Z && git push origin vX.Y.Z`. Workflow `Release` соберёт подписанный APK и опубликует релиз с заметками из CHANGELOG.

## Сообщить о проблеме

Используй [шаблоны issue](https://github.com/MrIsrak/RecepyBook/issues/new/choose). Для ошибок импорта обязательно прикладывай ссылку на пост.
