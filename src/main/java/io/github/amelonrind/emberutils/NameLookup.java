package io.github.amelonrind.emberutils;

import com.google.gson.*;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.amelonrind.emberutils.EmberUtils.mc;

public class NameLookup {
    private static final Map<String, Item> map = new HashMap<>(1356);

    static {
        load();
    }

    private static void load() {
        EmberUtils.LOGGER.info("Loading nameLookup map");
        map.put("火藥粉", Items.GUNPOWDER);
        map.put("黃金劍", Items.GOLDEN_SWORD);

        var supplier = mc.getDefaultResourcePack().open(ResourceType.CLIENT_RESOURCES, new Identifier("minecraft:lang/zh_tw.json"));
        if (supplier == null) {
            EmberUtils.LOGGER.warn("Unable to load default zh_tw.json");
            return;
        }
        try (InputStream is = supplier.get();
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)
        ) {
            JsonObject lang = JsonParser.parseReader(reader).getAsJsonObject();
            List<Map.Entry<String, JsonElement>> blocks = new ArrayList<>(1750);
            for (var ent : lang.entrySet()) {
                if (!ent.getValue().isJsonPrimitive()) continue;
                JsonPrimitive jp = ent.getValue().getAsJsonPrimitive();
                if (!jp.isString()) continue;

                if (ent.getKey().startsWith("item.minecraft.")) {
                    insert(jp.getAsString(), ent.getKey().substring("item.minecraft.".length()));
                } else if (ent.getKey().startsWith("block.minecraft.")) {
                    blocks.add(ent);
                }
            }
            for (var ent : blocks) {
                insert(ent.getValue().getAsString(), ent.getKey().substring("block.minecraft.".length()));
            }
            EmberUtils.LOGGER.info("Loaded nameLookup map");
        } catch (Exception e) {
            EmberUtils.LOGGER.warn("Unable to load default zh_tw.json", e);
        }
    }

    private static void insert(String text, String id) {
        Item item = Registries.ITEM.get(new Identifier(id));
        if (item == Items.AIR) return;
        map.putIfAbsent(text, item);
        if (text.contains(" ")) {
            text = text.replaceAll("\\s", "");
            map.putIfAbsent(text, item);
        }
    }

    @Nullable
    public static Item lookup(String name) {
        return map.get(name);
    }

    @Nullable
    @SuppressWarnings("unused")
    public static String lookupId(String name) {
        Item item = lookup(name);
        return item == null ? null : Registries.ITEM.getId(item).toString();
    }

}
