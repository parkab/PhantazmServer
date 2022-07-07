package com.github.phantazmnetwork.zombies.equipment.gun.reload.actionbar;

import com.github.phantazmnetwork.zombies.equipment.gun.GunState;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ReloadActionBarChooser {

    @NotNull Component choose(@NotNull GunState state, @NotNull Player player, float progress);

}
