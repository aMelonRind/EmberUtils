package io.github.amelonrind.emberutils.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import io.github.amelonrind.emberutils.EmberUtils;
import io.github.amelonrind.emberutils.feature.KeepChat;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModMenuApiImpl implements ModMenuApi {

    @Contract(value = "_ -> new", pure = true)
    private static @NotNull MutableText translatable(String key) {
        return EmberUtils.translatable("settings." + key);
    }

    @Contract(value = "_ -> new", pure = true)
    private static @NotNull OptionDescription descriptionOf(String key) {
        return OptionDescription.createBuilder()
                .text(translatable(key + ".description"))
                .build();
    }

    private static Option<Boolean> optionOf(String name, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(translatable(name))
                .description(descriptionOf(name))
                .binding(true, getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }

    private static Option<Boolean> optionOf(String name, int imageW, int imageH, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(translatable(name))
                .description(OptionDescription.createBuilder()
                        .text(translatable(name + ".description"))
                        .image(new Identifier("emberutils:description_images/" + name.toLowerCase() + ".png"), imageW, imageH)
                        .build())
                .binding(true, getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return p -> {
            Config cfg = Config.get();
            return YetAnotherConfigLib.createBuilder()
                    .title(translatable("title"))
                    .category(ConfigCategory.createBuilder()
                            .name(translatable("category.visuals"))
                            .option(optionOf("enchantmentTooltipFix", 199, 223, () -> cfg.enchantmentTooltipFix, val -> cfg.enchantmentTooltipFix = val))
                            .option(optionOf("durabilityDisplayFix", 275, 189, () -> cfg.durabilityDisplayFix, val -> cfg.durabilityDisplayFix = val))
                            .option(optionOf("mmoItemNameFix", 305, 94, () -> cfg.mmoItemNameFix, val -> cfg.mmoItemNameFix = val))
                            .option(optionOf("centeredItemNameBackground", 379, 175, () -> cfg.centeredItemNameBackground, val -> cfg.centeredItemNameBackground = val))
                            .option(optionOf("prettierItemName", 383, 204, () -> cfg.prettierItemName, val -> cfg.prettierItemName = val))
                            .option(optionOf("trailingTooltipFix", () -> cfg.trailingTooltipFix, val -> cfg.trailingTooltipFix = val))
                            .option(optionOf("preventDoubleDurability", () -> cfg.preventDoubleDurability, val -> cfg.preventDoubleDurability = val))
                            .option(optionOf("visibleBossbar", () -> cfg.visibleBossbar, val -> cfg.visibleBossbar = val))
                            .option(ListOption.<String>createBuilder()
                                    .name(translatable("visibleBossbarNames"))
                                    .description(descriptionOf("visibleBossbarNames"))
                                    .binding(Config.HANDLER.defaults().visibleBossbarNames, () -> cfg.visibleBossbarNames, val -> cfg.visibleBossbarNames = val)
                                    .controller(StringControllerBuilder::create)
                                    .build())
                            .build())
                    .category(ConfigCategory.createBuilder()
                            .name(translatable("category.utils"))
                            .option(optionOf("gemstoneTooltip", 393, 468, () -> cfg.gemstoneTooltip, val -> cfg.gemstoneTooltip = val))
                            .option(optionOf("deliveryHelper", () -> cfg.deliveryHelper, val -> cfg.deliveryHelper = val))
                            .option(Option.<Integer>createBuilder()
                                    .name(translatable("deliveryItemMultiplier"))
                                    .description(descriptionOf("deliveryItemMultiplier"))
                                    .binding(1, () -> cfg.deliveryItemMultiplier, val -> cfg.deliveryItemMultiplier = val)
                                    .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                            .range(1, 32)
                                            .step(1)
                                            .formatValue(val -> Text.literal(val + "x")))
                                    .build())
                            .option(optionOf("keepChat", () -> cfg.keepChat, val -> {
                                KeepChat.isClearingChatWithF3D = false;
                                cfg.keepChat = val;
                            }))
                            .option(optionOf("purchaseBlocker", () -> cfg.purchaseBlocker, val -> cfg.purchaseBlocker = val))
                            .option(optionOf("autoSelectNextItem", () -> cfg.autoSelectNextItem, val -> cfg.autoSelectNextItem = val))
                            .option(optionOf("factoryNotification", () -> cfg.factoryNotification, val -> cfg.factoryNotification = val))
                            .option(optionOf("smithingNotification", () -> cfg.smithingNotification, val -> cfg.smithingNotification = val))
                            .option(optionOf("requestsAttention", () -> cfg.requestsAttention, val -> cfg.requestsAttention = val))
                            .option(Option.<Boolean>createBuilder()
                                    .name(translatable("uraniumHud"))
                                    .description(descriptionOf("uraniumHud"))
                                    .binding(false, () -> cfg.uraniumHud, val -> cfg.uraniumHud = val)
                                    .controller(TickBoxControllerBuilder::create)
                                    .build())
                            .build())
                    .save(Config.HANDLER::save)
                    .build()
                    .generateScreen(p);
        };
    }

}
