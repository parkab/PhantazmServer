package com.github.phantazmnetwork.zombies.audience;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class ChatComponentSender implements ComponentSender {
    @Override
    public void send(@NotNull Audience audience, @NotNull Component component) {
        audience.sendMessage(component);
    }
}
