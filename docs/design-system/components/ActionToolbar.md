# ActionToolbar

Плавающая тёмная капсула действий внизу экрана: «Move», корзина и «•••» для раскрытой задачи, набор иконок для выбранных.

## Когда использовать
Пока раскрыт редактор задачи (Move / Delete / More) или включён режим выбора (дата, перенос, удаление, ещё).

## Размеры
- Высота 50 dp (`size-toolbar`), радиус `ThingsTheme.shapes.toolbarShape` (`radius-toolbar`, 25 dp), в 24 dp от низа; фон `ThingsTheme.colors.overlaySurface` (`dialog-bg`), иконки и текст цветом `overlay-content`, подпись «Move» — `dialogButton` в Bold.
- Меню «•••» открывается на `ThingsTheme.colors.overlaySurface` (`dialog-bg`).
- Иконки 24 dp; нажатие отзывается вибрацией `CLOCK_TICK`.

## Правила
- Панель появляется вместе с раскрытием и уходит при сворачивании; клавиатура её перекрывает.
