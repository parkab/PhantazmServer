package org.phantazm.zombies.command;

import com.github.steanky.element.core.key.KeyParser;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.phantazm.core.game.scene.RouteResult;
import org.phantazm.core.game.scene.Scene;
import org.phantazm.core.game.scene.SceneRouter;
import org.phantazm.core.game.scene.TransferResult;
import org.phantazm.core.guild.GuildMember;
import org.phantazm.core.guild.party.Party;
import org.phantazm.core.guild.party.PartyMember;
import org.phantazm.core.player.PlayerView;
import org.phantazm.core.player.PlayerViewProvider;
import org.phantazm.zombies.map.MapInfo;
import org.phantazm.zombies.scene.ZombiesJoinHelper;
import org.phantazm.zombies.scene.ZombiesJoinRequest;
import org.phantazm.zombies.scene.ZombiesRouteRequest;
import org.phantazm.zombies.scene.ZombiesScene;

import java.util.*;
import java.util.function.Function;

public class ZombiesJoinCommand extends Command {
    public ZombiesJoinCommand(@NotNull KeyParser keyParser, @NotNull Map<Key, MapInfo> maps,
            @NotNull ZombiesJoinHelper joinHelper, @NotNull Map<? super UUID, ? extends Party> parties) {
        super("join");

        Argument<String> mapKeyArgument = ArgumentType.String("map-key");

        Objects.requireNonNull(keyParser, "keyParser");
        Objects.requireNonNull(maps, "maps");

        mapKeyArgument.setSuggestionCallback((sender, context, suggestion) -> {
            for (Map.Entry<Key, MapInfo> entry : maps.entrySet()) {
                suggestion.addEntry(
                        new SuggestionEntry(entry.getKey().asString(), entry.getValue().settings().displayName()));
            }
        });
        addConditionalSyntax((sender, commandString) -> {
            if (commandString == null) {
                return sender instanceof Player;
            }

            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("You have to be a player to use that command!", NamedTextColor.RED));
                return false;
            }

            Party party = parties.get(player.getUuid());
            if (party != null) {
                PartyMember member = party.getMemberManager().getMember(player.getUuid());
                if (!party.getJoinPermission().hasPermission(member)) {
                    sender.sendMessage(Component.text("You don't have permission in your party to join games!",
                            NamedTextColor.RED));
                    return false;
                }
            }

            return true;
        }, (sender, context) -> {
            @Subst("test_map")
            String mapKeyString = context.get(mapKeyArgument);
            if (!keyParser.isValidKey(mapKeyString)) {
                sender.sendMessage(Component.text("Invalid key!", NamedTextColor.RED));
                return;
            }

            Key targetMap = keyParser.parseKey(mapKeyString);
            if (!maps.containsKey(targetMap)) {
                sender.sendMessage(Component.text("Invalid map!", NamedTextColor.RED));
                return;
            }

            joinHelper.joinGame(((Player)sender), targetMap);
        }, mapKeyArgument);
    }

}
