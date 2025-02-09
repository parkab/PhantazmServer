package org.phantazm.server;

import com.github.steanky.element.core.context.ContextManager;
import com.github.steanky.element.core.key.KeyParser;
import com.github.steanky.ethylene.codec.yaml.YamlCodec;
import com.github.steanky.ethylene.core.ConfigCodec;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.unimi.dsi.fastutil.booleans.BooleanObjectPair;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.DynamicChunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.scoreboard.TeamManager;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.phantazm.core.BasicClientBlockHandlerSource;
import org.phantazm.core.InstanceClientBlockHandler;
import org.phantazm.core.VecUtils;
import org.phantazm.core.equipment.LinearUpgradePath;
import org.phantazm.core.equipment.NoUpgradePath;
import org.phantazm.core.game.scene.RouterStore;
import org.phantazm.core.game.scene.SceneTransferHelper;
import org.phantazm.core.game.scene.fallback.SceneFallback;
import org.phantazm.core.guild.party.Party;
import org.phantazm.core.instance.AnvilFileSystemInstanceLoader;
import org.phantazm.core.instance.InstanceLoader;
import org.phantazm.core.item.AnimatedUpdatingItem;
import org.phantazm.core.item.StaticUpdatingItem;
import org.phantazm.core.particle.ParticleWrapper;
import org.phantazm.core.particle.data.*;
import org.phantazm.core.player.PlayerViewProvider;
import org.phantazm.proxima.bindings.minestom.InstanceSpawner;
import org.phantazm.proxima.bindings.minestom.Spawner;
import org.phantazm.stats.zombies.*;
import org.phantazm.zombies.Attributes;
import org.phantazm.zombies.command.ZombiesCommand;
import org.phantazm.zombies.corpse.CorpseCreator;
import org.phantazm.zombies.map.FileSystemMapLoader;
import org.phantazm.zombies.map.Loader;
import org.phantazm.zombies.map.MapInfo;
import org.phantazm.zombies.map.action.door.DoorPlaySoundAction;
import org.phantazm.zombies.map.action.door.DoorSendMessageAction;
import org.phantazm.zombies.map.action.door.DoorSendOpenedRoomsAction;
import org.phantazm.zombies.map.action.room.SpawnMobsAction;
import org.phantazm.zombies.map.action.round.AnnounceRoundAction;
import org.phantazm.zombies.map.action.round.RevivePlayersAction;
import org.phantazm.zombies.map.action.round.SelectBombedRoom;
import org.phantazm.zombies.map.action.round.SpawnPowerupAction;
import org.phantazm.zombies.map.action.wave.SelectPowerupZombieAction;
import org.phantazm.zombies.map.shop.display.*;
import org.phantazm.zombies.map.shop.display.creator.*;
import org.phantazm.zombies.map.shop.gui.InteractingClickHandler;
import org.phantazm.zombies.map.shop.interactor.*;
import org.phantazm.zombies.map.shop.predicate.*;
import org.phantazm.zombies.map.shop.predicate.logic.AndPredicate;
import org.phantazm.zombies.map.shop.predicate.logic.NotPredicate;
import org.phantazm.zombies.map.shop.predicate.logic.OrPredicate;
import org.phantazm.zombies.map.shop.predicate.logic.XorPredicate;
import org.phantazm.zombies.mob.BasicMobSpawnerSource;
import org.phantazm.zombies.mob.MobSpawnerSource;
import org.phantazm.zombies.player.BasicZombiesPlayerSource;
import org.phantazm.zombies.player.ZombiesPlayer;
import org.phantazm.zombies.player.condition.EquipmentCondition;
import org.phantazm.zombies.powerup.FileSystemPowerupLoader;
import org.phantazm.zombies.powerup.PowerupInfo;
import org.phantazm.zombies.powerup.action.*;
import org.phantazm.zombies.powerup.predicate.ImmediateDeactivationPredicate;
import org.phantazm.zombies.powerup.predicate.TimedDeactivationPredicate;
import org.phantazm.zombies.powerup.action.BossBarTimerAction;
import org.phantazm.zombies.powerup.visual.HologramVisual;
import org.phantazm.zombies.powerup.visual.ItemVisual;
import org.phantazm.zombies.scene.ZombiesScene;
import org.phantazm.zombies.scene.ZombiesSceneProvider;
import org.phantazm.zombies.scene.ZombiesSceneRouter;
import org.phantazm.zombies.sidebar.SidebarUpdater;
import org.phantazm.zombies.sidebar.lineupdater.*;
import org.phantazm.zombies.sidebar.lineupdater.condition.StateConditionCreator;
import org.phantazm.zombies.sidebar.lineupdater.creator.CoinsUpdaterCreator;
import org.phantazm.zombies.sidebar.lineupdater.creator.ConditionalUpdaterCreator;
import org.phantazm.zombies.sidebar.lineupdater.creator.StateUpdaterCreator;
import org.phantazm.zombies.sidebar.lineupdater.creator.ZombieKillsUpdaterCreator;
import org.phantazm.zombies.sidebar.section.CollectionSidebarSection;
import org.phantazm.zombies.sidebar.section.ZombiesPlayerSection;
import org.phantazm.zombies.sidebar.section.ZombiesPlayersSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class ZombiesFeature {
    public static final Path MAPS_FOLDER = Path.of("./zombies/maps");
    public static final Path POWERUPS_FOLDER = Path.of("./zombies/powerups");
    public static final Path INSTANCES_FOLDER = Path.of("./zombies/instances");

    private static final Logger LOGGER = LoggerFactory.getLogger(ZombiesFeature.class);

    private static Map<Key, MapInfo> maps;
    private static Map<Key, PowerupInfo> powerups;
    private static MobSpawnerSource mobSpawnerSource;
    private static ZombiesSceneRouter sceneRouter;
    private static ExecutorService databaseExecutor;
    private static HikariDataSource dataSource;
    private static ZombiesDatabase database;

    static void initialize(@NotNull EventNode<Event> globalEventNode, @NotNull ContextManager contextManager,
            @NotNull Map<BooleanObjectPair<String>, ConfigProcessor<?>> processorMap, @NotNull Spawner spawner,
            @NotNull KeyParser keyParser,
            @NotNull Function<? super Instance, ? extends InstanceSpawner.InstanceSettings> instanceSpaceFunction,
            @NotNull PlayerViewProvider viewProvider, @NotNull CommandManager commandManager,
            @NotNull SceneFallback sceneFallback, @NotNull Map<? super UUID, ? extends Party> parties,
            @NotNull RouterStore routerStore) throws IOException {
        Attributes.registerAll();
        registerElementClasses(contextManager);

        ConfigCodec codec = new YamlCodec();
        ZombiesFeature.maps = loadFeature("map", new FileSystemMapLoader(MAPS_FOLDER, codec));
        ZombiesFeature.powerups = loadFeature("powerup", new FileSystemPowerupLoader(POWERUPS_FOLDER, codec));
        ZombiesFeature.mobSpawnerSource = new BasicMobSpawnerSource(processorMap, spawner, keyParser);

        InstanceLoader instanceLoader =
                new AnvilFileSystemInstanceLoader(MinecraftServer.getInstanceManager(), INSTANCES_FOLDER,
                        DynamicChunk::new);

        LOGGER.info("Preloading {} map instances", maps.size());
        for (MapInfo map : maps.values()) {
            instanceLoader.preload(map.settings().instancePath(),
                    VecUtils.toPoint(map.settings().origin().add(map.settings().spawn())),
                    map.settings().chunkLoadRange());
        }

        Map<Key, ZombiesSceneProvider> providers = new HashMap<>(maps.size());
        TeamManager teamManager = MinecraftServer.getTeamManager();

        // https://bugs.mojang.com/browse/MC-87984
        Team mobNoPushTeam =
                teamManager.createBuilder("mobNoPush").collisionRule(TeamsPacket.CollisionRule.PUSH_OTHER_TEAMS)
                        .build();
        Team corpseTeam = teamManager.createBuilder("corpses").collisionRule(TeamsPacket.CollisionRule.NEVER)
                .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER).build();

        databaseExecutor = Executors.newSingleThreadExecutor();
        HikariConfig config = new HikariConfig("./zombies.hikari.properties");
        dataSource = new HikariDataSource(config);
        ZombiesSQLFetcher sqlFetcher = new JooqZombiesSQLFetcher();
        database = new SQLZombiesDatabase(databaseExecutor, dataSource, sqlFetcher);
        for (Map.Entry<Key, MapInfo> entry : maps.entrySet()) {
            ZombiesSceneProvider provider =
                    new ZombiesSceneProvider(20, instanceSpaceFunction, entry.getValue(), instanceLoader, sceneFallback,
                            globalEventNode, ZombiesFeature.mobSpawnerSource(), MobFeature.getModels(),
                            new BasicClientBlockHandlerSource(instance -> {
                                DimensionType dimensionType = instance.getDimensionType();
                                return new InstanceClientBlockHandler(instance, dimensionType.getMinY(),
                                        dimensionType.getHeight());
                            }, globalEventNode), contextManager, keyParser, mobNoPushTeam, corpseTeam, database,
                            ZombiesFeature.powerups(),
                            new BasicZombiesPlayerSource(EquipmentFeature::createEquipmentCreator,
                                    MobFeature.getModels()),
                            mapDependencyProvider -> contextManager.makeContext(entry.getValue().corpse())
                                    .provide(mapDependencyProvider));
            providers.put(entry.getKey(), provider);
        }

        ZombiesFeature.sceneRouter = new ZombiesSceneRouter(providers);

        MinecraftServer.getSchedulerManager()
                .scheduleTask(() -> sceneRouter.tick(System.currentTimeMillis()), TaskSchedule.immediate(),
                        TaskSchedule.nextTick());

        SceneTransferHelper transferHelper = new SceneTransferHelper(routerStore);
        commandManager.register(
                new ZombiesCommand(parties, sceneRouter, keyParser, maps, viewProvider, transferHelper, sceneFallback));
    }

    private static void registerElementClasses(ContextManager contextManager) {
        //Action<Room>, Action<Round>, Action<Door>, and Action<Wave>
        contextManager.registerElementClass(AnnounceRoundAction.class);
        contextManager.registerElementClass(RevivePlayersAction.class);
        contextManager.registerElementClass(SelectBombedRoom.class);
        contextManager.registerElementClass(SpawnMobsAction.class);
        contextManager.registerElementClass(SpawnPowerupAction.class);
        contextManager.registerElementClass(DoorSendMessageAction.class);
        contextManager.registerElementClass(DoorSendOpenedRoomsAction.class);
        contextManager.registerElementClass(SelectPowerupZombieAction.class);
        contextManager.registerElementClass(DoorPlaySoundAction.class);
        contextManager.registerElementClass(org.phantazm.zombies.map.action.round.PlaySoundAction.class);

        //ShopPredicate
        contextManager.registerElementClass(StaticCostPredicate.class);
        contextManager.registerElementClass(MapFlagPredicate.class);
        contextManager.registerElementClass(InteractingPredicate.class);
        contextManager.registerElementClass(PlayerFlagPredicate.class);
        contextManager.registerElementClass(PlayerInGamePredicate.class);
        contextManager.registerElementClass(PlayerStatePredicate.class);
        contextManager.registerElementClass(UuidPredicate.class);
        contextManager.registerElementClass(TypePredicate.class);

        contextManager.registerElementClass(AndPredicate.class);
        contextManager.registerElementClass(OrPredicate.class);
        contextManager.registerElementClass(NotPredicate.class);
        contextManager.registerElementClass(XorPredicate.class);

        contextManager.registerElementClass(EquipmentCostPredicate.class);
        contextManager.registerElementClass(EquipmentSpacePredicate.class);
        contextManager.registerElementClass(EquipmentPresentPredicate.class);

        //ShopInteractor
        contextManager.registerElementClass(MapFlaggingInteractor.class);
        contextManager.registerElementClass(PlayerFlaggingInteractor.class);
        contextManager.registerElementClass(MessagingInteractor.class);
        contextManager.registerElementClass(PlaySoundInteractor.class);
        contextManager.registerElementClass(DelayedInteractor.class);
        contextManager.registerElementClass(ConditionalInteractor.class);
        contextManager.registerElementClass(OpenGuiInteractor.class);
        contextManager.registerElementClass(CloseGuiInteractor.class);
        contextManager.registerElementClass(AddEquipmentInteractor.class);
        contextManager.registerElementClass(ChangeDoorStateInteractor.class);
        contextManager.registerElementClass(DeductCoinsInteractor.class);
        contextManager.registerElementClass(RefillAmmoInteractor.class);
        contextManager.registerElementClass(UpdateBlockPropertiesInteractor.class);
        contextManager.registerElementClass(SubstitutedTitleInteractor.class);
        contextManager.registerElementClass(TitleInteractor.class);

        //ShopDisplay
        contextManager.registerElementClass(StaticHologramDisplay.class);
        contextManager.registerElementClass(PlayerDisplay.class);
        contextManager.registerElementClass(StaticItemDisplay.class);
        contextManager.registerElementClass(ConditionalDisplay.class);
        contextManager.registerElementClass(FlagConditionalDisplay.class);
        contextManager.registerElementClass(IncrementalMetaDisplay.class);
        contextManager.registerElementClass(AnimatedItemDisplay.class);
        contextManager.registerElementClass(EmptyDisplay.class);

        contextManager.registerElementClass(EquipmentUpgradeCostDisplayCreator.class);
        contextManager.registerElementClass(IncrementalMetaPlayerDisplayCreator.class);
        contextManager.registerElementClass(PlayerHologramDisplayCreator.class);
        contextManager.registerElementClass(PlayerConditionalDisplayCreator.class);
        contextManager.registerElementClass(InteractionPoint.class);
        contextManager.registerElementClass(CombiningArmorPlayerDisplayCreator.class);

        //Sidebar
        contextManager.registerElementClass(SidebarUpdater.class);
        contextManager.registerElementClass(CollectionSidebarSection.class);
        contextManager.registerElementClass(ZombiesPlayersSection.class);
        contextManager.registerElementClass(ZombiesPlayerSection.class);
        contextManager.registerElementClass(ConditionalSidebarLineUpdater.class);
        contextManager.registerElementClass(ConditionalSidebarLineUpdater.ChildUpdater.class);
        contextManager.registerElementClass(ConstantSidebarLineUpdater.class);
        contextManager.registerElementClass(DateLineUpdater.class);
        contextManager.registerElementClass(JoinedPlayersSidebarLineUpdater.class);
        contextManager.registerElementClass(PlayerStateSidebarLineUpdater.class);
        contextManager.registerElementClass(RemainingZombiesSidebarLineUpdater.class);
        contextManager.registerElementClass(RoundSidebarLineUpdater.class);
        contextManager.registerElementClass(TicksLineUpdater.class);

        contextManager.registerElementClass(CoinsUpdaterCreator.class);
        contextManager.registerElementClass(ZombieKillsUpdaterCreator.class);
        contextManager.registerElementClass(ConditionalUpdaterCreator.class);
        contextManager.registerElementClass(StateUpdaterCreator.class);

        contextManager.registerElementClass(StateConditionCreator.class);

        //ClickHandlers
        contextManager.registerElementClass(InteractingClickHandler.class);

        //UpdatingItem
        contextManager.registerElementClass(StaticUpdatingItem.class);
        contextManager.registerElementClass(AnimatedUpdatingItem.class);

        //Particles
        contextManager.registerElementClass(ParticleWrapper.class);
        contextManager.registerElementClass(BlockParticleVariantData.class);
        contextManager.registerElementClass(BlockPositionSource.class);
        contextManager.registerElementClass(DustParticleVariantData.class);
        contextManager.registerElementClass(ItemStackParticleVariantData.class);
        contextManager.registerElementClass(NoParticleVariantData.class);
        contextManager.registerElementClass(TransitionDustParticleVariantData.class);
        contextManager.registerElementClass(VibrationParticleVariantData.class);


        //UpgradePath
        contextManager.registerElementClass(LinearUpgradePath.class);
        contextManager.registerElementClass(NoUpgradePath.class);

        //DeactivationPredicates
        contextManager.registerElementClass(TimedDeactivationPredicate.class);
        contextManager.registerElementClass(ImmediateDeactivationPredicate.class);

        //PowerupVisuals
        contextManager.registerElementClass(ItemVisual.class);
        contextManager.registerElementClass(HologramVisual.class);
        contextManager.registerElementClass(BossBarTimerAction.class);

        //PowerupActions
        contextManager.registerElementClass(MapFlaggingAction.class);
        contextManager.registerElementClass(PlayerFlaggingAction.class);
        contextManager.registerElementClass(MapTransactionModifierMultiplyAction.class);
        contextManager.registerElementClass(MapTransactionModifierAddAction.class);
        contextManager.registerElementClass(KillAllInRadiusAction.class);
        contextManager.registerElementClass(PlaySoundAction.class);
        contextManager.registerElementClass(ModifyWindowsAction.class);
        contextManager.registerElementClass(SendTitleAction.class);
        contextManager.registerElementClass(SendMessageAction.class);
        contextManager.registerElementClass(AttributeModifierAction.class);
        contextManager.registerElementClass(PreventAmmoDrainAction.class);

        //Player conditions
        contextManager.registerElementClass(EquipmentCondition.class);

        //Corpses
        contextManager.registerElementClass(CorpseCreator.class);
        contextManager.registerElementClass(CorpseCreator.TimeLine.class);
        contextManager.registerElementClass(CorpseCreator.StaticLine.class);

        LOGGER.info("Registered Zombies element classes.");
    }

    private static <T extends Keyed> Map<Key, T> loadFeature(String featureName, Loader<T> loader) throws IOException {
        List<String> dataNames = loader.loadableData();
        Map<Key, T> data = new HashMap<>(dataNames.size());

        for (String dataName : dataNames) {
            try {
                T feature = loader.load(dataName);
                Key id = feature.key();

                if (data.containsKey(id)) {
                    LOGGER.warn("Found duplicate " + featureName + " with id " + id + "; the previously loaded " +
                            "version will be overwritten");
                }

                data.put(id, feature);
            }
            catch (IOException e) {
                LOGGER.warn("Exception when loading " + featureName, e);
            }
        }

        LOGGER.info("Loaded " + data.size() + " " + featureName + "s");
        return Map.copyOf(data);
    }

    public static @NotNull @Unmodifiable Map<Key, MapInfo> maps() {
        return FeatureUtils.check(maps);
    }

    public static @NotNull @Unmodifiable Map<Key, PowerupInfo> powerups() {
        return FeatureUtils.check(powerups);
    }

    public static @NotNull MobSpawnerSource mobSpawnerSource() {
        return FeatureUtils.check(mobSpawnerSource);
    }

    public static @NotNull Optional<ZombiesScene> getPlayerScene(@NotNull UUID playerUUID) {
        return FeatureUtils.check(sceneRouter).getCurrentScene(playerUUID);
    }

    public static @NotNull Optional<ZombiesPlayer> getZombiesPlayer(@NotNull UUID playerUUID) {
        return FeatureUtils.check(sceneRouter).getCurrentScene(playerUUID)
                .map(scene -> scene.getZombiesPlayers().get(playerUUID));
    }

    public static @NotNull ZombiesSceneRouter zombiesSceneRouter() {
        return FeatureUtils.check(sceneRouter);
    }

    public static @NotNull ZombiesDatabase getDatabase() {
        return FeatureUtils.check(database);
    }

    public static void end() {
        if (dataSource != null) {
            dataSource.close();
        }

        if (databaseExecutor != null) {
            databaseExecutor.shutdown();

            try {
                LOGGER.info(
                        "Shutting down database executor. Please allow for one minute before shutdown " + "completes.");
                if (!databaseExecutor.awaitTermination(1L, TimeUnit.MINUTES)) {
                    databaseExecutor.shutdownNow();

                    LOGGER.warn(
                            "Not all database tasks completed. Please allow for one minute for tasks to be canceled.");
                    if (!databaseExecutor.awaitTermination(1L, TimeUnit.MINUTES)) {
                        LOGGER.warn("Database tasks failed to cancel.");
                    }
                }
            }
            catch (InterruptedException e) {
                databaseExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
