package com.github.phantazmnetwork.zombies.game;

import com.github.phantazmnetwork.mob.PhantazmMob;
import com.github.phantazmnetwork.zombies.map.SpawnInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public interface SpawnDistributor {
    @NotNull List<PhantazmMob> distributeSpawns(@NotNull Collection<SpawnInfo> spawns);
}
