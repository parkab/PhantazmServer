package org.phantazm.zombies.scene;

import com.github.steanky.vector.Vec3I;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.network.player.PlayerSocketConnection;
import org.jetbrains.annotations.NotNull;
import org.phantazm.commons.TickTaskScheduler;
import org.phantazm.core.game.scene.InstanceScene;
import org.phantazm.core.game.scene.RouteResult;
import org.phantazm.core.game.scene.fallback.SceneFallback;
import org.phantazm.core.player.PlayerView;
import org.phantazm.zombies.map.MapSettingsInfo;
import org.phantazm.zombies.map.ZombiesMap;
import org.phantazm.zombies.player.ZombiesPlayer;
import org.phantazm.zombies.player.state.ZombiesPlayerStateKeys;
import org.phantazm.zombies.player.state.context.DeadPlayerStateContext;
import org.phantazm.zombies.player.state.context.NoContext;
import org.phantazm.zombies.stage.Stage;
import org.phantazm.zombies.stage.StageTransition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ZombiesScene extends InstanceScene<ZombiesJoinRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZombiesScene.class);
    private static final CompletableFuture<?>[] EMPTY_COMPLETABLE_FUTURE_ARRAY = new CompletableFuture[0];
    private final ConnectionManager connectionManager;
    private final ZombiesMap map;
    private final Map<UUID, ZombiesPlayer> zombiesPlayers;
    private final MapSettingsInfo mapSettingsInfo;
    private final StageTransition stageTransition;
    private final LeaveHandler leaveHandler;
    private final Function<? super PlayerView, ? extends ZombiesPlayer> playerCreator;
    private final TickTaskScheduler taskScheduler;

    private boolean joinable = true;

    public ZombiesScene(@NotNull UUID uuid, @NotNull ConnectionManager connectionManager, @NotNull ZombiesMap map,
            @NotNull Map<UUID, PlayerView> players, @NotNull Map<UUID, ZombiesPlayer> zombiesPlayers,
            @NotNull Instance instance, @NotNull SceneFallback fallback, @NotNull MapSettingsInfo mapSettingsInfo,
            @NotNull StageTransition stageTransition, @NotNull LeaveHandler leaveHandler,
            @NotNull Function<? super PlayerView, ? extends ZombiesPlayer> playerCreator,
            @NotNull TickTaskScheduler taskScheduler) {
        super(uuid, instance, players, fallback);
        this.connectionManager = Objects.requireNonNull(connectionManager, "connectionManager");
        this.map = Objects.requireNonNull(map, "map");
        this.zombiesPlayers = Objects.requireNonNull(zombiesPlayers, "zombiesPlayers");
        this.mapSettingsInfo = Objects.requireNonNull(mapSettingsInfo, "mapSettingsInfo");
        this.stageTransition = Objects.requireNonNull(stageTransition, "stageTransition");
        this.leaveHandler = Objects.requireNonNull(leaveHandler, "leaveHandler");
        this.playerCreator = Objects.requireNonNull(playerCreator, "playerCreator");
        this.taskScheduler = Objects.requireNonNull(taskScheduler, "taskScheduler");
    }

    public @NotNull Map<UUID, ZombiesPlayer> getZombiesPlayers() {
        return Map.copyOf(zombiesPlayers);
    }

    public @NotNull MapSettingsInfo getMapSettingsInfo() {
        return mapSettingsInfo;
    }

    public Stage getCurrentStage() {
        return stageTransition.getCurrentStage();
    }

    public boolean isComplete() {
        return stageTransition.isComplete();
    }

    public @NotNull ZombiesMap getMap() {
        return map;
    }

    public @NotNull StageTransition getStageTransition() {
        return stageTransition;
    }

    @Override
    public @NotNull RouteResult join(@NotNull ZombiesJoinRequest joinRequest) {
        Collection<ZombiesPlayer> oldPlayers = new ArrayList<>(joinRequest.getPlayers().size());
        Collection<PlayerView> newPlayers = new ArrayList<>(joinRequest.getPlayers().size());
        for (PlayerView player : joinRequest.getPlayers()) {
            ZombiesPlayer zombiesPlayer = zombiesPlayers.get(player.getUUID());
            if (zombiesPlayer != null) {
                if (zombiesPlayer.hasQuit()) {
                    oldPlayers.add(zombiesPlayer);
                }
            }
            else {
                newPlayers.add(player);
            }
        }

        Stage stage = getCurrentStage();
        if (stage == null) {
            return new RouteResult(false, Component.text("The game is not currently running.", NamedTextColor.RED));
        }
        if (stage.hasPermanentPlayers() && !newPlayers.isEmpty()) {
            return new RouteResult(false, Component.text("The game is not accepting new players.", NamedTextColor.RED));
        }

        if (zombiesPlayers.size() + newPlayers.size() > mapSettingsInfo.maxPlayers()) {
            return new RouteResult(false, Component.text("Too many players!", NamedTextColor.RED));
        }

        RouteResult protocolResult = checkWithinProtocolVersionBounds(newPlayers);
        if (protocolResult != null) {
            return protocolResult;
        }

        Vec3I spawn = mapSettingsInfo.origin().add(mapSettingsInfo.spawn());
        Pos pos = new Pos(spawn.x(), spawn.y(), spawn.z(), mapSettingsInfo.yaw(), mapSettingsInfo.pitch());
        List<PlayerView> teleportedViews = new ArrayList<>(oldPlayers.size() + newPlayers.size());
        List<Player> teleportedPlayers = new ArrayList<>(oldPlayers.size() + newPlayers.size());
        List<CompletableFuture<?>> futures = new ArrayList<>(oldPlayers.size() + newPlayers.size());
        List<Runnable> runnables = new ArrayList<>(oldPlayers.size() + newPlayers.size());
        for (ZombiesPlayer zombiesPlayer : oldPlayers) {
            zombiesPlayer.getPlayer().ifPresent(player -> {
                teleportedViews.add(zombiesPlayer.module().getPlayerView());
                teleportedPlayers.add(player);
                if (player.getInstance() == instance) {
                    futures.add(player.teleport(pos));
                }
                else {
                    futures.add(player.setInstance(instance, pos));
                }
                runnables.add(() -> {
                    zombiesPlayer.setState(ZombiesPlayerStateKeys.DEAD, DeadPlayerStateContext.rejoin());
                });
            });
        }
        for (PlayerView view : newPlayers) {
            view.getPlayer().ifPresent(player -> {
                teleportedViews.add(view);
                teleportedPlayers.add(player);
                if (player.getInstance() == instance) {
                    futures.add(player.teleport(pos));
                }
                else {
                    futures.add(player.setInstance(instance, pos));
                }
                runnables.add(() -> {
                    ZombiesPlayer zombiesPlayer = playerCreator.apply(view);
                    zombiesPlayer.start();
                    zombiesPlayer.setState(ZombiesPlayerStateKeys.ALIVE, NoContext.INSTANCE);
                    zombiesPlayers.put(view.getUUID(), zombiesPlayer);
                });
            });
        }

        CompletableFuture.allOf(futures.toArray(EMPTY_COMPLETABLE_FUTURE_ARRAY)).whenComplete((ignored, error) -> {
            for (int i = 0; i < futures.size(); i++) {
                PlayerView view = teleportedViews.get(i);
                Player teleportedPlayer = teleportedPlayers.get(i);
                CompletableFuture<?> future = futures.get(i);
                Runnable runnable = runnables.get(i);

                if (future.isCompletedExceptionally()) {
                    future.whenComplete((ignored1, throwable) -> {
                        LOGGER.warn("Failed to send {} to an instance", teleportedPlayer.getUuid(), throwable);
                    });
                    continue;
                }

                for (Player otherPlayer : connectionManager.getOnlinePlayers()) {
                    if (otherPlayer.getInstance() != instance) {
                        otherPlayer.removeViewer(teleportedPlayer);
                        teleportedPlayer.sendPacket(otherPlayer.getRemovePlayerToList());
                        teleportedPlayer.removeViewer(otherPlayer);
                        otherPlayer.sendPacket(teleportedPlayer.getRemovePlayerToList());
                    }
                    else {
                        teleportedPlayer.sendPacket(otherPlayer.getAddPlayerToList());
                        otherPlayer.addViewer(teleportedPlayer);
                        otherPlayer.sendPacket(teleportedPlayer.getAddPlayerToList());
                        teleportedPlayer.addViewer(otherPlayer);
                    }
                }

                runnable.run();

                players.put(view.getUUID(), view);
                stage.onJoin(zombiesPlayers.get(teleportedPlayer.getUuid()));
            }
        }).join();

        return RouteResult.SUCCESSFUL;
    }

    @Override
    public @NotNull RouteResult leave(@NotNull Iterable<UUID> leavers) {
        for (UUID uuid : leavers) {
            ZombiesPlayer zombiesPlayer = zombiesPlayers.get(uuid);
            if (zombiesPlayer != null) {
                zombiesPlayer.end();
            }
        }

        return leaveHandler.leave(leavers);
    }

    @Override
    public boolean isJoinable() {
        return joinable;
    }

    @Override
    public void setJoinable(boolean joinable) {
        this.joinable = joinable;
    }

    private RouteResult checkWithinProtocolVersionBounds(@NotNull Collection<PlayerView> newPlayers) {
        for (PlayerView playerView : newPlayers) {
            Optional<Player> player = playerView.getPlayer();
            if (player.isEmpty()) {
                continue;
            }

            boolean hasMinimum = mapSettingsInfo.minimumProtocolVersion() >= 0;
            boolean hasMaximum = mapSettingsInfo.maximumProtocolVersion() >= 0;

            int protocolVersion = getActualProtocolVersion(player.get().getPlayerConnection());

            if (hasMinimum && protocolVersion < mapSettingsInfo.minimumProtocolVersion()) {
                return new RouteResult(false,
                        Component.text("A player's Minecraft version is too old!", NamedTextColor.RED));
            }
            if (hasMaximum && protocolVersion > mapSettingsInfo.maximumProtocolVersion()) {
                return new RouteResult(false,
                        Component.text("A player's Minecraft version is too new!", NamedTextColor.RED));
            }
        }

        return null;
    }

    @SuppressWarnings("UnstableApiUsage")
    private int getActualProtocolVersion(PlayerConnection playerConnection) {
        int protocolVersion = MinecraftServer.PROTOCOL_VERSION;
        if (!(playerConnection instanceof PlayerSocketConnection socketConnection)) {
            return protocolVersion;
        }

        GameProfile gameProfile = socketConnection.gameProfile();
        if (gameProfile == null) {
            return protocolVersion;
        }

        for (GameProfile.Property property : gameProfile.properties()) {
            if (property.name().equals("protocolVersion")) {
                try {
                    protocolVersion = Integer.parseInt(property.value());
                }
                catch (NumberFormatException ignored) {
                }
                break;
            }
        }

        return protocolVersion;
    }

    @Override
    public int getJoinWeight(@NotNull ZombiesJoinRequest request) {
        Stage stage = getCurrentStage();
        if (stage == null || stage.hasPermanentPlayers()) {
            return Integer.MIN_VALUE;
        }

        return 0;
    }

    @Override
    public void shutdown() {
        taskScheduler.end();
        for (ZombiesPlayer zombiesPlayer : zombiesPlayers.values()) {
            zombiesPlayer.end();
        }
        super.shutdown();
    }

    @Override
    public void tick(long time) {
        super.tick(time);
        if (!isShutdown() && stageTransition.isComplete()) {
            shutdown();
            return;
        }

        map.tick(time);
        stageTransition.tick(time);
        taskScheduler.tick(time);
        for (ZombiesPlayer zombiesPlayer : zombiesPlayers.values()) {
            zombiesPlayer.tick(time);
        }
    }
}
