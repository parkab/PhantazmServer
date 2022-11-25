package com.github.phantazmnetwork.zombies.equipment.gun.shoot.blockiteration;

import com.github.phantazmnetwork.core.VecUtils;
import com.github.phantazmnetwork.zombies.map.objects.MapObjects;
import com.github.steanky.element.core.annotation.Cache;
import com.github.steanky.element.core.annotation.Dependency;
import com.github.steanky.element.core.annotation.FactoryMethod;
import com.github.steanky.element.core.annotation.Model;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Model("zombies.gun.block_iteration.window")
@Cache(false)
public class WindowBlockIteration implements BlockIteration {

    private final Context context;

    @FactoryMethod
    public WindowBlockIteration(@NotNull @Dependency("zombies.dependency.gun.map_objects") MapObjects mapObjects) {
        Objects.requireNonNull(mapObjects, "mapObjects");
        this.context = new Context() {
            @Override
            public boolean isValidEndpoint(@NotNull Point blockLocation, @NotNull Block block) {
                return true;
            }

            @Override
            public boolean acceptRaytracedBlock(@NotNull Vec intersection, @NotNull Block block) {
                return mapObjects.windowAt(VecUtils.toBlockInt(intersection)).isEmpty();
            }
        };
    }

    @Override
    public @NotNull Context createContext() {
        return context;
    }

}
