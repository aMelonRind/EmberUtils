package io.github.amelonrind.emberutils;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LoreHelper {
    private static final LoreHelper EMPTY = new LoreHelper(new NbtList()) {
        @Override
        public MutableText get(int index) {
            return null;
        }

        @Override
        public @Nullable String getString(int index) {
            return null;
        }

        @Override
        public boolean startsWith(int index, String prefix) {
            return false;
        }
    };
    public final NbtList raw;
    public final int size;

    @NotNull
    public static LoreHelper from(@NotNull ItemStack item) {
        NbtCompound display = item.getSubNbt("display");
        if (display == null || !display.contains("Lore", NbtElement.LIST_TYPE)) return EMPTY;
        NbtList list = display.getList("Lore", NbtElement.STRING_TYPE);
        if (list.isEmpty()) return EMPTY;
        return new LoreHelper(list);
    }

    private LoreHelper(NbtList lore) {
        raw = lore;
        size = raw.size();
    }

    @Nullable
    public MutableText get(int index) {
        if (index < 0) index = size + index;
        String str = raw.getString(index);
        if (str.isEmpty()) return null;
        try {
            return Text.Serialization.fromJson(str);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public String getString(int index) {
        Text text = get(index);
        return text == null ? null : text.getString();
    }

    public boolean startsWith(int index, String prefix) {
        Text text = get(index);
        return text != null && text.getString().startsWith(prefix);
    }

}
