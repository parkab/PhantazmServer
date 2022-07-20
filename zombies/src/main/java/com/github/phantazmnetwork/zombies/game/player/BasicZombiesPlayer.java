package com.github.phantazmnetwork.zombies.game.player;

import com.github.phantazmnetwork.core.inventory.InventoryProfileSwitcher;
import com.github.phantazmnetwork.core.player.PlayerView;
import com.github.phantazmnetwork.zombies.equipment.Equipment;
import com.github.phantazmnetwork.zombies.game.coin.PlayerCoins;
import com.github.phantazmnetwork.zombies.game.kill.PlayerKills;
import com.github.phantazmnetwork.zombies.game.player.state.PlayerStateSwitcher;
import net.minestom.server.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public class BasicZombiesPlayer implements ZombiesPlayer {

    private final PlayerView playerView;

    private final PlayerCoins coins;

    private final PlayerKills kills;

    private final InventoryProfileSwitcher profileSwitcher;

    private final PlayerStateSwitcher stateSwitcher;

    private boolean crouching = false;

    private boolean reviving = false;

    private boolean inGame = false;

    public BasicZombiesPlayer(@NotNull PlayerView playerView, @NotNull PlayerCoins coins, @NotNull PlayerKills kills,
                              @NotNull InventoryProfileSwitcher profileSwitcher,
                              @NotNull PlayerStateSwitcher stateSwitcher) {
        this.playerView = Objects.requireNonNull(playerView, "playerView");
        this.coins = Objects.requireNonNull(coins, "coins");
        this.kills = Objects.requireNonNull(kills, "kills");
        this.profileSwitcher = Objects.requireNonNull(profileSwitcher, "profileSwitcher");
        this.stateSwitcher = Objects.requireNonNull(stateSwitcher, "stateSwitcher");
        playerView.getPlayer().ifPresent(player -> {
            this.crouching = player.getPose() == Entity.Pose.SNEAKING;
        });
    }

    @Override
    public void tick(long time) {
    }

    @Override
    public boolean isCrouching() {
        return crouching;
    }

    @Override
    public void setCrouching(boolean crouching) {
        this.crouching = crouching;
    }

    @Override
    public void setReviving(boolean reviving) {
        this.reviving = reviving;
    }

    @Override
    public boolean isReviving() {
        return reviving;
    }

    @Override
    public boolean isInGame() {
        return inGame;
    }

    @Override
    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }

    @Override
    public @NotNull PlayerCoins getCoins() {
        return coins;
    }

    @Override
    public @NotNull PlayerKills getKills() {
        return kills;
    }

    @Override
    public @NotNull @UnmodifiableView Collection<Equipment> getEquipment() {
        return Collections.emptyList();
    }

    @Override
    public @NotNull InventoryProfileSwitcher getProfileSwitcher() {
        return profileSwitcher;
    }

    @Override
    public @NotNull PlayerStateSwitcher getStateSwitcher() {
        return stateSwitcher;
    }

    @Override
    public @NotNull PlayerView getPlayerView() {
        return playerView;
    }
}
