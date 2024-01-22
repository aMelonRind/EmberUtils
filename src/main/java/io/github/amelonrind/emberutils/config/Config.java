package io.github.amelonrind.emberutils.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import io.github.amelonrind.emberutils.EmberUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

public class Config {
    public static final ConfigClassHandler<Config> HANDLER = ConfigClassHandler.createBuilder(Config.class)
            .id(new Identifier(EmberUtils.MOD_ID, "main"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve(EmberUtils.MOD_ID + ".json5"))
                    .setJson5(true)
                    .build())
            .build();

    public static Config get() {
        return HANDLER.instance();
    }

    @SerialEntry(comment = "Fixes tooltip texture on enchantments on MMOItems.")
    public boolean enchantmentTooltipFix = true;

    @SerialEntry(comment = "Show durability on not damageable item in vanilla.")
    public boolean durabilityDisplayFix = true;

    @SerialEntry(comment = "Fixes item name carrying tooltip background in chat or above hotbar.")
    public boolean mmoItemNameFix = true;

    @SerialEntry(comment = "Center the MMOItems name background on above hotbar. Will override mmoItemNameFix.")
    public boolean centeredItemNameBackground = true;

    @SerialEntry(comment = "Fixes MMOItems that has previously took out from an anvil which will lose name background.")
    public boolean prettierItemName = true;

    @SerialEntry(comment = "Reveals runes on MMOItems' tooltip.")
    public boolean runeTooltip = true;

    @SerialEntry(comment = "Keeps chat even if switched server or disconnected. You can still clear chat with F3+D.")
    public boolean keepChat = true;

}
