package com.github.phantazmnetwork.zombies.equipment.gun.effect;

import com.github.phantazmnetwork.commons.Namespaces;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class StaticReloadActionBarEffect extends ReloadActionBarEffect {

    public static final Key SERIAL_KEY = Key.key(Namespaces.PHANTAZM, "gun.effect.action_bar.reload.static");

    private final Component message;

    public StaticReloadActionBarEffect(@NotNull Component message) {
        this.message = Objects.requireNonNull(message, "message");
    }

    @Override
    protected @NotNull Component getComponent(float progress) {
        return message;
    }

    @Override
    public @NotNull Key getSerialKey() {
        return SERIAL_KEY;
    }
}
