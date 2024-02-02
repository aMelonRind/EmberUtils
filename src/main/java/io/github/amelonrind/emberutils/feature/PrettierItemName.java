package io.github.amelonrind.emberutils.feature;

import io.github.amelonrind.emberutils.config.Config;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class PrettierItemName {
    private static final String prefix1 = "\uEA6E ";
    private static final String prefix2 = " \uEA72\uEA6F\uEA6E\uEA6C";

    public static void onGetName(CallbackInfoReturnable<Text> cir, Supplier<String> namePrefixSupplier) {
        if (!Config.get().prettierItemName) return;
        String namePrefix = namePrefixSupplier.get();
        if (namePrefix == null) return;
        Text text = cir.getReturnValue();
        Optional<String> start = text.visit(Optional::of);
        if (start.isEmpty()) return;
        String startStr = start.get();
        if (startStr.contains(namePrefix)) return;
        if (startStr.startsWith(" ")) {
            AtomicBoolean isFirst = new AtomicBoolean(true);
            MutableText t = Text.empty();
            text.visit((style, str) -> {
                if (isFirst.get()) {
                    t.append(Text.literal(str.stripLeading()).setStyle(style));
                    isFirst.set(false);
                } else {
                    t.append(Text.literal(str).setStyle(style));
                }
                return Optional.empty();
            }, Style.EMPTY);
            text = t;
        }
        cir.setReturnValue(Text.empty()
                .append(Text.literal(prefix1 + namePrefix + prefix2)
                        .setStyle(Style.EMPTY.withItalic(false).withColor(0xFFFFFF)))
                .append(text)
        );
    }

}
