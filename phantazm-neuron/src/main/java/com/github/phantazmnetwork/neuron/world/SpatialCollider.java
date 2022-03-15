package com.github.phantazmnetwork.neuron.world;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.ToDoubleFunction;

public class SpatialCollider implements Collider {
    @FunctionalInterface
    private interface DoubleBiPredicate {
        boolean test(double a, double b);
    }

    private final Space space;

    public SpatialCollider(@NotNull Space space) {
        this.space = Objects.requireNonNull(space, "space");
    }

    @Override
    public double highestCollisionAlong(double oX, double oY, double oZ, double vX, double vY, double vZ, double dX,
                                        double dY, double dZ) {
        return collidesMovingAlong(oX, oY, oZ, vX, vY, vZ, dX, dY, dZ, solid -> solid.getY() + solid.originY() + solid
                .vectorY(), (a, b) -> a > b, Double.NEGATIVE_INFINITY);
    }

    @Override
    public double lowestCollisionAlong(double oX, double oY, double oZ, double vX, double vY, double vZ, double dX,
                                       double dY, double dZ) {
        return collidesMovingAlong(oX, oY, oZ, vX, vY, vZ, dX, dY, dZ, solid -> solid.getY() + solid.originY(),
                (a, b) -> a < b, Double.POSITIVE_INFINITY);
    }

    @Override
    public double heightAt(int x, int y, int z) {
        Solid solid = space.solidAt(x, y, z);
        return solid == null ? y : y + solid.vectorY();
    }

    private double collidesMovingAlong(double oX, double oY, double oZ, double vX, double vY, double vZ, double dX,
                                       double dY, double dZ, ToDoubleFunction<Solid> valueFunction,
                                       DoubleBiPredicate valuePredicate, double initialValue) {
        double eoX = oX;
        double eoY = oY;
        double eoZ = oZ;

        double evX = vX;
        double evY = vY;
        double evZ = vZ;

        //perform an expansion in direction (dX, dY, dZ)
        if(dX < 0) {
            eoX += dX;
            evX -= dX;
        }
        else {
            evX += dX;
        }

        if(dY < 0) {
            eoY += dY;
            evY -= dY;
        }
        else {
            evY += dY;
        }

        if(dZ < 0) {
            eoZ += dZ;
            evZ -= dZ;
        }
        else {
            evZ += dZ;
        }

        double adjustedXZ = (Math.max(vX, vZ) * (Math.abs(dX) + Math.abs(dZ))) / 2;
        double adjustedXY = (vY * (Math.abs(dX) + Math.abs(dY))) / 2;
        double adjustedZY = (vY * (Math.abs(dZ) + Math.abs(dY))) / 2;

        double centerX = oX + (vX / 2);
        double centerY = oY + (vY / 2);
        double centerZ = oZ + (vZ / 2);

        double best = initialValue;
        for(Solid candidate : space.solidsOverlapping(eoX, eoY, eoZ, evX, evY, evZ)) {
            double coX = candidate.getX() + candidate.originX();
            double coY = candidate.getY() + candidate.originY();
            double coZ = candidate.getZ() + candidate.originZ();

            float cvX = candidate.vectorX();
            float cvY = candidate.vectorY();
            float cvZ = candidate.vectorZ();

            //only check solids not overlapping with the original bounds
            if(!Solid.overlaps(coX, coY, coZ, cvX, cvY, cvZ, oX, oY, oZ, vX, vY, vZ)) {
                double minX = coX - centerX;
                double minY = coY - centerY;
                double minZ = coZ - centerZ;

                double maxX = minX + cvX;
                double maxY = minY + cvY;
                double maxZ = minZ + cvZ;

                if(checkPair(adjustedXZ, dX, dZ, minX, minZ, maxX, maxZ) &&
                        checkPair(adjustedXY, dX, dY, minX, minY, maxX, maxY) &&
                        checkPair(adjustedZY, dZ, dY, minZ, minY, maxZ, maxY)) {
                    //collision found
                    double value = valueFunction.applyAsDouble(candidate);
                    if(valuePredicate.test(value, best)) {
                        best = value;
                    }
                }
            }
        }

        return best;
    }

    private static boolean checkPair(double size, double dA, double dB, double minA, double minB, double maxA,
                                     double maxB) {
        if(dA == 0 && dB == 0) {
            return true;
        }

        return dA * dB <= 0 ? checkPlane(size, dA, dB, minA, minB, maxA, maxB) : checkPlane(size, dA, dB, maxA, minB,
                minA, maxB);
    }

    private static boolean checkPlane(double size, double dA, double dB, double minA, double minB, double maxA,
                                      double maxB) {
        double bMinusAMin = (minB * dA) - (minA * dB);
        if(bMinusAMin >= size) { //!minInFirst
            return (maxB * dA) - (maxA * dB) < size;  //... && maxInFirst
        }

        //we know minInFirst is true
        if(bMinusAMin > -size) { //... && minInSecond
            return true;
        }

        return (maxB * dA) - (maxA * dB) > -size; // ... && !minInSecond
    }
}