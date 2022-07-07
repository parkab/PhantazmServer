package com.github.phantazmnetwork.api.particle.data;

import com.github.phantazmnetwork.commons.Namespaces;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.Block;
import net.minestom.server.particle.Particle;
import net.minestom.server.utils.binary.BinaryWriter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;

public final class BlockParticleData implements ParticleData {

    public static final Key SERIAL_KEY = Key.key(Namespaces.PHANTAZM, "particle_data.block");

    private static final Set<Key> VALID_KEYS = Set.of(
            Particle.BLOCK.key(),
            Particle.BLOCK_MARKER.key(),
            Particle.FALLING_DUST.key()
    );

    public static @NotNull ConfigProcessor<BlockParticleData> processor() {
        return new ConfigProcessor<>() {
            @Override
            public @NotNull BlockParticleData dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
                short stateId = element.getNumberOrThrow("stateId").shortValue();
                return new BlockParticleData(stateId);
            }

            @Override
            public @NotNull ConfigElement elementFromData(@NotNull BlockParticleData blockParticleData) {
                LinkedConfigNode node = new LinkedConfigNode(1);
                node.putNumber("stateId", blockParticleData.stateId);

                return node;
            }
        };
    }

    private final short stateId;

    public BlockParticleData(@NotNull Block block) {
        this(Objects.requireNonNull(block, "block").stateId());
    }

    private BlockParticleData(short stateId) {
        this.stateId = stateId;
    }

    @Override
    public boolean isValid(@NotNull Particle particle) {
        return VALID_KEYS.contains(particle.key());
    }

    @Override
    public void write(@NotNull BinaryWriter binaryWriter) {
        binaryWriter.writeVarInt(stateId);
    }

    @Override
    public @NotNull Key key() {
        return SERIAL_KEY;
    }
}
