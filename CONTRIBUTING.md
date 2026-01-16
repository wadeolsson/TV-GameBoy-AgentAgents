# Contributing a Game Module

The goal is to keep each game in its own Gradle module so students can work in parallel.

## Create a new game module
1) Copy `games/template-game` to `games/game-your-name`.
2) Update the package name and class name inside your new module.
3) Add the module to `settings.gradle`.
4) Add the module as a dependency in `launcher/build.gradle`.
5) Register the game in `launcher/src/main/java/tvgameboy/launcher/GameRegistry.java`.

## Package conventions
- Use `tvgameboy.games.<your_game>` as the package root.
- Implement the `tvgameboy.shared.Game` interface and return a Swing view from `getView(Runnable)`.

## Registry entry example
Add a `GameEntry` like this:

```
games.add(new GameEntry(
        "your-game-id",
        "Your Game Name",
        YourGameClass::new
));
```
