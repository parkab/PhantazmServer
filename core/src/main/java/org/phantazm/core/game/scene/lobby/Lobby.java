package org.phantazm.core.game.scene.lobby;

import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.phantazm.core.config.InstanceConfig;
import org.phantazm.core.game.scene.InstanceScene;
import org.phantazm.core.game.scene.RouteResult;
import org.phantazm.core.game.scene.fallback.SceneFallback;
import org.phantazm.core.npc.NPCHandler;
import org.phantazm.core.player.PlayerView;

import java.util.*;

/**
 * Represents a lobby. Most basic scene which contains {@link Player}s.
 */
public class Lobby extends InstanceScene<LobbyJoinRequest> {
    private final InstanceConfig instanceConfig;
    private final Map<UUID, PlayerView> players;
    private final NPCHandler npcHandler;

    private boolean joinable = true;

    /**
     * Creates a lobby.
     *
     * @param instance       The {@link Instance} that the lobby's players are sent to
     * @param instanceConfig The {@link InstanceConfig} used for the lobby's {@link Instance}
     * @param fallback       A fallback for the lobby
     */
    public Lobby(@NotNull UUID uuid, @NotNull Instance instance, @NotNull InstanceConfig instanceConfig,
            @NotNull SceneFallback fallback, @NotNull NPCHandler npcHandler) {
        super(uuid, instance, fallback);
        this.instanceConfig = Objects.requireNonNull(instanceConfig, "instanceConfig");
        this.players = new HashMap<>();
        this.npcHandler = Objects.requireNonNull(npcHandler, "npcHandler");

        this.npcHandler.spawnAll();
    }

    @Override
    public @NotNull RouteResult join(@NotNull LobbyJoinRequest joinRequest) {
        if (isShutdown()) {
            return new RouteResult(false, Component.text("Lobby is shutdown."));
        }
        if (!isJoinable()) {
            return new RouteResult(false, Component.text("Lobby is not joinable."));
        }

        Collection<PlayerView> playerViews = joinRequest.getPlayers();
        Collection<Pair<PlayerView, Player>> joiners = new ArrayList<>(playerViews.size());
        for (PlayerView playerView : playerViews) {
            playerView.getPlayer().ifPresent(player -> {
                if (player.getInstance() != instance) {
                    joiners.add(Pair.of(playerView, player));
                }
            });
        }

        if (joiners.isEmpty()) {
            return new RouteResult(false, Component.text("Everybody is already in the lobby."));
        }

        for (Pair<PlayerView, Player> player : joiners) {
            players.put(player.first().getUUID(), player.first());
        }

        joinRequest.handleJoin(instance, instanceConfig);
        return RouteResult.SUCCESSFUL;
    }

    @Override
    public @NotNull RouteResult leave(@NotNull Iterable<UUID> leavers) {
        boolean anyInside = false;
        for (UUID uuid : leavers) {
            if (players.containsKey(uuid)) {
                anyInside = true;
                break;
            }
        }

        if (!anyInside) {
            return new RouteResult(false, Component.text("None of the players are in the lobby."));
        }

        for (UUID uuid : leavers) {
            players.remove(uuid);
        }

        return RouteResult.SUCCESSFUL;
    }

    @Override
    public @UnmodifiableView @NotNull Map<UUID, PlayerView> getPlayers() {
        return players;
    }

    @Override
    public boolean isJoinable() {
        return joinable;
    }

    @Override
    public void setJoinable(boolean joinable) {
        this.joinable = joinable;
    }

    public void cleanup() {
        this.npcHandler.end();
    }

    @Override
    public void tick(long time) {
        this.npcHandler.tick(time);
    }
}
