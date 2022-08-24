package com.github.phantazmnetwork.zombies.game.scoreboard.sidebar.section;

import com.github.phantazmnetwork.zombies.game.scoreboard.sidebar.lineupdater.SidebarLineUpdater;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SingleSidebarSection implements SidebarSection {

    private final SidebarLineUpdater lineUpdater;

    public SingleSidebarSection(@NotNull SidebarLineUpdater lineUpdater) {
        this.lineUpdater = Objects.requireNonNull(lineUpdater, "lineUpdater");
    }

    @Override
    public void invalidateCache() {
        lineUpdater.invalidateCache();
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public @NotNull List<Optional<Component>> update(long time) {
        return Collections.singletonList(lineUpdater.tick(time));
    }
}
