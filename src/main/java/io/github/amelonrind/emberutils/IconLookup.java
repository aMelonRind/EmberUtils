package io.github.amelonrind.emberutils;

import com.google.gson.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.resource.*;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.IntStream;

import static io.github.amelonrind.emberutils.EmberUtils.mc;

public class IconLookup {
    private static final String EMBERCRAFT_PACK_DESC = "EmberCraftTW";
    private static final Set<String> IGNORES = Set.of(
            "minecraft:item/empty_armor_slot_shield.png",
            "minecraft:item/spawn_egg.png",
            "minecraft:item/spawn_egg_overlay.png"
    );
    private static final Int2ObjectMap<Item> FALLBACK = loadFallback();
    private static final Int2ObjectMap<Item> map = new Int2ObjectOpenHashMap<>(370);

    static {
        putExceptions();
    }

    private static Int2ObjectMap<Item> loadFallback() {
        EmberUtils.LOGGER.info("Loading iconLookup fallback");
        Int2ObjectMap<Item> res = new Int2ObjectOpenHashMap<>(367);
        Optional<Resource> resource = mc.getResourceManager().getResource(new Identifier("emberutils:icon_lookup_fallback.json"));
        if (resource.isEmpty()) {
            EmberUtils.LOGGER.warn("Cannot find resource emberutils:icon_lookup_fallback.json");
            return res;
        }
        try (InputStream is = resource.get().getInputStream();
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)
        ) {
            JsonElement json = JsonParser.parseReader(reader);
            if (!json.isJsonObject()) throw new JsonParseException("icon_lookup_fallback should be an object!");
            for (var ent : json.getAsJsonObject().entrySet()) {
                if (!ent.getValue().isJsonPrimitive()) continue;
                JsonPrimitive jp = ent.getValue().getAsJsonPrimitive();
                if (!jp.isString()) continue;

                Item item = Registries.ITEM.get(new Identifier(jp.getAsString()));
                if (item != Items.AIR) res.put(ent.getKey().charAt(0), item);
                else EmberUtils.LOGGER.warn("Invalid item id {}", jp.getAsString());
            }
            EmberUtils.LOGGER.info("Loaded iconLookup fallback");
        } catch (Exception e) {
            EmberUtils.LOGGER.warn("Unable to load resource emberutils:icon_lookup_fallback.json", e);
        }
        return res;
    }

    private static void putExceptions() {
        map.put('', Items.ELYTRA); // broken elytra
        map.put('', Items.SHORT_GRASS); // grass in 1.20.2
    }

    public static void onResourceReload() {
        EmberUtils.LOGGER.info("Loading iconLookup map");
        ResourcePackManager manager = mc.getResourcePackManager();
        manager.scanPacks();

        ResourcePackProfile found = null;
        Collection<ResourcePackProfile> enabled = manager.getEnabledProfiles();
        for (ResourcePackProfile prof : enabled) {
            if (prof.getDescription().getString().equals(EMBERCRAFT_PACK_DESC)) {
                found = prof;
                if (prof.getSource() == ResourcePackSource.SERVER) break;
            }
        }
        if (found == null) for (ResourcePackProfile prof : manager.getProfiles()) {
            if (enabled.contains(prof)) continue;
            if (prof.getDescription().getString().equals(EMBERCRAFT_PACK_DESC)) {
                found = prof;
                break;
            }
        }
        if (found == null) {
            if (map.isEmpty()) EmberUtils.LOGGER.warn("Unable to load icons from EmberCraft resource pack, using fallback");
            return;
        }
        ResourcePack pack = found.createResourcePack();
        var supplier = pack.open(ResourceType.CLIENT_RESOURCES, new Identifier("minecraft:font/uniform.json"));
        if (supplier == null) {
            if (map.isEmpty()) EmberUtils.LOGGER.warn("Unable to load icons from EmberCraft resource pack, using fallback");
            return;
        }
        try (pack;
             InputStream is = supplier.get();
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)
        ) {
            JsonArray prov = JsonParser.parseReader(reader).getAsJsonObject().get("providers").getAsJsonArray();
            map.clear();
            putExceptions();
            for (JsonElement e : prov) {
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                JsonElement fe = o.get("file");
                if (!fe.isJsonPrimitive()) continue;
                JsonPrimitive fp = fe.getAsJsonPrimitive();
                if (!fp.isString()) continue;
                String file = fp.getAsString();
                if (!file.startsWith("minecraft:") || !file.endsWith(".png")) continue;
                if (!file.startsWith("minecraft:block/") && !file.startsWith("minecraft:item/")) continue;
                if (IGNORES.contains(file)) continue;
                if (file.indexOf('/') != file.lastIndexOf('/')) continue;

                JsonElement ce = o.get("chars");
                if (!ce.isJsonArray()) continue;
                JsonArray ca = ce.getAsJsonArray();
                int[] chars = ca.asList().stream().flatMapToInt(el -> {
                    if (!el.isJsonPrimitive()) return IntStream.empty();
                    JsonPrimitive ep = el.getAsJsonPrimitive();
                    if (!ep.isString()) return IntStream.empty();
                    return ep.getAsString().chars();
                }).toArray();
                if (Arrays.stream(chars).allMatch(map::containsKey)) continue;
                Item item = resolveFile(file.substring("minecraft:".length(), file.length() - ".png".length()));
                if (item == null) {
                    EmberUtils.LOGGER.info("Cannot resolve file {} for chars [{}]", file, new String(chars, 0, chars.length));
                    continue;
                }
                for (int c : chars) map.putIfAbsent(c, item);
            }
            EmberUtils.LOGGER.info("Loaded iconLookup map");
        } catch (Exception e) {
            EmberUtils.LOGGER.warn("Unable to load uniform from EmberCraft resource pack", e);
        }
    }

    @Nullable
    private static Item resolveFile(@NotNull String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash == path.length() - 1) return null;
        String full = lastSlash != -1 ? path.substring(lastSlash + 1) : path;
        if (full.isBlank()) return null;
        Item res = Registries.ITEM.get(new Identifier(full));
        if (res != Items.AIR) return res;
        for (int i = full.lastIndexOf('_'); i != -1; i = i == 0 ? -1 : full.lastIndexOf('_', i - 1)) {
            res = Registries.ITEM.get(new Identifier(full.substring(0, i)));
            if (res != Items.AIR) return res;
        }
        if (path.startsWith("block/")) {
            for (int i = full.length(); i != -1; i = i == 0 ? -1 : full.lastIndexOf('_', i - 1)) {
                res = Registries.ITEM.get(new Identifier(full.substring(0, i) + "_block"));
                if (res != Items.AIR) return res;
            }
        }
        return null;
    }

    @Nullable
    public static Item lookup(@NotNull String str) {
        return str.isBlank() ? null : lookup(str.charAt(0));
    }

    @Nullable
    public static Item lookup(int c) {
        Item res = map.get(c);
        return res != null ? res : FALLBACK.get(c);
    }

    @Nullable
    @SuppressWarnings("unused")
    public static String lookupId(@NotNull String str) {
        Item item = lookup(str);
        return item == null ? null : Registries.ITEM.getId(item).toString();
    }

}
