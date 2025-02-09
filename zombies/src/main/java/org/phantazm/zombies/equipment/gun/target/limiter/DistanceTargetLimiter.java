package org.phantazm.zombies.equipment.gun.target.limiter;

import com.github.steanky.element.core.annotation.Cache;
import com.github.steanky.element.core.annotation.DataObject;
import com.github.steanky.element.core.annotation.FactoryMethod;
import com.github.steanky.element.core.annotation.Model;
import it.unimi.dsi.fastutil.Pair;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * A {@link TargetLimiter} based on distance from the shot's start position.
 */
@Model("zombies.gun.target_limiter.distance")
@Cache
public class DistanceTargetLimiter implements TargetLimiter {

    private final Data data;

    /**
     * Creates a new {@link DistanceTargetLimiter} with the given {@link Data}.
     *
     * @param data The {@link Data} to use
     */
    @FactoryMethod
    public DistanceTargetLimiter(@NotNull Data data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public @NotNull List<Pair<? extends LivingEntity, Vec>> limitTargets(@NotNull Pos start,
            @NotNull List<Pair<? extends LivingEntity, Vec>> targets) {
        List<Pair<? extends LivingEntity, Vec>> targetsCopy = new ArrayList<>(targets);
        Comparator<Pair<? extends LivingEntity, Vec>> comparator =
                Comparator.comparingDouble(pair -> start.distanceSquared(pair.value()));
        if (!data.prioritizeClosest()) {
            comparator = comparator.reversed();
        }

        targetsCopy.sort(comparator);
        return targetsCopy.subList(0, Math.min(targets.size(), data.targetLimit()));
    }

    /**
     * Data for a {@link DistanceTargetLimiter}.
     *
     * @param targetLimit       The maximum number of targets to select
     * @param prioritizeClosest Whether the closest targets should be prioritized or the farthest targets should be prioritized
     */
    @DataObject
    public record Data(int targetLimit, boolean prioritizeClosest) {

    }
}
