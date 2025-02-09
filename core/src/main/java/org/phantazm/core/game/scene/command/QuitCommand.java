package org.phantazm.core.game.scene.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.phantazm.core.game.scene.RouterStore;
import org.phantazm.core.game.scene.TransferResult;
import org.phantazm.core.player.PlayerViewProvider;

import java.util.Collections;
import java.util.Objects;

public final class QuitCommand {

    private QuitCommand() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull Command quitCommand(@NotNull RouterStore routerStore,
            @NotNull PlayerViewProvider viewProvider) {
        Objects.requireNonNull(routerStore, "routerStore");
        Objects.requireNonNull(viewProvider, "viewProvider");

        Command command = new Command("quit", "leave", "l");
        command.addConditionalSyntax((sender, commandString) -> {
            if (commandString == null) {
                return sender instanceof Player;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("You have to be a player to use that command!", NamedTextColor.RED));
                return false;
            }

            return true;
        }, (sender, context) -> {
            Player player = (Player)sender;
            routerStore.getCurrentScene(player.getUuid()).ifPresent(scene -> {
                if (!scene.isQuittable()) {
                    sender.sendMessage(Component.text("You can't quit this scene.", NamedTextColor.RED));
                    return;
                }

                TransferResult result = scene.leave(Collections.singleton(player.getUuid()));
                if (result.executor().isPresent()) {
                    result.executor().get().run();
                    scene.getFallback().fallback(viewProvider.fromPlayer(player));
                }
                else {
                    result.message().ifPresent(sender::sendMessage);
                }
            });
        });

        return command;
    }
}
