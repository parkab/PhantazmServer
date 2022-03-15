package com.github.phantazmnetwork.neuron.world;

import com.github.phantazmnetwork.commons.vector.Vec3I;
import org.jetbrains.annotations.Nullable;
import com.github.phantazmnetwork.neuron.agent.Agent;

/**
 * <p>This class converts a position and direction — represented by the vectors (x, y, z) and (deltaX, deltaY, deltaZ),
 * respectively — into a vector representative of the movement an {@link Agent} would have to make in order to travel
 * from (x, y, z) to an arbitrary calculated vector {@code dest} which varies depending on the environment and other
 * factors, which may be specific to the agent.</p>
 *
 * <p>NodeTranslator also serves the secondary function of determining which <i>direction</i> vectors are invalid (may
 * not be traversed). If a direction is invalid, {@link NodeTranslator#translate(int, int, int, int, int, int)} will
 * return {@code null}.</p>
 */
@FunctionalInterface
public interface NodeTranslator {
    /**
     * Determines (adjusts) the given delta vector (deltaX, deltaY, deltaZ), typically according to environmental
     * factors. A typical use case would be "snapping" the vector pointing to the location of a new node such that it is
     * on top of a solid. If this movement attempt is not valid, this method will return null.
     * @param x the x-component of the current node's position
     * @param y the y-component of the current node's position
     * @param z the z-component of the current node's position
     * @param deltaX the x-component of the attempted movement vector
     * @param deltaY the y-component of the attempted movement vector
     * @param deltaZ the z-component of the attempted movement vector
     * @return the adjusted delta vector, or null if it's not possible to move this direction
     */
    @Nullable Vec3I translate(int x, int y, int z, int deltaX, int deltaY, int deltaZ);
}
