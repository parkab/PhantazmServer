package com.github.phantazmnetwork.zombies.equipment.gun.shoot.handler;

import com.github.phantazmnetwork.commons.Namespaces;
import com.github.phantazmnetwork.mob.PhantazmMob;
import com.github.phantazmnetwork.zombies.equipment.gun.GunState;
import com.github.phantazmnetwork.zombies.equipment.gun.shoot.GunHit;
import com.github.phantazmnetwork.zombies.equipment.gun.shoot.GunShot;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.DamageType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;

public class DamageShotHandler implements ShotHandler {

    public record Data(float damage, float headshotDamage) implements Keyed {

        public static final Key SERIAL_KEY = Key.key(Namespaces.PHANTAZM, "gun.shot_handler.damage");

        @Override
        public @NotNull Key key() {
            return SERIAL_KEY;
        }
    }

    public static @NotNull ConfigProcessor<Data> processor() {
        return new ConfigProcessor<>() {

            @Override
            public @NotNull Data dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
                float damage = element.getNumberOrThrow("damage").floatValue();
                float headshotDamage = element.getNumberOrThrow("headshotDamage").floatValue();
                return new Data(damage, headshotDamage);
            }

            @Override
            public @NotNull ConfigElement elementFromData(@NotNull Data data) {
                ConfigNode node = new LinkedConfigNode(2);
                node.putNumber("damage", data.damage());
                node.putNumber("headshotDamage", data.headshotDamage());
                return node;
            }
        };
    }

    private final Data data;

    public DamageShotHandler(@NotNull Data data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public void handle(@NotNull GunState state, @NotNull Player attacker, @NotNull Collection<PhantazmMob> previousHits, @NotNull GunShot shot) {
        for (GunHit target : shot.regularTargets()) {
            target.mob().entity().damage(DamageType.fromPlayer(attacker), data.damage());
        }
        for (GunHit target : shot.headshotTargets()) {
            target.mob().entity().damage(DamageType.fromPlayer(attacker), data.headshotDamage());
        }
    }

    @Override
    public void tick(@NotNull GunState state, long time) {

    }

    @Override
    public @NotNull Keyed getData() {
        return data;
    }

}
