package com.github.phantazmnetwork.zombies.game.map.objects;

import com.github.phantazmnetwork.zombies.game.map.*;
import com.github.phantazmnetwork.zombies.game.map.shop.Shop;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public record BasicMapObjects(@Unmodifiable @NotNull List<Spawnpoint> spawnpoints,
                              @Unmodifiable @NotNull List<Window> windows,
                              @Unmodifiable @NotNull List<Shop> shops,
                              @Unmodifiable @NotNull List<Door> doors,
                              @Unmodifiable @NotNull List<Room> rooms,
                              @Unmodifiable @NotNull List<Round> rounds) implements MapObjects {
    public BasicMapObjects(@Unmodifiable @NotNull List<Spawnpoint> spawnpoints,
            @Unmodifiable @NotNull List<Window> windows, @Unmodifiable @NotNull List<Shop> shops,
            @Unmodifiable @NotNull List<Door> doors, @Unmodifiable @NotNull List<Room> rooms,
            @Unmodifiable @NotNull List<Round> rounds) {
        this.spawnpoints = List.copyOf(spawnpoints);
        this.windows = List.copyOf(windows);
        this.shops = List.copyOf(shops);
        this.doors = List.copyOf(doors);
        this.rooms = List.copyOf(rooms);
        this.rounds = List.copyOf(rounds);
    }
}
