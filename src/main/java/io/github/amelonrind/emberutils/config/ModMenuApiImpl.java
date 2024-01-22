package io.github.amelonrind.emberutils.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import io.github.amelonrind.emberutils.EmberUtils;
import net.minecraft.text.MutableText;
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
                            .name(translatable("category"))
                            .option(optionOf("enchantmentTooltipFix", 199, 223, () -> cfg.enchantmentTooltipFix, val -> cfg.enchantmentTooltipFix = val))
                            .option(optionOf("durabilityDisplayFix", 275, 189, () -> cfg.durabilityDisplayFix, val -> cfg.durabilityDisplayFix = val))
                            .option(optionOf("mmoItemNameFix", 305, 94, () -> cfg.mmoItemNameFix, val -> cfg.mmoItemNameFix = val))
                            .option(optionOf("centeredItemNameBackground", 379, 175, () -> cfg.centeredItemNameBackground, val -> cfg.centeredItemNameBackground = val))
                            .option(optionOf("prettierItemName", 383, 204, () -> cfg.prettierItemName, val -> cfg.prettierItemName = val))
                            .option(optionOf("gemstoneTooltip", 393, 468, () -> cfg.gemstoneTooltip, val -> cfg.gemstoneTooltip = val))
                            .option(optionOf("keepChat", () -> cfg.keepChat, val -> {
                                EmberUtils.isClearingChatWithF3D = false;
                                cfg.keepChat = val;
                            }))
                            .build())
                    .save(Config.HANDLER::save)
                    .build()
                    .generateScreen(p);
        };
    }

}
