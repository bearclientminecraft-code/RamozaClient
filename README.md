# RamozaClient

Premium ClickGUI (только визуальная часть) для Minecraft **1.21.11**, Fabric, Java 21, Yarn.
GUI-основа плюс модули категории Player: Auto Respawn, Fast Place, Free Cam, Key Pearl, No Jump Delay, No Push, Sprint, XP Macro. Категории без модулей (Render, Pot Cheats) в GUI не рисуются и появятся сами, как только в них зарегистрируют модуль.

## Статус (честно)

| Что | Состояние |
|---|---|
| Слой интерфейса (всё в `gui/`, кроме 3 адаптеров) | компилируется javac без предупреждений, прогнан headless: 36 проверок + фаззинг 6000 событий |
| Адаптеры Minecraft (`McRenderer`, `RamozaScreen`, `RamozaClientMod`) | **не собирались против настоящего Minecraft**, проверены только на заглушках API |
| Модули Player (`module/player/`, `mixin/`) | компилируется против настоящего Minecraft 1.21.11; **работа модулей и Mixin в игре не проверялась** |
| `./gradlew build` | собирается: проверено локально (loom 1.19.4, Gradle 9.9.2, JDK 25, class-файлы Java 21) |
| Запуск в игре | **не проверялся** (headless-среда) |

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
3. Вкладка **Actions** -> workflow `build` -> артефакт `RamozaClient-1.21.11` (jar). Нужен Fabric API в папке mods.

Готовый jar без сборки: `dist/RamozaClient-1.0.0.jar` (Fabric 1.21.11).

Локально: `./gradlew build` (loom 1.19.4 требует JDK 25 для запуска Gradle, классы компилируются под Java 21) или открыть папку в IntelliJ IDEA.
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
Зарегистрируйте в `ModuleManager.registerDefaults()` (заглушек `Example N` больше нет). GUI подхватит модуль, настройки и сохранение сам.

## Модули Player

| Модуль | Что делает | Как реализован |
|---|---|---|
| Auto Respawn | сам нажимает «Возродиться» на экране смерти; настройка `Delay` (тики); в хардкоре ничего не делает | `DeathScreen` + `requestRespawn()` |
| No Jump Delay | убирает паузу между прыжками при зажатом пробеле | Mixin-аксессор `LivingEntity.jumpingCooldown` |
| No Push | игрока не толкают сущности и жидкости (настройки `Entities`, `Liquids`) | Mixin `Entity.pushAwayFrom`, `PlayerEntity.isPushedByFluids` |
| Sprint | спринт автоматически при зажатой клавише вперёд | нажимает клавишу спринта, остальные условия проверяет сама игра |
| Fast Place | быстрая установка при зажатой ПКМ; `Delay (ms)` 0-200, `Random Delay`, `Blocks Only` | аксессор `MinecraftClient.itemUseCooldown` |
| Free Cam | камера отделяется от игрока, летает WASD/Space/Shift; `Speed` | `CameraMixin` (позиция камеры), `Entity.changeLookDirection` (мышь), клавиши движения сбрасываются |
| Key Pearl | по клавише: слот Pearl, бросок, слот Totem; `Pearl Slot`, `Totem Slot` (1-9), `Delay (ms)`, `Random Delay` | опрос GLFW, `interactItem`; без жемчуга в слоте ничего не делает |
| XP Macro | спамит бутылки опыта из любой руки, хотбара или инвентаря; `Activation` (Always / Hold Key), `Key`, `Search Inventory`, `Delay (ms)`, `Random Delay` | `interactItem`, для инвентаря `clickSlot(SWAP)` туда и обратно |

