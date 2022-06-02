package com.github.phantazmnetwork.zombies.map;

import com.github.phantazmnetwork.commons.vector.Vec3I;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record MapInfo(@NotNull Key id, @NotNull String displayName, @NotNull String displayItemNBT,
                      @NotNull Vec3I origin, @NotNull String roomsPath, @NotNull String doorsPath,
                      @NotNull String shopsPath, @NotNull String windowsPath, @NotNull String roundsPath) { }
