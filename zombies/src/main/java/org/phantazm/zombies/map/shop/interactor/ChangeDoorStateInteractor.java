package org.phantazm.zombies.map.shop.interactor;

import com.github.steanky.element.core.annotation.Cache;
import com.github.steanky.element.core.annotation.DataObject;
import com.github.steanky.element.core.annotation.FactoryMethod;
import com.github.steanky.element.core.annotation.Model;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;
import org.phantazm.core.VecUtils;
import org.phantazm.zombies.map.Door;
import org.phantazm.zombies.map.objects.MapObjects;
import org.phantazm.zombies.map.shop.PlayerInteraction;
import org.phantazm.zombies.player.ZombiesPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@Model("zombies.map.shop.interactor.door_state")
@Cache(false)
public class ChangeDoorStateInteractor extends InteractorBase<ChangeDoorStateInteractor.Data> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeDoorStateInteractor.class);

    private final Supplier<? extends MapObjects> mapObjects;

    private Door door;
    private boolean searchedDoor;

    @FactoryMethod
    public ChangeDoorStateInteractor(@NotNull Data data, @NotNull Supplier<? extends MapObjects> mapObjects) {
        super(data);
        this.mapObjects = Objects.requireNonNull(mapObjects, "mapObjects");
    }

    @Override
    public boolean handleInteraction(@NotNull PlayerInteraction interaction) {
        if (!searchedDoor) {
            searchedDoor = true;

            Optional<Door> doorOptional = mapObjects.get().doorTracker().atPoint(VecUtils.toPoint(data.doorPosition));
            boolean isPresent = doorOptional.isPresent();
            if (isPresent) {
                door = doorOptional.get();
            }
            else {
                LOGGER.warn("Failed to locate door at {}", data.doorPosition);
                return false;
            }
        }

        ZombiesPlayer player = interaction.player();
        if (door != null) {
            switch (data.type) {
                case OPEN -> {
                    return door.open(player);
                }
                case CLOSE -> {
                    return door.close(player);
                }
                case TOGGLE -> {
                    door.toggle(player);
                    return true;
                }
            }
        }
        else {
            LOGGER.warn("Tried to open nonexistent door at {}", data.doorPosition);
        }

        return false;
    }

    @DataObject
    public record Data(@NotNull Vec3I doorPosition, @NotNull OpenType type) {
    }

    public enum OpenType {
        OPEN,
        CLOSE,
        TOGGLE
    }
}