| Auto Mace | в падении бьёт цель под прицелом булавой: слот булавы, удар, возврат слота; `Min Fall Distance` | `attackEntity` + `swingHand`, один удар за падение |
| Mace Swap | при вашем клике по сущности в падении переключает слот на булаву до пакета атаки и возвращает после задержки | `MinecraftClientMixin` на `doAttack` |
| Stun Slam | клик по цели со щитом в падении: топор, пауза, булава, пауза, возврат слота | тот же хук; нужны топор и булава в хотбаре |
| Lunge Swap | по клавише: слот копья, обычный левый клик (рывок Lunge), после паузы слот `Hand Slot` | `MinecraftClientAccessor.doAttack` (invoker) |
| Pearl Catch | по клавише: пёрл, пауза, заряд ветра (по умолчанию взгляд вниз, питч потом возвращается), пауза, возврат слота | `Pearl Slot`, `WindCharge Slot` |
| Elytra Swap | нагрудник ↔ элитры без инвентаря тремя кликами `clickSlot`; `Mode`: Key или Auto (элитры при падении, нагрудник при приземлении) | слот брони 6 |

У всех шести: `Activate Key`, `Delay (ms)`, `Random Delay`. Для Auto Mace, Mace Swap и Stun Slam Activate Key работает как «взведено, пока зажата» (не назначена = всегда взведено). Для Lunge Swap, Pearl Catch и Elytra Swap это нажатие-триггер. Шаги идут через `MacroRunner` (по тикам, без `Thread.sleep`), и после каждого шага задержка выбирается заново.

**Random Delay** (везде, где есть задержка, включая Auto Respawn): на каждом новом действии задержка заново выбирается как `Delay ± Random Delay` (например 25 ± 2 = 23..27 мс), не меньше 0. Модули тикают 20 раз в секунду, поэтому задержка проверяется раз в тик (~50 мс): реальное ожидание округляется вверх до тика. Delay у Auto Respawn по-прежнему в тиках, Random Delay у него в мс.

Модули в каждой категории выводятся по алфавиту (без учёта регистра, числа по значению), порядок задаётся в `ModuleManager.in()`.
Тик модулей: `ModuleManager.tick()` из `START_CLIENT_TICK`.

## Известные ограничения

- **Новые модули Player не собирались против настоящего Minecraft**: проверены javac на заглушках API. Под сомнением имена `PlayerInventory.getSelectedSlot/setSelectedSlot`, `MinecraftClient.itemUseCooldown`, `Entity.changeLookDirection`, `Camera.update / isThirdPerson / setPos / setRotation`. Если сборка или запуск упадёт, пришлите лог.
- **Макросы Mace/Spear/Pearl/Elytra** тоже не собирались против настоящего Minecraft. Под сомнением `MinecraftClient.doAttack` (возвращает boolean), `Entity.fallDistance`, `PlayerInventory.setSelectedSlot`. Что именно делает Lunge на копье в 1.21.11, мод не знает: он просто делает обычный левый клик, остальное решает игра.
- **Free Cam** не блокирует клики: атака и использование предметов идут из позиции игрока, а не камеры.
- **XP Macro** не наводится: смотрите туда, куда должны упасть бутылки.
- **Key Pearl** не проверяет, что в Totem Slot лежит тотем: просто переключается на этот слот.

- **No Push** не отменяет выталкивание из блоков и скорость, присланную сервером (отдача, velocity-пакеты).

- **Версия.** В техзадании была 1.21.1, проект собран под **1.21.11**. С 1.21.6 и 1.21.9 изменились API рендера и ввода, поэтому под 1.21.1 понадобится переписать только `McRenderer`, `RamozaScreen` и регистрацию клавиши.
- **Blur** это штатный blur меню Minecraft: он включается мгновенно и не анимируется (его сила задаётся в настройках видео). Liquid Glass без blur остаётся стеклом за счёт полупрозрачных слоёв.
- **Шрифт** стандартный шрифт Minecraft. Кастомный TTF потребует ресурс-пака со шрифтом.
- Скругления сглаживаются построчно: чётче всего при `Scale = 1.0`. В очень маленьком окне GUI автоматически уменьшается, и края становятся грубее.
- Около 5000 прямоугольников на кадр (в основном скругления). Если упадёт FPS, уменьшите `Corner Rounding` или отключите Blur.
- Панели нельзя перетаскивать; нет вставки из буфера в поиск.
- Превью в `docs/preview/` получены программным рендером: шрифт и blur в игре будут отличаться.
