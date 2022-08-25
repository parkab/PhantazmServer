package com.github.phantazmnetwork.zombies.game.scoreboard.sidebar.lineupdater;

import com.github.phantazmnetwork.commons.AdventureConfigProcessors;
import com.github.phantazmnetwork.commons.Wrapper;
import com.github.phantazmnetwork.core.time.TickFormatter;
import com.github.steanky.element.core.ElementFactory;
import com.github.steanky.element.core.annotation.DataObject;
import com.github.steanky.element.core.annotation.FactoryMethod;
import com.github.steanky.element.core.annotation.Model;
import com.github.steanky.element.core.annotation.ProcessorMethod;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

@Model("zombies.sidebar.lineupdater.ticks")
public class TicksLineUpdater implements SidebarLineUpdater {

    @DataObject
    public record Data(@NotNull Key tickDependencyReference, @NotNull String tickFormatterPath) {

    }

    private static final ElementFactory<Data, TicksLineUpdater> FACTORY = (objectData, context, dependencyProvider) -> {
        Wrapper<Long> ticksWrapper = dependencyProvider.provide(Key.key("wrapper.long"));
        TickFormatter tickFormatter = context.provide(objectData.tickFormatterPath(), dependencyProvider);
        return new TicksLineUpdater(ticksWrapper, tickFormatter);
    };

    private final Wrapper<Long> ticksWrapper;

    private final TickFormatter tickFormatter;

    private long lastTicks = -1;

    public TicksLineUpdater(@NotNull Wrapper<Long> ticksWrapper, @NotNull TickFormatter tickFormatter) {
        this.ticksWrapper = Objects.requireNonNull(ticksWrapper, "ticksWrapper");
        this.tickFormatter = Objects.requireNonNull(tickFormatter, "tickFormatter");
    }

    @FactoryMethod
    public static ElementFactory<Data, TicksLineUpdater> factory() {
        return FACTORY;
    }

    @ProcessorMethod
    public static ConfigProcessor<Data> processor() {
        ConfigProcessor<Key> keyProcessor = AdventureConfigProcessors.key();
        return new ConfigProcessor<Data>() {

            @Override
            public Data dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
                Key tickDependencyReference =
                        keyProcessor.dataFromElement(element.getElementOrThrow("tickDependencyReference"));
                String tickFormatterPath = element.getStringOrThrow("tickFormatterPath");
                return new Data(tickDependencyReference, tickFormatterPath);
            }

            @Override
            public @NotNull ConfigElement elementFromData(@NotNull Data data) {
                return ConfigNode.of("tickDependencyReference", data.tickDependencyReference(), "tickFormatterPath",
                        data.tickFormatterPath());
            }
        };
    }

    @Override
    public void invalidateCache() {
        lastTicks = -1;
    }

    @Override
    public @NotNull Optional<Component> tick(long time) {
        if (lastTicks == -1 || lastTicks != ticksWrapper.get()) {
            lastTicks = ticksWrapper.get();
            return Optional.of(tickFormatter.format(ticksWrapper.get()));
        }

        return Optional.empty();
    }
}
