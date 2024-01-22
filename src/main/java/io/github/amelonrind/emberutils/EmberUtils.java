package io.github.amelonrind.emberutils;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class EmberUtils implements ClientModInitializer {
    public static final String MOD_ID = "emberutils";

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull MutableText translatable(String key) {
        return Text.translatable(MOD_ID + "." + key);
    }

    @Override
    public void onInitializeClient() {
        // nothing here for now
    }

}
