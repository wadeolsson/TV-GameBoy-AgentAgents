package tvgameboy.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tvgameboy.games.template.MoleGame;

public final class GameRegistry {
    private static final List<GameEntry> GAMES;

    static {
        // Prepare six slots (3x2 grid). Place the TemplateGame at bottom-middle (index 4).
        List<GameEntry> games = new ArrayList<>(Collections.nCopies(6, null));

        games.set(4, new GameEntry(
            "mole-game",
            "Mole",
            MoleGame::new
        ));

        GAMES = Collections.unmodifiableList(games);
    }

    private GameRegistry() {
    }

    public static List<GameEntry> getGames() {
        return GAMES;
    }
}
