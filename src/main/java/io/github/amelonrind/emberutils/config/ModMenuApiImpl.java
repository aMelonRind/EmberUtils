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
//                .image(new Identifier("emberutils:description_images/test.png"), 256, 256)
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

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return p -> {
            Config cfg = Config.get();
            return YetAnotherConfigLib.createBuilder()
                    .title(translatable("title"))
                    .category(ConfigCategory.createBuilder()
                            .name(translatable("category"))
                            .option(optionOf("enchantmentTooltipFix", () -> cfg.enchantmentTooltipFix, val -> cfg.enchantmentTooltipFix = val))
                            .option(optionOf("mmoItemNameFix", () -> cfg.mmoItemNameFix, val -> cfg.mmoItemNameFix = val))
                            .option(optionOf("centeredItemNameBackground", () -> cfg.centeredItemNameBackground, val -> cfg.centeredItemNameBackground = val))
                            .option(optionOf("prettierItemName", () -> cfg.prettierItemName, val -> cfg.prettierItemName = val))
                            .option(optionOf("runeTooltip", () -> cfg.runeTooltip, val -> cfg.runeTooltip = val))
                            .option(optionOf("keepChat", () -> cfg.keepChat, val -> cfg.keepChat = val))
                            .build())
                    .save(Config.HANDLER::save)
                    .build()
                    .generateScreen(p);
        };
    }

}
