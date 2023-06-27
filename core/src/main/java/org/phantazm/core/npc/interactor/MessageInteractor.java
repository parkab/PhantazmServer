package org.phantazm.core.npc.interactor;

import com.github.steanky.element.core.annotation.Cache;
import com.github.steanky.element.core.annotation.DataObject;
import com.github.steanky.element.core.annotation.FactoryMethod;
import com.github.steanky.element.core.annotation.Model;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.mapper.annotation.Default;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

@Model("npc.interactor.message")
@Cache
public class MessageInteractor implements Interactor {
    private final Data data;

    @FactoryMethod
    public MessageInteractor(@NotNull Data data) {
        this.data = data;
    }

    @Override
    public void interact(@NotNull Player player) {
        if (data.broadccast) {
            Instance instance = player.getInstance();
            if (instance != null) {
                instance.sendMessage(data.message);
            }
            else {
                player.sendMessage(data.message);
            }
        }
        else {
            player.sendMessage(data.message);
        }
    }

    @DataObject
    public record Data(@NotNull Component message, boolean broadccast) {
        @Default("broadcast")
        public static @NotNull ConfigElement broadcastDefault() {
            return ConfigPrimitive.of(false);
        }
    }
}
