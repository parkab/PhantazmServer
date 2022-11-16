package com.github.phantazmnetwork.zombies.powerup;

import com.github.phantazmnetwork.zombies.player.ZombiesPlayer;
import com.github.steanky.element.core.annotation.*;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

@Model("zombies.powerup.action.player_flagging")
public class PlayerFlaggingPowerupActionFactory implements Supplier<PowerupAction> {
    private final Data data;
    private final Supplier<DeactivationPredicate> deactivationPredicate;

    @FactoryMethod
    public PlayerFlaggingPowerupActionFactory(@NotNull Data data,
            @NotNull @DataName("deactivation_predicate") Supplier<DeactivationPredicate> deactivationPredicate) {
        this.data = Objects.requireNonNull(data, "data");
        this.deactivationPredicate = Objects.requireNonNull(deactivationPredicate, "deactivationPredicate");
    }

    @Override
    public PowerupAction get() {
        return new PlayerFlaggingPowerupAction(data, deactivationPredicate.get());
    }

    @DataObject
    public record Data(@NotNull Key flag, @NotNull @DataPath("deactivation_predicate") String deactivationPredicate) {
    }

    private static class PlayerFlaggingPowerupAction extends PowerupActionBase {
        private final Data data;

        private PlayerFlaggingPowerupAction(Data data, DeactivationPredicate deactivationPredicate) {
            super(deactivationPredicate);
            this.data = data;
        }

        @Override
        public void activate(@NotNull ZombiesPlayer player, long time) {
            super.activate(player, time);
            player.flags().setFlag(data.flag);
        }

        @Override
        public void deactivate(@NotNull ZombiesPlayer player) {
            player.flags().clearFlag(data.flag);
        }
    }
}
