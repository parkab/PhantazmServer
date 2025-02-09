package org.phantazm.zombies.spawn;

import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.phantazm.mob.MobModel;
import org.phantazm.mob.PhantazmMob;
import org.phantazm.zombies.map.SpawnInfo;
import org.phantazm.zombies.map.Spawnpoint;
import org.phantazm.zombies.player.ZombiesPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

public class BasicSpawnDistributor implements SpawnDistributor {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicSpawnDistributor.class);

    private final Function<? super Key, ? extends MobModel> modelFunction;
    private final Random random;

    private final Collection<? extends ZombiesPlayer> zombiesPlayers;

    private final Team mobNoPushTeam;

    public BasicSpawnDistributor(@NotNull Function<? super Key, ? extends MobModel> modelFunction,
            @NotNull Random random, @NotNull Collection<? extends ZombiesPlayer> zombiesPlayers,
            @Nullable Team mobNoPushTeam) {
        this.modelFunction = Objects.requireNonNull(modelFunction, "modelFunction");
        this.random = Objects.requireNonNull(random, "random");
        this.zombiesPlayers = Objects.requireNonNull(zombiesPlayers, "zombiesPlayers");
        this.mobNoPushTeam = mobNoPushTeam;
    }

    @Override
    public @NotNull List<PhantazmMob> distributeSpawns(@NotNull List<? extends Spawnpoint> spawnpoints,
            @NotNull Collection<? extends SpawnInfo> spawns) {
        if (spawnpoints.isEmpty()) {
            return new ArrayList<>(0);
        }

        List<Pair<MobModel, Key>> spawnList = new ArrayList<>(spawns.size());
        for (SpawnInfo spawnInfo : spawns) {
            Key id = spawnInfo.id();
            MobModel model = modelFunction.apply(id);
            if (model == null) {
                LOGGER.warn("Found unrecognized mob type '{}'", id);
                continue;
            }

            for (int i = 0; i < spawnInfo.amount(); i++) {
                spawnList.add(Pair.of(model, spawnInfo.spawnType()));
            }
        }

        if (spawnList.isEmpty()) {
            LOGGER.warn("Received empty spawn list");
            return List.of();
        }

        List<Spawnpoint> sortedSpawnpoints = new ArrayList<>(spawnpoints.size());
        for (Spawnpoint spawnpoint : spawnpoints) {
            if (spawnpoint.canSpawnAny(zombiesPlayers)) {
                sortedSpawnpoints.add(spawnpoint);
            }
        }

        sortedSpawnpoints.sort((first, second) -> {
            double firstClosest = Double.POSITIVE_INFINITY;
            double secondClosest = Double.POSITIVE_INFINITY;
            for (ZombiesPlayer zombiesPlayer : zombiesPlayers) {
                Optional<Player> playerOptional = zombiesPlayer.getPlayer();
                if (playerOptional.isPresent()) {
                    Player player = playerOptional.get();

                    double firstDistance = player.getDistance(first.spawnPoint());
                    double secondDistance = player.getDistance(second.spawnPoint());

                    if (firstDistance < firstClosest) {
                        firstClosest = firstDistance;
                    }

                    if (secondDistance < secondClosest) {
                        secondClosest = secondDistance;
                    }
                }
            }

            return Double.compare(firstClosest, secondClosest);
        });

        Collections.shuffle(spawnList, random);

        List<PhantazmMob> spawnedMobs = new ArrayList<>(spawnList.size());
        int candidateIndex = 0;
        for (int i = spawnList.size() - 1; i >= 0; i--) {
            Pair<MobModel, Key> spawnEntry = spawnList.get(i);
            MobModel model = spawnEntry.first();
            Key spawnType = spawnEntry.second();

            boolean spawned = false;
            for (int j = 0; j < sortedSpawnpoints.size(); j++) {
                Spawnpoint candidate = sortedSpawnpoints.get(candidateIndex++);
                candidateIndex %= sortedSpawnpoints.size();

                if (candidate.canSpawn(model, spawnType, zombiesPlayers)) {
                    PhantazmMob mob = candidate.spawn(model);
                    if (mobNoPushTeam != null) {
                        mob.entity().setTeam(mobNoPushTeam);
                    }

                    spawnedMobs.add(mob);
                    spawned = true;
                    break;
                }
            }

            if (!spawned) {
                LOGGER.warn("Found no suitable spawnpoint for mob {} using spawn type {}", model.key(), spawnType);
            }
        }

        return spawnedMobs;
    }
}
