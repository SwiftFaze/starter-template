package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.player.Player;
import javax.swing.JComponent;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A single live player's editable stats. Unlike ClassSandboxProvider (which is
 * stateless and creates a fresh model each time), this provider is stateful: it
 * holds a {@code Supplier<Player>} to always read the currently-running player, not a
 * stale direct reference. When GamePanel.resetState() creates a new player
 * object, the supplier returns the new object immediately, so edits stay
 * synchronized with whichever player is live in the game.
 */
public class PlayerSandboxProvider implements DevConsoleProvider {

    private static final String PLAYER_ID = "core:player";
    private final Supplier<Player> playerSupplier;
    private final PlayerSnapshotter snapshotter;

    public PlayerSandboxProvider(Supplier<Player> playerSupplier) {
        this.playerSupplier = playerSupplier;
        this.snapshotter = new PlayerSnapshotter(playerSupplier);
    }

    @Override
    public List<DevConsoleEntry> entries() {
        return List.of(new DevConsoleEntry("core", PLAYER_ID, "Player", "Player"));
    }

    @Override
    public JComponent createPanel(String id) {
        return new PlayerDetailPanel(playerSupplier.get());
    }

    @Override
    public Optional<DevConsoleFieldMutator> fieldMutator(String id) {
        return Optional.of(new PlayerFieldMutator(playerSupplier));
    }

    @Override
    public Optional<DevConsoleSnapshotter> snapshotter(String id) {
        return Optional.of(snapshotter);
    }
}
