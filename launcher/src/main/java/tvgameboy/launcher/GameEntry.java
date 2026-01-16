package tvgameboy.launcher;

import java.util.function.Supplier;
import javax.swing.Icon;
import tvgameboy.shared.Game;

public final class GameEntry {
    private final String id;
    private final String displayName;
    private final Icon icon;
    private final Supplier<Game> factory;

    public GameEntry(String id, String displayName, Supplier<Game> factory) {
        this(id, displayName, null, factory);
    }

    public GameEntry(String id, String displayName, Icon icon, Supplier<Game> factory) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.factory = factory;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Icon getIcon() {
        return icon;
    }

    public Supplier<Game> getFactory() {
        return factory;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
