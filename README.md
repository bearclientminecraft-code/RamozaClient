# RamozaClient

Premium ClickGUI (только визуальная часть) для Minecraft **1.21.11**, Fabric, Java 21, Yarn.
Здесь нет игровых модулей, боевых функций и обхода античита: это GUI-основа, в которую потом подключаются ваши модули.

## Статус (честно)

| Что | Состояние |
|---|---|
| Слой интерфейса (всё в `gui/`, кроме 3 адаптеров) | компилируется javac без предупреждений, прогнан headless: 36 проверок + фаззинг 6000 событий |
| Адаптеры Minecraft (`McRenderer`, `RamozaScreen`, `RamozaClientMod`) | **не собирались против настоящего Minecraft**, проверены только на заглушках API |
| `./gradlew build` | собирается: проверено локально (loom 1.19.4, Gradle 9.9.2, JDK 25, class-файлы Java 21) |
| Запуск в игре | **не проверялся** |

Если CI на GitHub упадёт, пришлите лог: скорее всего это версия в `gradle.properties` или мелкая правка API в одном из трёх адаптеров.

## Как собрать через GitHub

1. Распакуйте архив, получите папку `RamozaClient`.
2. Создайте пустой репозиторий на GitHub и выполните в папке:
   ```
   git init
   git add .
   git commit -m "RamozaClient base"
   git branch -M main
   git remote add origin https://github.com/<вы>/RamozaClient.git
   git push -u origin main
   ```
3. Вкладка **Actions** -> workflow `build` -> артефакт `RamozaClient` (jar). Нужен Fabric API в папке mods.

Локально: `./gradlew build` (wrapper сам скачает Gradle 9.9.2; JDK 25 должен быть в `JAVA_HOME`, class-файлы собираются под Java 21) или открыть папку в IntelliJ IDEA.
Если Gradle скажет, что версия не найдена, возьмите актуальные для 1.21.11 с https://fabricmc.net/develop/ и поправьте `gradle.properties`.

## Управление

| Клавиша | Действие |
|---|---|
| Right Shift | открыть / закрыть (переназначается в Controls) |
| Esc | закрыть (с анимацией); если идёт назначение клавиши или открыто окно настроек, сначала отменяет их |
| Up / Down | предыдущий / следующий элемент, список сам прокручивается |
| Left / Right | меньше / больше (слайдер, список, переключатель); на модуле: свернуть / развернуть настройки; иначе переход к соседней панели |
| Enter / Space | действие над выбранным элементом |
| Ctrl+F или просто буквы | поиск; Delete очищает |
| ЛКМ / ПКМ по модулю | включить / раскрыть настройки |

**Self Destruct** в этом прототипе только скрывает GUI: он не откроется, пока вы не нажмёте клавишу
**Restore GUI** (по умолчанию `End`, видна в Controls). Ничего не прячется от игры, сервера или системы. Состояние не сохраняется между запусками.

## Структура

```
dev/ramoza/client
  RamozaClientMod        точка входа, клавиши
  core/RamozaCore        общее состояние
  config/                GuiSettings, ConfigManager (config/ramozaclient.properties)
  module/  setting/      ClientModule, Category, настройки модулей
  gui/
    GuiRoot              раскладка, анимация открытия/закрытия, ввод, навигация
    RamozaScreen         адаптер Screen (единственное место с вводом Minecraft)
    theme/               Palette (все цвета), ThemeManager, 6 тем, Dark/Light/Glass
    anim/                Easing, Animation, SmoothFloat (всё на delta time)
    render/              Renderer2D, Gfx (скруглённые углы, тени), McRenderer, Icons
    component/           CategoryPanel, ModuleRow, Toggle, Slider, Dropdown, KeybindButton,
                         ThemeCard, SettingsPanel, Tooltip, SearchBar, BottomBar, ...
```

## Как добавить свой модуль

```java
public final class MyModule extends ClientModule {
    public MyModule() {
        super("My Module", "Что делает", Category.PLAYER);
        add(new BoolSetting("Option", "Подсказка", true));
        add(new NumberSetting("Range", "Подсказка", 3, 1, 6, 0.5));
    }
    @Override protected void onEnable() { /* ... */ }
}
```
Зарегистрируйте в `ModuleManager.registerDefaults()` вместо заглушек `Example N`. GUI подхватит модуль, настройки и сохранение сам.

## Известные ограничения

- **Версия.** В техзадании была 1.21.1, проект собран под **1.21.11**. С 1.21.6 и 1.21.9 изменились API рендера и ввода, поэтому под 1.21.1 понадобится переписать только `McRenderer`, `RamozaScreen` и регистрацию клавиши.
- **Blur** это штатный blur меню Minecraft: он включается мгновенно и не анимируется (его сила задаётся в настройках видео). Liquid Glass без blur остаётся стеклом за счёт полупрозрачных слоёв.
- **Шрифт** стандартный шрифт Minecraft. Кастомный TTF потребует ресурс-пака со шрифтом.
- Скругления сглаживаются построчно: чётче всего при `Scale = 1.0`. В очень маленьком окне GUI автоматически уменьшается, и края становятся грубее.
- Около 5000 прямоугольников на кадр (в основном скругления). Если упадёт FPS, уменьшите `Rounded Corners` или отключите Blur.
- Панели нельзя перетаскивать; нет вставки из буфера в поиск.
- Превью в `docs/preview/` получены программным рендером: шрифт и blur в игре будут отличаться.
