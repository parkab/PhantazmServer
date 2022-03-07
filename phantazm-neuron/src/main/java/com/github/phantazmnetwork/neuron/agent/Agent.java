package com.github.phantazmnetwork.neuron.agent;

import com.github.phantazmnetwork.commons.vector.Vec3I;
import com.github.phantazmnetwork.neuron.node.Calculator;
import com.github.phantazmnetwork.neuron.node.Destination;
import com.github.phantazmnetwork.neuron.operation.PathOperation;
import org.jetbrains.annotations.NotNull;

/**
 * Represents something capable of pathfinding. This is the most general representation of a navigation-capable object,
 * and generally all that is required to be used by a pathfinding algorithm such as A*. More specific sub-interfaces
 * exist to expose more complex functionality.
 * @see PhysicalAgent
 * @see WalkingAgent
 */
public interface Agent {
    /**
     * Determines if the agent has a starting location. In other words, returns {@code true} if the agent is capable of
     * pathfinding, and {@code false} if it isn't. {@link PathOperation} implementations query this method before
     * pathfinding starts. If no start position exists, the operation immediately terminates in a failed state.
     * @return {@code true} if this agent has a starting position (is valid for pathing); {@code false} otherwise
     */
    boolean hasStartPosition();

    /**
     * <p>Retrieves, and potentially computes, the starting position of this agent. This value may be cached or lazily
     * computed, but it should only expose <i>one</i> unchanging value. The starting position might not reflect the
     * actual position of the agent. In particular, the agent's actual position will typically be a single or
     * double-precision floating point 3D vector, whereas pathfinding only deals with 3D integer vectors.</p>
     *
     * <p>The computation of the starting position may be more complicated than one might expect. It is important to
     * ensure that for any returned vector {@code vec}, there exists an unobstructed path from the agent's actual
     * position to {@code vec}. Naive implementations of this method that do not perform the necessary checks may cause
     * agents to become "stuck" as they attempt to walk to their "starting" node.</p>
     *
     * <p>In some cases, the agent may be unable to produce a meaningful starting position. This can happen if the
     * agent's position is invalid (what this means is up to the implementation), or if world conditions are such that
     * there is no way to determine the starting position. In these cases, this method should throw an
     * {@link IllegalStateException}.</p>
     *
     * <p>Before calling this method, users must query {@link Agent#hasStartPosition()} to determine if a starting point
     * exists, and by extension determine if this agent is valid for pathfinding.</p>
     * @return the starting position of this entity, which should be immutable
     * @throws IllegalStateException if no start position may be computed; only if {@link Agent#hasStartPosition()}
     * returns false
     */
    @NotNull Vec3I getStartPosition();

    /**
     * Retrieves the {@link Explorer} instance used by this agent to expand new nodes as part of pathfinding.
     * @return the {@link Explorer} instance used by this agent
     */
    @NotNull Explorer getWalker();

    /**
     * Retrieves the {@link Calculator} instance this agent uses to compute the effective distance between nodes as
     * well as the heuristic.
     * @return a {@link Calculator} instance
     */
    @NotNull Calculator getCalculator();

    /**
     * <p>Determines if this agent has reached its destination, given its current position as a 3D integer, usually
     * corresponding to the position of a particular node along the path as it's being found by a
     * {@link PathOperation}.</p>
     *
     * <p>If the agent does not have a valid starting position, this may or may not throw an
     * {@link IllegalStateException}, depending on the implementation.</p>
     * @param x the x coordinate to check
     * @param y the y coordinate
     * @param z the z coordinate
     * @param destination the destination this agent is walking to
     * @return true if the destination has been successfully reached (successful completion of the path), false if the
     * PathOperation should keep checking (or fail if no new nodes exist)
     */
    boolean reachedDestination(int x, int y, int z, @NotNull Destination destination);
}