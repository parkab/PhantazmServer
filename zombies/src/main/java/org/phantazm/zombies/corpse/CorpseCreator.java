package org.phantazm.zombies.corpse;

import com.github.steanky.element.core.annotation.*;
import com.github.steanky.element.core.dependency.DependencyProvider;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.mapper.annotation.Default;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.instance.Instance;
import net.minestom.server.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.phantazm.commons.Activable;
import org.phantazm.core.ComponentUtils;
import org.phantazm.core.entity.fakeplayer.MinimalFakePlayer;
import org.phantazm.core.hologram.Hologram;
import org.phantazm.core.hologram.InstanceHologram;
import org.phantazm.core.time.TickFormatter;
import org.phantazm.zombies.player.ZombiesPlayer;
import org.phantazm.zombies.player.state.revive.ReviveHandler;

import java.util.List;
import java.util.UUID;

@Model("zombies.corpse")
@Cache(false)
public class CorpseCreator {
    public interface Source {
        @NotNull CorpseCreator make(@NotNull DependencyProvider mapDependencyProvider);
    }

    private final Data data;
    private final List<CorpseLine> idleLines;
    private final List<CorpseLine> revivingLines;
    private final Team corpseTeam;

    @FactoryMethod
    public CorpseCreator(@NotNull Data data, @NotNull @Child("idle_lines") List<CorpseLine> idleLines,
            @NotNull @Child("reviving_lines") List<CorpseLine> revivingLines, @NotNull Team corpseTeam) {
        this.data = data;
        this.idleLines = List.copyOf(idleLines);
        this.revivingLines = List.copyOf(revivingLines);
        this.corpseTeam = corpseTeam;
    }

    public @NotNull CorpseCreator.Corpse forPlayer(@NotNull Instance instance, @NotNull ZombiesPlayer zombiesPlayer,
            @NotNull Point deathLocation, @NotNull ReviveHandler reviveHandler) {
        PlayerSkin skin = zombiesPlayer.getPlayer().map(Player::getSkin).orElse(null);
        String corpseUsername = UUID.randomUUID().toString().substring(0, 16);
        MinimalFakePlayer corpseEntity =
                new MinimalFakePlayer(MinecraftServer.getSchedulerManager(), corpseUsername, skin);

        Hologram hologram = new InstanceHologram(deathLocation.add(0, data.hologramHeightOffset, 0), data.hologramGap);

        hologram.setInstance(instance);
        corpseEntity.setInstance(instance, deathLocation.add(0, data.corpseHeightOffset, 0));
        corpseTeam.addMember(corpseUsername);

        return new Corpse(reviveHandler, hologram, corpseEntity, idleLines, revivingLines);
    }

    @DataObject
    public record Data(double hologramGap,
                       double hologramHeightOffset,
                       double corpseHeightOffset,
                       @NotNull @ChildPath("idle_lines") List<String> idleLines,
                       @NotNull @ChildPath("reviving_lines") List<String> revivingLines) {
        @Default("hologramGap")
        public static ConfigElement defaultHologramGap() {
            return ConfigPrimitive.of(0.0);
        }

        @Default("hologramHeightOffset")
        public static ConfigElement defaultHologramHeightOffset() {
            return ConfigPrimitive.of(1.0);
        }

        @Default("corpseHeightOffset")
        public static ConfigElement defaultCorpseHeightOffset() {
            return ConfigPrimitive.of(0.25);
        }
    }

    public interface CorpseLine {
        @NotNull Component update(@NotNull Corpse corpse, long time);
    }

    @Model("zombies.corpse.line.static")
    @Cache
    public static class StaticLine implements CorpseLine {
        private final Data data;

        @FactoryMethod
        public StaticLine(@NotNull Data data) {
            this.data = data;
        }

        @Override
        public @NotNull Component update(@NotNull Corpse corpse, long time) {
            return data.message;
        }

        @DataObject
        public record Data(@NotNull Component message) {
        }
    }

    @Model("zombies.corpse.line.time")
    @Cache
    public static class TimeLine implements CorpseLine {
        private final Data data;
        private final TickFormatter tickFormatter;

        @FactoryMethod
        public TimeLine(@NotNull Data data, @NotNull @Child("tick_formatter") TickFormatter tickFormatter) {
            this.data = data;
            this.tickFormatter = tickFormatter;
        }

        @Override
        public @NotNull Component update(@NotNull Corpse corpse, long time) {
            return ComponentUtils.tryFormat(data.formatString, tickFormatter.format(data.ticksUntilRevive
                                                                                    ? corpse.reviveHandler.getTicksUntilRevive()
                                                                                    : corpse.reviveHandler.getTicksUntilDeath()));
        }

        @DataObject
        public record Data(@NotNull String formatString,
                           boolean ticksUntilRevive,
                           @NotNull @ChildPath("tick_formatter") String tickFormatter) {
        }
    }

    public class Corpse implements Activable {
        private final ReviveHandler reviveHandler;

        private final Hologram hologram;
        private final MinimalFakePlayer corpseEntity;

        private final List<CorpseLine> idleLines;
        private final List<CorpseLine> revivingLines;

        private List<CorpseLine> currentLines;

        private Corpse(@NotNull ReviveHandler reviveHandler, @NotNull Hologram hologram,
                @NotNull MinimalFakePlayer corpseEntity, @NotNull List<CorpseLine> idleLines,
                @NotNull List<CorpseLine> revivingLines) {
            this.reviveHandler = reviveHandler;
            this.hologram = hologram;
            this.corpseEntity = corpseEntity;
            this.idleLines = idleLines;
            this.revivingLines = revivingLines;
        }

        @Override
        public void start() {
            for (CorpseLine corpseLine : idleLines) {
                hologram.add(corpseLine.update(this, System.currentTimeMillis()));
            }

            this.currentLines = idleLines;

            corpseEntity.init();
            corpseEntity.setPose(Entity.Pose.SLEEPING);
        }

        public @NotNull ReviveHandler reviveHandler() {
            return reviveHandler;
        }

        public void tick(long time) {
            List<CorpseLine> currentLines = this.currentLines;
            if (currentLines == null) {
                return;
            }

            List<CorpseLine> newLines = reviveHandler.isReviving() ? revivingLines : idleLines;
            if (newLines != currentLines) {
                hologram.clear();
                hologram.addAll(newLines.stream().map(line -> line.update(this, time)).toList());
                this.currentLines = newLines;
                return;
            }

            for (int i = 0; i < CorpseCreator.this.idleLines.size(); i++) {
                Component newLine = currentLines.get(i).update(this, time);
                Component oldLine = hologram.get(i);

                if (!newLine.equals(oldLine)) {
                    hologram.set(i, newLine);
                }
            }
        }

        public void disable() {
            hologram.clear();
        }

        public void remove() {
            disable();
            corpseEntity.remove();
        }

        public @NotNull Activable asKnockActivable() {
            return new Activable() {
                @Override
                public void start() {
                    Corpse.this.start();
                }

                @Override
                public void tick(long time) {
                    Corpse.this.tick(time);
                }

                @Override
                public void end() {
                    Corpse.this.remove();
                }
            };
        }

        public @NotNull Activable asDeathActivable() {
            return new Activable() {
                @Override
                public void end() {
                    Corpse.this.disable();
                }
            };
        }

    }
}
