package org.phantazm.mob.skill;

import com.github.steanky.element.core.annotation.*;
import com.github.steanky.vector.Bounds3D;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.phantazm.core.particle.ParticleWrapper;
import org.phantazm.mob.PhantazmMob;

import java.util.Objects;
import java.util.Random;

@Model("mob.skill.spawn_particle")
@Cache(false)
public class SpawnParticleSkill implements Skill {

    private final Random random;

    private final ParticleWrapper particle;

    private final Data data;

    @FactoryMethod
    public SpawnParticleSkill(@NotNull Data data, @NotNull Random random,
            @NotNull @Child("particle") ParticleWrapper particle) {
        this.data = Objects.requireNonNull(data, "data");
        this.random = Objects.requireNonNull(random, "random");
        this.particle = Objects.requireNonNull(particle, "particle");
    }

    @Override
    public void use(@NotNull PhantazmMob self) {
        if (self.entity().getInstance() == null) {
            return;
        }

        double x = self.entity().getPosition().x() + data.bounds().originX() + getOffset(data.bounds().lengthX());
        double y = self.entity().getPosition().y() + data.bounds().originY() + getOffset(data.bounds().lengthY());
        double z = self.entity().getPosition().z() + data.bounds().originZ() + getOffset(data.bounds().lengthZ());

        Instance instance = self.entity().getInstance();
        if (instance != null) {
            particle.sendTo(instance, x, y, z);
        }
    }

    private double getOffset(double length) {
        if (length <= 0) {
            return 0;
        }

        return random.nextDouble(length);
    }

    @DataObject
    public record Data(@NotNull @ChildPath("particle") String particle, @NotNull Bounds3D bounds) {

    }

}
