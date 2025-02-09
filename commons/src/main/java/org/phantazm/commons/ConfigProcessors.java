package org.phantazm.commons;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.kyori.adventure.util.RGBLike;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Contains static {@link ConfigProcessor} implementations used to serialize/deserialize certain common objects.
 */
public final class ConfigProcessors {
    private static final ConfigProcessor<Key> key = new ConfigProcessor<>() {
        @Override
        public Key dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
            try {
                @Subst("key")
                String string = ConfigProcessor.STRING.dataFromElement(element);
                if (string.contains(":")) {
                    return Key.key(string);
                }
                else {
                    return Key.key(Namespaces.PHANTAZM, string);
                }
            }
            catch (InvalidKeyException keyException) {
                throw new ConfigProcessException(keyException);
            }
        }

        @Override
        public @NotNull ConfigElement elementFromData(Key key) {
            if (key.namespace().equals(Namespaces.PHANTAZM)) {
                return ConfigPrimitive.of(key.value());
            }
            return ConfigPrimitive.of(key.asString());
        }
    };
    private static final ConfigProcessor<Component> component = new ConfigProcessor<>() {
        @Override
        public Component dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
            return MiniMessage.miniMessage().deserialize(ConfigProcessor.STRING.dataFromElement(element));
        }

        @Override
        public @NotNull ConfigElement elementFromData(@NotNull Component component) {
            return ConfigPrimitive.of(MiniMessage.miniMessage().serialize(component));
        }
    };
    private static final ConfigProcessor<RGBLike> rgbLike = new ConfigProcessor<>() {
        @Override
        public @NotNull RGBLike dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
            String hex = ConfigProcessor.STRING.dataFromElement(element);
            TextColor color = TextColor.fromHexString(hex);
            if (color == null) {
                throw new ConfigProcessException("Invalid hex: " + hex);
            }

            return color;
        }

        @Override
        public @NotNull ConfigElement elementFromData(@NotNull RGBLike rgbLike) {
            return ConfigPrimitive.of(TextColor.color(rgbLike).asHexString());
        }
    };
    private static final ConfigProcessor<TitlePart<Component>> componentTitlePart = new ConfigProcessor<>() {
        @Override
        public TitlePart<Component> dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
            String name = ConfigProcessor.STRING.dataFromElement(element);
            return switch (name) {
                case "TITLE" -> TitlePart.TITLE;
                case "SUBTITLE" -> TitlePart.SUBTITLE;
                default -> throw new ConfigProcessException("Unrecognized TitlePart " + name);
            };
        }

        @Override
        public @NotNull ConfigElement elementFromData(TitlePart<Component> componentTitlePart)
                throws ConfigProcessException {
            if (componentTitlePart.equals(TitlePart.TITLE)) {
                return ConfigPrimitive.of("TITLE");
            }
            else if (componentTitlePart.equals(TitlePart.SUBTITLE)) {
                return ConfigPrimitive.of("SUBTITLE");
            }

            throw new ConfigProcessException("Unrecognized TitlePart " + componentTitlePart);
        }
    };
    private static final ConfigProcessor<TitlePart<Title.Times>> timesTitlePart =
            ConfigProcessor.emptyProcessor(() -> TitlePart.TIMES);
    private static final ConfigProcessor<Sound.Source> soundSource = ConfigProcessor.enumProcessor(Sound.Source.class);
    private static final ConfigProcessor<Sound> sound = new ConfigProcessor<>() {
        @Override
        public Sound dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
            Key name = key.dataFromElement(element.getElementOrThrow("name"));
            Sound.Source source = soundSource.dataFromElement(element.getElementOrThrow("source"));
            float volume = element.getNumberOrThrow("volume").floatValue();
            float pitch = element.getNumberOrThrow("pitch").floatValue();
            return Sound.sound(name, source, volume, pitch);
        }

        @Override
        public @NotNull ConfigElement elementFromData(Sound sound) throws ConfigProcessException {
            ConfigNode node = new LinkedConfigNode(4);
            node.put("name", key.elementFromData(sound.name()));
            node.put("source", soundSource.elementFromData(sound.source()));
            node.putNumber("volume", sound.volume());
            node.putNumber("pitch", sound.pitch());
            return node;
        }
    };

    private static final ConfigProcessor<UUID> uuid = new ConfigProcessor<>() {
        @Override
        public UUID dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
            try {
                return UUID.fromString(ConfigProcessor.STRING.dataFromElement(element));
            }
            catch (IllegalArgumentException e) {
                throw new ConfigProcessException("invalid UUID string", e);
            }
        }

        @Override
        public @NotNull ConfigElement elementFromData(UUID uuid) {
            return ConfigPrimitive.of(uuid.toString());
        }
    };

    private ConfigProcessors() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the {@link ConfigProcessor} implementation used to serialize/deserialize {@link UUID} objects.
     *
     * @return the ConfigProcessor used to serialize/deserialize UUID instances
     */
    public static @NotNull ConfigProcessor<UUID> uuid() {
        return uuid;
    }

    /**
     * Returns the {@link ConfigProcessor} implementation used to serialize/deserialize {@link Key} objects.
     *
     * @return the ConfigProcessor used to serialize/deserialize Key instances
     */
    public static @NotNull ConfigProcessor<Key> key() {
        return key;
    }

    /**
     * Returns the {@link ConfigProcessor} implementation used to serialize/deserialize {@link Component} objects.
     *
     * @return the ConfigProcessor used to serialize/deserialize Component instances
     */
    public static @NotNull ConfigProcessor<Component> component() {
        return component;
    }

    /**
     * Returns the {@link  ConfigProcessor} implementation used to serialize/deserialize {@link TitlePart} objects of
     * type {@link Component}.
     *
     * @return the ConfigProcessor used to serialize/deserialize TitlePart instances
     */
    public static @NotNull ConfigProcessor<TitlePart<Component>> componentTitlePart() {
        return componentTitlePart;
    }

    public static @NotNull ConfigProcessor<TitlePart<Title.Times>> timesTitlePart() {
        return timesTitlePart;
    }

    /**
     * Returns the {@link ConfigProcessor} implementation used to serialize/deserialize {@link Sound} objects.
     *
     * @return the ConfigProcessor used to serialize/deserialize Sound instances
     */
    public static @NotNull ConfigProcessor<Sound> sound() {
        return sound;
    }

    /**
     * Returns the {@link ConfigProcessor} implementation used to serialize/deserialize {@link Sound.Source} objects.
     *
     * @return the ConfigProcessor used to serialize/deserialize Sound.Source instances
     */
    public static @NotNull ConfigProcessor<Sound.Source> soundSource() {
        return soundSource;
    }

    /**
     * Returns the {@link ConfigProcessor} implementation used to serialize/deserialize {@link RGBLike} objects.
     *
     * @return the ConfigProcessor used to serialize/deserialize RGBLike instances
     */
    public static @NotNull ConfigProcessor<RGBLike> rgbLike() {
        return rgbLike;
    }

}
