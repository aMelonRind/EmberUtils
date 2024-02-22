package io.github.amelonrind.emberutils;

import com.google.common.collect.ImmutableSet;
import net.minecraft.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;

public class ItemRecord extends LinkedHashMap<Item, Integer> {

    @SuppressWarnings("unused")
    public void addAll(@NotNull ItemRecord other) {
        for (var ent : other.entrySet()) {
            add(ent.getKey(), ent.getValue());
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public int add(Item key, int value) {
        int val = getOrDefault(key, 0) + value;
        put(key, val);
        return val;
    }

    @SuppressWarnings("unused")
    public void removeZeros() {
        if (!values().contains(0)) return;
        for (Item key : ImmutableSet.copyOf(keySet())) remove(key, 0);
    }

}
