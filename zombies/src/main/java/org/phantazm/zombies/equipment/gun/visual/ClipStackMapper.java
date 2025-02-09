package org.phantazm.zombies.equipment.gun.visual;

import com.github.steanky.element.core.annotation.*;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.phantazm.zombies.equipment.gun.GunState;
import org.phantazm.zombies.equipment.gun.reload.ReloadTester;

import java.util.Objects;

/**
 * A {@link GunStackMapper} that maps {@link ItemStack} based on a gun's current clip.
 */
@Model("zombies.gun.stack_mapper.clip.stack_count")
@Cache(false)
public class ClipStackMapper implements GunStackMapper {

    private final ReloadTester reloadTester;

    /**
     * Creates a {@link ClipStackMapper}.
     *
     * @param reloadTester The {@link ReloadTester} to use to determine whether the gun is currently reloading
     */
    @FactoryMethod
    public ClipStackMapper(@NotNull @Child("reload_tester") ReloadTester reloadTester) {
        this.reloadTester = Objects.requireNonNull(reloadTester, "reloadTester");
    }

    @Override
    public @NotNull ItemStack map(@NotNull GunState state, @NotNull ItemStack intermediate) {
        if (!reloadTester.isReloading(state)) {
            return intermediate.withAmount(Math.max(1, state.clip()));
        }

        return intermediate;
    }

    /**
     * Data for a {@link ClipStackMapper}.
     *
     * @param reloadTester A path to the gun's {@link ReloadTester}
     */
    @DataObject
    public record Data(@NotNull @ChildPath("reload_tester") String reloadTester) {
    }
}
