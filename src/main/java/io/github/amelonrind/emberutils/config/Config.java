package io.github.amelonrind.emberutils.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import io.github.amelonrind.emberutils.EmberUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.List;

public class Config {
    public static final ConfigClassHandler<Config> HANDLER = ConfigClassHandler.createBuilder(Config.class)
            .id(new Identifier(EmberUtils.MOD_ID, "main"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve(EmberUtils.MOD_ID).resolve("settings.json5"))
                    .setJson5(true)
                    .build())
            .build();

    public static Config get() {
        return HANDLER.instance();
    }

    @SerialEntry(comment = "Fixes tooltip texture on enchantments.")
    public boolean enchantmentTooltipFix = true;

    @SerialEntry(comment = "Show durability on not damageable item in vanilla.")
    public boolean durabilityDisplayFix = true;

    @SerialEntry(comment = "Fixes item name carrying tooltip background in chat or above hotbar.")
    public boolean mmoItemNameFix = true;

    @SerialEntry(comment = "Center the MMOItems name background on the tooltip above hotbar. Will override mmoItemNameFix.")
    public boolean centeredItemNameBackground = true;

    @SerialEntry(comment = "Fixes MMOItems that has previously took out from an anvil which will lose name background.")
    public boolean prettierItemName = true;

    @SerialEntry(comment = "Fixes trailing advanced tooltips on MMOItems.")
    public boolean trailingTooltipFix = true;

    @SerialEntry(comment = "Prevents double durability tooltip when the MMOItem has its own.")
    public boolean preventDoubleDurability = true;

    @SerialEntry(comment = "Makes some bossbar visible by changing its style.")
    public boolean visibleBossBar = true;

    @SerialEntry(comment = "The names for Visible BossBar to override on.")
    public List<String> visibleBossBarNames = List.of("不願離去的兔兔");

    @SerialEntry(comment = "Reveals runes on MMOItems' tooltip.")
    public boolean gemstoneTooltip = true;

    @SerialEntry(comment = "Memorizes the items needed to do delivery when delivery screen is closed and then show them on hud.")
    public boolean deliveryHelper = true;

    @SerialEntry
    public int deliveryItemMultiplier = 1;

    @SerialEntry(comment = "Blacklisted items for Delivery Helper.")
    public List<Item> deliveryBlacklist = List.of(Items.DIAMOND_HORSE_ARMOR);

    @SerialEntry(comment = "Keeps chat even if switched server or disconnected. You can still clear chat with F3+D.")
    public boolean keepChat = true;

    @SerialEntry(comment = "Prevents you from accidentally purchasing factories. You'll need to click 3 times to buy the factory.")
    public boolean purchaseBlocker = true;

    @SerialEntry(comment = "Auto select next item when exchanging items with a npc.")
    public boolean autoSelectNextItem = true;

    @SerialEntry(comment = "Notifies in chat when a factory product is complete.")
    public boolean factoryNotification = true;

    @SerialEntry(comment = "Notifies in chat when a smithing item is complete.")
    public boolean smithingNotification = true;

    @SerialEntry(comment = "Requests window attention when notifying.")
    public boolean requestsAttention = true;

    @SerialEntry(comment = "Adds an eye-catching hud to the screen, reminds you to not forget about uranium.")
    public boolean uraniumHud = false;

}
