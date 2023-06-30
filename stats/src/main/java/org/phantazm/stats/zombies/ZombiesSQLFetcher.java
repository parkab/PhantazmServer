package org.phantazm.stats.zombies;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface ZombiesSQLFetcher {

    void synchronizeZombiesPlayerMapStats(@NotNull Connection connection, @NotNull ZombiesPlayerMapStats mapStats);

    @NotNull ZombiesPlayerMapStats getMapStats(@NotNull Connection connection, @NotNull UUID playerUUID,
            @NotNull Key mapKey);

    @NotNull List<BestTime> getBestTimes(@NotNull Connection connection, @NotNull Key mapKey) throws SQLException;

}
