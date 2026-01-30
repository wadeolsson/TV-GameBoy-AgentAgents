package tvgameboy.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tvgameboy.games.template.TemplateGame;
import tvgameboy.games.mole.MoleGame;

public final class GameRegistry {
    private static final List<GameEntry> GAMES;

    static {
        List<GameEntry> games = new ArrayList<>();

        // Put the Mole game in the top-middle selector slot by adding it before the template entry
        games.add(new GameEntry(
            "mole-game",
            "Mole Mole",
            MoleGame::new
        ));

        games.add(new GameEntry(
            "template-game",
            "Template Game",
            TemplateGame::new
        ));

        GAMES = Collections.unmodifiableList(games);
    }

    private GameRegistry() {
    }

    public static List<GameEntry> getGames() {
        return GAMES;
    }
}
