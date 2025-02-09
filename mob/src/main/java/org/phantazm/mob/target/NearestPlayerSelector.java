package org.phantazm.mob.target;

import com.github.steanky.element.core.annotation.*;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Objects;

@Model("mob.selector.nearest_player")
@Cache(false)
public class NearestPlayerSelector extends FirstTargetSelector<Player> {

    /**
     * Creates a new {@link MappedSelector}.
     *
     * @param delegate The delegate {@link TargetSelector} to map
     */
    @FactoryMethod
    public NearestPlayerSelector(@NotNull @Child("delegate") TargetSelector<Iterable<Player>> delegate) {
        super(delegate);
    }

    @Override
    protected Player map(@NotNull Iterable<Player> players) {
        Iterator<Player> iterator = players.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }

        return null;
    }

    @DataObject
    public record Data(@NotNull @ChildPath("delegate") String delegate) {

        public Data {
            Objects.requireNonNull(delegate, "delegate");
        }

    }
}
