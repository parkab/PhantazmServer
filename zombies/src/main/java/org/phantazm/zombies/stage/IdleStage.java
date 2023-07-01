package org.phantazm.zombies.stage;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.phantazm.zombies.leaderboard.BestTimeLeaderboard;
import org.phantazm.zombies.player.ZombiesPlayer;
import org.phantazm.zombies.sidebar.SidebarUpdater;

import java.util.*;
import java.util.function.Function;

public class IdleStage implements Stage {

    private final Map<UUID, SidebarUpdater> sidebarUpdaters = new HashMap<>();

    private final Collection<? extends ZombiesPlayer> zombiesPlayers;

    private final Function<? super ZombiesPlayer, ? extends SidebarUpdater> sidebarUpdaterCreator;

    private final BestTimeLeaderboard timeLeaderboard;

    private final long revertTicks;

    private long emptyTicks;

    public IdleStage(@NotNull Collection<? extends ZombiesPlayer> zombiesPlayers,
            @NotNull Function<? super ZombiesPlayer, ? extends SidebarUpdater> sidebarUpdaterCreator,
            @NotNull BestTimeLeaderboard timeLeaderboard,
            long revertTicks) {
        this.zombiesPlayers = Objects.requireNonNull(zombiesPlayers, "zombiesPlayers");
        this.sidebarUpdaterCreator = Objects.requireNonNull(sidebarUpdaterCreator, "sidebarUpdaterCreator");
        this.timeLeaderboard = Objects.requireNonNull(timeLeaderboard, "timeLeaderboard");
        this.revertTicks = revertTicks;
    }

    @Override
    public boolean shouldContinue() {
        return !zombiesPlayers.isEmpty();
    }

    @Override
    public boolean shouldRevert() {
        return false;
    }

    @Override
    public boolean shouldAbort() {
        return emptyTicks >= revertTicks;
    }

    @Override
    public void onJoin(@NotNull ZombiesPlayer zombiesPlayer) {

    }

    @Override
    public void onLeave(@NotNull ZombiesPlayer zombiesPlayer) {
        SidebarUpdater updater = sidebarUpdaters.remove(zombiesPlayer.getUUID());
        if (updater != null) {
            updater.end();
        }
    }

    @Override
    public void start() {
        emptyTicks = 0L;
        timeLeaderboard.startIfNotActive();
    }

    @Override
    public void tick(long time) {
        if (zombiesPlayers.isEmpty()) {
            ++emptyTicks;
        }
        else {
            emptyTicks = 0L;

            for (ZombiesPlayer zombiesPlayer : zombiesPlayers) {
                if (!zombiesPlayer.hasQuit()) {
                    SidebarUpdater sidebarUpdater = sidebarUpdaters.computeIfAbsent(zombiesPlayer.getUUID(), unused -> {
                        return sidebarUpdaterCreator.apply(zombiesPlayer);
                    });
                    sidebarUpdater.tick(time);
                }
            }
        }
    }

    @Override
    public void end() {
        for (SidebarUpdater sidebarUpdater : sidebarUpdaters.values()) {
            sidebarUpdater.end();
        }
    }

    @Override
    public boolean hasPermanentPlayers() {
        return false;
    }

    @Override
    public boolean canRejoin() {
        return false;
    }

    @Override
    public @NotNull Key key() {
        return StageKeys.IDLE_STAGE;
    }
}
