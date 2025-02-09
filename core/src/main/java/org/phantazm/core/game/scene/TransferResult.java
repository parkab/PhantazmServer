package org.phantazm.core.game.scene;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the result of transferring a player to or from a {@link Scene}.
 */
public record TransferResult(@NotNull Optional<Runnable> executor, @NotNull Optional<Component> message) {

    /**
     * Creates a transfer result.
     *
     * @param message A message relating to the routing
     */
    public TransferResult {
        Objects.requireNonNull(executor, "executor");
        Objects.requireNonNull(message, "message");
    }

    public static @NotNull TransferResult success(@NotNull Runnable executor) {
        return new TransferResult(Optional.of(executor), Optional.empty());
    }

    public static @NotNull TransferResult failure(@Nullable Component message) {
        return new TransferResult(Optional.empty(), Optional.ofNullable(message));
    }

}
