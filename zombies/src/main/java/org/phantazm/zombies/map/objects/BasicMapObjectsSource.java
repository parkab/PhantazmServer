package org.phantazm.zombies.map.objects;

import com.github.steanky.element.core.ElementException;
import com.github.steanky.element.core.annotation.Depend;
import com.github.steanky.element.core.annotation.Memoize;
import com.github.steanky.element.core.context.ContextManager;
import com.github.steanky.element.core.context.ElementContext;
import com.github.steanky.element.core.dependency.DependencyModule;
import com.github.steanky.element.core.dependency.DependencyProvider;
import com.github.steanky.element.core.dependency.ModuleDependencyProvider;
import com.github.steanky.element.core.key.KeyParser;
import com.github.steanky.element.core.path.ElementPath;
import com.github.steanky.toolkit.collection.Wrapper;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.phantazm.commons.TickTaskScheduler;
import org.phantazm.core.ClientBlockHandler;
import org.phantazm.core.ClientBlockHandlerSource;
import org.phantazm.core.ElementUtils;
import org.phantazm.core.VecUtils;
import org.phantazm.core.gui.BasicSlotDistributor;
import org.phantazm.core.gui.SlotDistributor;
import org.phantazm.core.sound.SongPlayer;
import org.phantazm.core.tracker.BoundedTracker;
import org.phantazm.mob.MobModel;
import org.phantazm.mob.MobStore;
import org.phantazm.mob.PhantazmMob;
import org.phantazm.mob.spawner.MobSpawner;
import org.phantazm.zombies.coin.BasicTransactionModifierSource;
import org.phantazm.zombies.coin.TransactionModifierSource;
import org.phantazm.zombies.map.*;
import org.phantazm.zombies.map.action.Action;
import org.phantazm.zombies.map.handler.RoundHandler;
import org.phantazm.zombies.map.handler.WindowHandler;
import org.phantazm.zombies.map.shop.Shop;
import org.phantazm.zombies.map.shop.display.ShopDisplay;
import org.phantazm.zombies.map.shop.interactor.ShopInteractor;
import org.phantazm.zombies.map.shop.predicate.ShopPredicate;
import org.phantazm.zombies.mob.MobSpawnerSource;
import org.phantazm.zombies.player.ZombiesPlayer;
import org.phantazm.zombies.powerup.PowerupHandler;
import org.phantazm.zombies.spawn.BasicSpawnDistributor;
import org.phantazm.zombies.spawn.SpawnDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BasicMapObjectsSource implements MapObjects.Source {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicMapObjectsSource.class);
    private static final Consumer<? super ElementException> HANDLER = ElementUtils.logging(LOGGER, "map");

    private final MapInfo mapInfo;
    private final ContextManager contextManager;
    private final MobSpawnerSource mobSpawnerSource;
    private final Map<Key, MobModel> mobModels;
    private final ClientBlockHandlerSource clientBlockHandlerSource;
    private final KeyParser keyParser;

    public BasicMapObjectsSource(@NotNull MapInfo mapInfo, @NotNull ContextManager contextManager,
            @NotNull MobSpawnerSource mobSpawnerSource, @NotNull Map<Key, MobModel> mobModels,
            @NotNull ClientBlockHandlerSource clientBlockHandlerSource, @NotNull KeyParser keyParser) {
        this.mapInfo = Objects.requireNonNull(mapInfo, "mapInfo");
        this.contextManager = Objects.requireNonNull(contextManager, "contextManager");
        this.mobSpawnerSource = Objects.requireNonNull(mobSpawnerSource, "mobSpawnerSource");
        this.mobModels = Objects.requireNonNull(mobModels, "mobModels");
        this.clientBlockHandlerSource = Objects.requireNonNull(clientBlockHandlerSource, "clientBlockHandlerSource");
        this.keyParser = Objects.requireNonNull(keyParser, "keyParser");
    }

    @Override
    public @NotNull MapObjects make(@NotNull Instance instance,
            @NotNull Map<? super UUID, ? extends ZombiesPlayer> playerMap,
            @NotNull Supplier<? extends RoundHandler> roundHandlerSupplier, @NotNull MobStore mobStore,
            @Nullable Team mobNoPushTeam, @NotNull Wrapper<PowerupHandler> powerupHandler,
            @NotNull Wrapper<WindowHandler> windowHandler, @NotNull Wrapper<EventNode<Event>> eventNode,
            @NotNull SongPlayer songPlayer, @NotNull TickTaskScheduler tickTaskScheduler, @NotNull Team corpseTeam) {
        Random random = new Random();
        ClientBlockHandler clientBlockHandler = clientBlockHandlerSource.forInstance(instance);
        SpawnDistributor spawnDistributor =
                new BasicSpawnDistributor(mobModels::get, random, playerMap.values(), mobNoPushTeam);

        Flaggable flaggable = new BasicFlaggable();
        TransactionModifierSource transactionModifierSource = new BasicTransactionModifierSource();
        SlotDistributor slotDistributor = new BasicSlotDistributor(1);

        MapSettingsInfo mapSettingsInfo = mapInfo.settings();
        Pos respawnPos =
                new Pos(VecUtils.toPoint(mapSettingsInfo.origin().add(mapSettingsInfo.spawn())), mapSettingsInfo.yaw(),
                        mapSettingsInfo.pitch());

        Wrapper<MapObjects> mapObjectsWrapper = Wrapper.ofNull();
        MobSpawner mobSpawner = mobSpawnerSource.make(random, mapObjectsWrapper, mobStore);

        Module module =
                new Module(keyParser, instance, random, roundHandlerSupplier, flaggable, transactionModifierSource,
                        slotDistributor, playerMap, respawnPos, mapObjectsWrapper, powerupHandler, windowHandler,
                        eventNode, mobStore, songPlayer, corpseTeam);

        DependencyProvider provider = new ModuleDependencyProvider(keyParser, module);

        Point origin = VecUtils.toPoint(mapSettingsInfo.origin());

        Map<Key, SpawnruleInfo> spawnruleInfoMap = buildSpawnrules(mapInfo.spawnrules());

        List<Shop> shops = buildShops(origin, mapInfo.shops(), provider, instance);
        BoundedTracker<Shop> shopTracker = BoundedTracker.tracker(shops);

        List<Door> doors = buildDoors(origin, mapInfo.doors(), provider, instance, mapObjectsWrapper);
        BoundedTracker<Door> doorTracker = BoundedTracker.tracker(doors);

        List<Room> rooms = buildRooms(origin, mapInfo.rooms(), provider);
        BoundedTracker<Room> roomTracker = BoundedTracker.tracker(rooms);

        List<Window> windows =
                buildWindows(origin, mapInfo.windows(), provider, instance, clientBlockHandler, roomTracker);
        BoundedTracker<Window> windowTracker = BoundedTracker.tracker(windows);

        List<Spawnpoint> spawnpoints =
                buildSpawnpoints(origin, mapInfo.spawnpoints(), spawnruleInfoMap, instance, mobSpawner, windowTracker,
                        roomTracker);

        List<Round> rounds = buildRounds(mapInfo.rounds(), spawnpoints, provider, spawnDistributor);

        MapObjects mapObjects =
                new BasicMapObjects(spawnpoints, windowTracker, shopTracker, doorTracker, roomTracker, rounds, provider,
                        mobSpawner, origin, module, tickTaskScheduler);
        mapObjectsWrapper.set(mapObjects);

        return mapObjects;
    }

    private Map<Key, SpawnruleInfo> buildSpawnrules(List<SpawnruleInfo> spawnruleInfoList) {
        Map<Key, SpawnruleInfo> spawnruleInfoMap = new HashMap<>(spawnruleInfoList.size());
        for (SpawnruleInfo spawnruleInfo : spawnruleInfoList) {
            if (spawnruleInfoMap.putIfAbsent(spawnruleInfo.id(), spawnruleInfo) != null) {
                LOGGER.warn("Spawnrule found with duplicate id '{}'", spawnruleInfo.id());
            }
        }

        return spawnruleInfoMap;
    }

    private List<Spawnpoint> buildSpawnpoints(Point mapOrigin, List<SpawnpointInfo> spawnpointInfoList,
            Map<Key, SpawnruleInfo> spawnruleInfoMap, Instance instance, MobSpawner mobSpawner,
            BoundedTracker<Window> windowTracker, BoundedTracker<Room> roomTracker) {
        List<Spawnpoint> spawnpoints = new ArrayList<>(spawnpointInfoList.size());
        for (SpawnpointInfo spawnpointInfo : spawnpointInfoList) {
            spawnpoints.add(new Spawnpoint(mapOrigin, spawnpointInfo, instance, spawnruleInfoMap::get, mobSpawner,
                    windowTracker, roomTracker));
        }

        return spawnpoints;
    }

    private List<Window> buildWindows(Point mapOrigin, List<WindowInfo> windowInfoList,
            DependencyProvider dependencyProvider, Instance instance, ClientBlockHandler clientBlockHandler,
            BoundedTracker<Room> roomTracker) {

        List<Window> windows = new ArrayList<>(windowInfoList.size());
        for (WindowInfo windowInfo : windowInfoList) {
            List<Action<Window>> repairActions = contextManager.makeContext(windowInfo.repairActions())
                    .provideCollection(ElementPath.EMPTY, dependencyProvider, HANDLER);

            List<Action<Window>> breakActions = contextManager.makeContext(windowInfo.breakActions())
                    .provideCollection(ElementPath.EMPTY, dependencyProvider, HANDLER);

            windows.add(new Window(mapOrigin, instance, windowInfo, clientBlockHandler, repairActions, breakActions,
                    roomTracker));
        }

        return windows;
    }

    private List<Shop> buildShops(Point mapOrigin, List<ShopInfo> shopInfoList, DependencyProvider dependencyProvider,
            Instance instance) {
        List<Shop> shops = new ArrayList<>(shopInfoList.size());
        for (ShopInfo shopInfo : shopInfoList) {
            ElementContext shopContext = contextManager.makeContext(shopInfo.data());

            List<ShopPredicate> predicates =
                    shopContext.provideCollection(ElementPath.of("predicates"), dependencyProvider, HANDLER);
            List<ShopInteractor> successInteractors =
                    shopContext.provideCollection(ElementPath.of("successInteractors"), dependencyProvider, HANDLER);
            List<ShopInteractor> failureInteractors =
                    shopContext.provideCollection(ElementPath.of("failureInteractors"), dependencyProvider, HANDLER);
            List<ShopDisplay> displays =
                    shopContext.provideCollection(ElementPath.of("displays"), dependencyProvider, HANDLER);

            shops.add(new Shop(mapOrigin, shopInfo, instance, predicates, successInteractors, failureInteractors,
                    displays));
        }

        return shops;
    }

    private List<Door> buildDoors(Point mapOrigin, List<DoorInfo> doorInfoList, DependencyProvider dependencyProvider,
            Instance instance, Wrapper<MapObjects> mapObjectsWrapper) {
        List<Door> doors = new ArrayList<>(doorInfoList.size());
        for (DoorInfo doorInfo : doorInfoList) {
            List<Action<Door>> openActions = contextManager.makeContext(doorInfo.openActions())
                    .provideCollection(ElementPath.EMPTY, dependencyProvider, HANDLER);

            List<Action<Door>> closeActions = contextManager.makeContext(doorInfo.closeActions())
                    .provideCollection(ElementPath.EMPTY, dependencyProvider, HANDLER);

            List<Action<Door>> failOpenActions = contextManager.makeContext(doorInfo.failOpenActions())
                    .provideCollection(ElementPath.EMPTY, dependencyProvider, HANDLER);

            doors.add(new Door(mapOrigin, doorInfo, instance, Block.AIR, openActions, closeActions, failOpenActions,
                    mapObjectsWrapper));
        }

        return doors;
    }

    private List<Room> buildRooms(Point mapOrigin, List<RoomInfo> roomInfoList, DependencyProvider dependencyProvider) {
        List<Room> rooms = new ArrayList<>(roomInfoList.size());
        for (RoomInfo roomInfo : roomInfoList) {
            List<Action<Room>> openActions = contextManager.makeContext(roomInfo.openActions())
                    .provideCollection(ElementPath.EMPTY, dependencyProvider, HANDLER);

            rooms.add(new Room(mapOrigin, roomInfo, openActions));
        }

        return rooms;
    }

    private List<Round> buildRounds(List<RoundInfo> roundInfoList, List<Spawnpoint> spawnpoints,
            DependencyProvider dependencyProvider, SpawnDistributor spawnDistributor) {
        List<Round> rounds = new ArrayList<>(roundInfoList.size());
        for (RoundInfo roundInfo : roundInfoList) {
            List<Action<Round>> startActions = contextManager.makeContext(roundInfo.startActions())
                    .provideCollection(ElementPath.EMPTY, dependencyProvider, HANDLER);

            List<Action<Round>> endActions = contextManager.makeContext(roundInfo.endActions())
                    .provideCollection(ElementPath.EMPTY, dependencyProvider, HANDLER);

            List<WaveInfo> waveInfo = roundInfo.waves();
            List<Wave> waves = new ArrayList<>(waveInfo.size());
            for (WaveInfo wave : roundInfo.waves()) {
                List<Action<List<PhantazmMob>>> spawnActions = contextManager.makeContext(wave.spawnActions())
                        .provideCollection(ElementPath.EMPTY, dependencyProvider, HANDLER);

                waves.add(new Wave(wave, spawnActions));
            }

            rounds.add(new Round(roundInfo, waves, startActions, endActions, spawnDistributor, spawnpoints));
        }

        return rounds;
    }

    @Memoize
    @Depend
    public static class Module implements DependencyModule, MapObjects.Module {
        private final KeyParser keyParser;
        private final Instance instance;
        private final Random random;
        private final Supplier<? extends RoundHandler> roundHandlerSupplier;
        private final Flaggable flaggable;
        private final TransactionModifierSource transactionModifierSource;
        private final SlotDistributor slotDistributor;
        private final Map<? super UUID, ? extends ZombiesPlayer> playerMap;
        private final Pos respawnPos;
        private final Supplier<? extends MapObjects> mapObjectsSupplier;
        private final Wrapper<PowerupHandler> powerupHandler;
        private final Wrapper<WindowHandler> windowHandler;
        private final Wrapper<EventNode<Event>> eventNode;
        private final MobStore mobStore;
        private final SongPlayer songPlayer;
        private final Team corpseTeam;

        private Module(KeyParser keyParser, Instance instance, Random random,
                Supplier<? extends RoundHandler> roundHandlerSupplier, Flaggable flaggable,
                TransactionModifierSource transactionModifierSource, SlotDistributor slotDistributor,
                Map<? super UUID, ? extends ZombiesPlayer> playerMap, Pos respawnPos,
                Supplier<? extends MapObjects> mapObjectsSupplier, Wrapper<PowerupHandler> powerupHandler,
                Wrapper<WindowHandler> windowHandler, Wrapper<EventNode<Event>> eventNode, MobStore mobStore,
                SongPlayer songPlayer, Team corpseTeam) {
            this.keyParser = Objects.requireNonNull(keyParser, "keyParser");
            this.instance = Objects.requireNonNull(instance, "instance");
            this.random = Objects.requireNonNull(random, "random");
            this.roundHandlerSupplier = Objects.requireNonNull(roundHandlerSupplier, "roundHandlerSupplier");
            this.flaggable = Objects.requireNonNull(flaggable, "flaggable");
            this.transactionModifierSource = Objects.requireNonNull(transactionModifierSource, "modifierSource");
            this.slotDistributor = Objects.requireNonNull(slotDistributor, "slotDistributor");
            this.playerMap = Objects.requireNonNull(playerMap, "playerMap");
            this.respawnPos = Objects.requireNonNull(respawnPos, "respawnPos");
            this.mapObjectsSupplier = Objects.requireNonNull(mapObjectsSupplier, "mapObjectsSupplier");
            this.powerupHandler = Objects.requireNonNull(powerupHandler, "powerupHandler");
            this.windowHandler = Objects.requireNonNull(windowHandler, "windowHandler");
            this.eventNode = Objects.requireNonNull(eventNode, "eventNode");
            this.mobStore = Objects.requireNonNull(mobStore, "mobStore");
            this.songPlayer = Objects.requireNonNull(songPlayer, "songPlayer");
            this.corpseTeam = Objects.requireNonNull(corpseTeam, "corpseTeam");
        }

        @Override
        public @NotNull KeyParser keyParser() {
            return keyParser;
        }

        @Override
        public @NotNull Instance instance() {
            return instance;
        }

        @Override
        public @NotNull Random random() {
            return random;
        }

        @Override
        public @NotNull Supplier<? extends RoundHandler> roundHandlerSupplier() {
            return roundHandlerSupplier;
        }

        @Override
        public @NotNull Flaggable flags() {
            return flaggable;
        }

        @Override
        public @NotNull TransactionModifierSource modifierSource() {
            return transactionModifierSource;
        }

        @Override
        public @NotNull SlotDistributor slotDistributor() {
            return slotDistributor;
        }

        @Override
        public @NotNull Map<? super UUID, ? extends ZombiesPlayer> playerMap() {
            return playerMap;
        }

        @Override
        public @NotNull Collection<? extends ZombiesPlayer> playerCollection() {
            return playerMap.values();
        }

        @Override
        public @NotNull Pos respawnPos() {
            return respawnPos;
        }

        @Override
        public @NotNull Supplier<? extends MapObjects> mapObjectsSupplier() {
            return mapObjectsSupplier;
        }

        @Override
        public @NotNull Supplier<? extends PowerupHandler> powerupHandler() {
            return powerupHandler;
        }

        @Override
        public @NotNull Supplier<? extends WindowHandler> windowHandler() {
            return windowHandler;
        }

        @Override
        public @NotNull Supplier<? extends EventNode<Event>> eventNode() {
            return eventNode;
        }

        @Override
        public @NotNull MobStore mobStore() {
            return mobStore;
        }

        @Override
        public @NotNull SongPlayer songPlayer() {
            return songPlayer;
        }

        @Override
        public @NotNull Team corpseTeam() {
            return corpseTeam;
        }
    }
}
