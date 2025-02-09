package org.phantazm.zombies.player;

import com.github.steanky.toolkit.collection.Wrapper;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.ScoreboardObjectivePacket;
import net.minestom.server.scoreboard.Sidebar;
import net.minestom.server.scoreboard.TabList;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTException;
import org.jglrxavpok.hephaistos.parser.SNBTParser;
import org.phantazm.commons.Activable;
import org.phantazm.commons.BasicTickTaskScheduler;
import org.phantazm.commons.CancellableState;
import org.phantazm.commons.TickTaskScheduler;
import org.phantazm.core.equipment.EquipmentCreator;
import org.phantazm.core.equipment.EquipmentHandler;
import org.phantazm.core.inventory.*;
import org.phantazm.core.player.PlayerView;
import org.phantazm.core.time.PrecisionSecondTickFormatter;
import org.phantazm.core.time.TickFormatter;
import org.phantazm.mob.MobModel;
import org.phantazm.mob.MobStore;
import org.phantazm.mob.spawner.MobSpawner;
import org.phantazm.zombies.coin.BasicPlayerCoins;
import org.phantazm.zombies.coin.BasicTransactionModifierSource;
import org.phantazm.zombies.coin.PlayerCoins;
import org.phantazm.zombies.coin.TransactionModifierSource;
import org.phantazm.zombies.coin.component.BasicTransactionComponentCreator;
import org.phantazm.zombies.corpse.CorpseCreator;
import org.phantazm.zombies.equipment.gun.ZombiesEquipmentModule;
import org.phantazm.zombies.kill.BasicPlayerKills;
import org.phantazm.zombies.kill.PlayerKills;
import org.phantazm.zombies.map.EquipmentGroupInfo;
import org.phantazm.zombies.map.Flaggable;
import org.phantazm.zombies.map.MapSettingsInfo;
import org.phantazm.zombies.map.objects.MapObjects;
import org.phantazm.zombies.player.state.*;
import org.phantazm.zombies.player.state.context.DeadPlayerStateContext;
import org.phantazm.zombies.player.state.context.KnockedPlayerStateContext;
import org.phantazm.zombies.player.state.context.NoContext;
import org.phantazm.zombies.player.state.context.QuitPlayerStateContext;
import org.phantazm.zombies.player.state.revive.KnockedPlayerState;
import org.phantazm.zombies.player.state.revive.NearbyReviverFinder;
import org.phantazm.zombies.player.state.revive.ReviveHandler;
import org.phantazm.zombies.scene.ZombiesScene;
import org.phantazm.stats.zombies.BasicZombiesPlayerMapStats;
import org.phantazm.stats.zombies.ZombiesPlayerMapStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class BasicZombiesPlayerSource implements ZombiesPlayer.Source {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicZombiesPlayerSource.class);

    private final Function<ZombiesEquipmentModule, EquipmentCreator> equipmentCreatorFunction;

    private final Map<Key, MobModel> mobModelMap;

    public BasicZombiesPlayerSource(
            @NotNull Function<ZombiesEquipmentModule, EquipmentCreator> equipmentCreatorFunction,
            @NotNull Map<Key, MobModel> mobModelMap) {
        this.equipmentCreatorFunction = Objects.requireNonNull(equipmentCreatorFunction, "equipmentCreatorFunction");
        this.mobModelMap = Objects.requireNonNull(mobModelMap, "mobModelMap");
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull ZombiesPlayer createPlayer(@NotNull ZombiesScene scene,
            @NotNull Map<? super UUID, ? extends ZombiesPlayer> zombiesPlayers,
            @NotNull MapSettingsInfo mapSettingsInfo, @NotNull Instance instance, @NotNull PlayerView playerView,
            @NotNull TransactionModifierSource mapTransactionModifierSource, @NotNull Flaggable flaggable,
            @NotNull EventNode<Event> eventNode, @NotNull Random random, @NotNull MapObjects mapObjects,
            @NotNull MobStore mobStore, @NotNull MobSpawner mobSpawner, @NotNull CorpseCreator corpseCreator) {
        TransactionModifierSource playerTransactionModifierSource = new BasicTransactionModifierSource();

        ZombiesPlayerMeta meta = new ZombiesPlayerMeta();
        ZombiesPlayerMapStats stats =
                BasicZombiesPlayerMapStats.createBasicStats(playerView.getUUID(), mapSettingsInfo.id());

        PlayerCoins coins = new BasicPlayerCoins(playerView, stats, new BasicTransactionComponentCreator(), 0);
        PlayerKills kills = new BasicPlayerKills(stats);

        InventoryProfile livingProfile = new BasicInventoryProfile(45);

        Map<Key, EquipmentGroupInfo> equipmentGroups = mapSettingsInfo.equipmentGroups();

        Map.Entry<Key, InventoryObjectGroup>[] inventoryObjectGroupEntries = new Map.Entry[equipmentGroups.size()];
        Iterator<Map.Entry<Key, EquipmentGroupInfo>> iterator = equipmentGroups.entrySet().iterator();

        for (int i = 0; i < inventoryObjectGroupEntries.length; i++) {
            Map.Entry<Key, EquipmentGroupInfo> entry = iterator.next();
            EquipmentGroupInfo groupInfo = entry.getValue();

            String defaultItemString = groupInfo.defaultItem();

            ItemStack itemStack = null;
            if (!defaultItemString.isEmpty()) {
                try {
                    if (new SNBTParser(
                            new StringReader(groupInfo.defaultItem())).parse() instanceof NBTCompound compound) {
                        itemStack = ItemStack.fromItemNBT(compound);
                    }
                }
                catch (NBTException e) {
                    LOGGER.warn("Failed to load item in slot {} because its NBT string is invalid: {}", i, e);
                }
            }

            inventoryObjectGroupEntries[i] = Map.entry(entry.getKey(),
                    new BasicInventoryObjectGroup(livingProfile, groupInfo.slots(),
                            itemStack == null ? null : new StaticInventoryObject(itemStack)));
        }

        InventoryAccess livingInventoryAccess =
                new InventoryAccess(livingProfile, Map.ofEntries(inventoryObjectGroupEntries));
        InventoryAccess deadInventoryAccess = new InventoryAccess(new BasicInventoryProfile(45), Map.of());

        InventoryAccessRegistry accessRegistry = new BasicInventoryAccessRegistry(playerView);
        accessRegistry.registerAccess(InventoryKeys.ALIVE_ACCESS, livingInventoryAccess);
        accessRegistry.registerAccess(InventoryKeys.DEAD_ACCESS, deadInventoryAccess);

        EquipmentHandler equipmentHandler = new EquipmentHandler(accessRegistry);

        Wrapper<ZombiesPlayer> zombiesPlayerWrapper = Wrapper.ofNull();
        ZombiesEquipmentModule equipmentModule =
                new ZombiesEquipmentModule(zombiesPlayers, playerView, stats, mobSpawner, mobStore, eventNode, random,
                        mapObjects, zombiesPlayerWrapper, mobModelMap::get);
        EquipmentCreator equipmentCreator = equipmentCreatorFunction.apply(equipmentModule);

        Sidebar sidebar = new Sidebar(
                Component.text(StringUtils.center("ZOMBIES", 16), NamedTextColor.YELLOW, TextDecoration.BOLD));
        TabList tabList = new TabList(UUID.randomUUID().toString(), ScoreboardObjectivePacket.Type.INTEGER);

        Function<NoContext, ZombiesPlayerState> aliveStateCreator = unused -> {
            return new BasicZombiesPlayerState(Component.text("ALIVE"), ZombiesPlayerStateKeys.ALIVE.key(),
                    List.of(new BasicAliveStateActivable(accessRegistry, playerView, sidebar, tabList)));
        };
        BiFunction<DeadPlayerStateContext, Collection<Activable>, ZombiesPlayerState> deadStateCreator =
                (context, activables) -> {
                    List<Activable> combinedActivables = new ArrayList<>(activables);
                    combinedActivables.add(
                            new BasicDeadStateActivable(accessRegistry, context, instance, playerView, mapSettingsInfo,
                                    sidebar, tabList, stats));
                    return new BasicZombiesPlayerState(Component.text("DEAD").color(NamedTextColor.RED),
                            ZombiesPlayerStateKeys.DEAD.key(), combinedActivables);
                };
        Function<KnockedPlayerStateContext, ZombiesPlayerState> knockedStateCreator = context -> {
            TickFormatter tickFormatter = new PrecisionSecondTickFormatter(new PrecisionSecondTickFormatter.Data(1));

            Wrapper<CorpseCreator.Corpse> corpseWrapper = Wrapper.ofNull();
            Supplier<ZombiesPlayerState> deadStateSupplier = () -> {
                DeadPlayerStateContext deathContext = DeadPlayerStateContext.killed(context.getKiller().orElse(null),
                        context.getKnockRoom().orElse(null));
                return deadStateCreator.apply(deathContext,
                        List.of(corpseWrapper.get().asDeathActivable(), new Activable() {
                            @Override
                            public void end() {
                                meta.setCorpse(null);
                            }
                        }));
            };

            ReviveHandler reviveHandler =
                    new ReviveHandler(() -> aliveStateCreator.apply(NoContext.INSTANCE), deadStateSupplier,
                            new NearbyReviverFinder(zombiesPlayers, playerView, mapSettingsInfo.reviveRadius()), 500L);

            CorpseCreator.Corpse corpse =
                    corpseCreator.forPlayer(instance, zombiesPlayerWrapper.get(), context.getKnockLocation(),
                            reviveHandler);

            corpseWrapper.set(corpse);
            return new KnockedPlayerState(reviveHandler,
                    List.of(new BasicKnockedStateActivable(context, instance, playerView, mapSettingsInfo,
                                    reviveHandler, tickFormatter, sidebar, tabList, stats), corpse.asKnockActivable(),
                            new Activable() {
                                @Override
                                public void start() {
                                    meta.setCorpse(corpse);
                                }
                            }));
        };
        Map<UUID, CancellableState> stateMap = new ConcurrentHashMap<>();
        TickTaskScheduler taskScheduler = new BasicTickTaskScheduler();
        Function<QuitPlayerStateContext, ZombiesPlayerState> quitStateCreator = unused -> {
            return new BasicZombiesPlayerState(Component.text("QUIT").color(NamedTextColor.RED),
                    ZombiesPlayerStateKeys.QUIT.key(),
                    List.of(new BasicQuitStateActivable(instance, playerView, mapSettingsInfo, sidebar, tabList,
                            stateMap, taskScheduler)));
        };

        Map<PlayerStateKey<?>, Function<?, ? extends ZombiesPlayerState>> stateFunctions =
                Map.of(ZombiesPlayerStateKeys.ALIVE, aliveStateCreator, ZombiesPlayerStateKeys.DEAD,
                        (Function<DeadPlayerStateContext, ZombiesPlayerState>)context -> deadStateCreator.apply(context,
                                List.of()), ZombiesPlayerStateKeys.KNOCKED, knockedStateCreator,
                        ZombiesPlayerStateKeys.QUIT, quitStateCreator);

        PlayerStateSwitcher stateSwitcher = new PlayerStateSwitcher();

        ZombiesPlayerModule module =
                new ZombiesPlayerModule(playerView, meta, coins, kills, equipmentHandler, equipmentCreator,
                        accessRegistry, stateSwitcher, stateFunctions, sidebar, tabList, mapTransactionModifierSource,
                        playerTransactionModifierSource, flaggable, stats);

        ZombiesPlayer zombiesPlayer = new BasicZombiesPlayer(scene, module, stateMap, taskScheduler);
        zombiesPlayerWrapper.set(zombiesPlayer);
        return zombiesPlayer;
    }
}
