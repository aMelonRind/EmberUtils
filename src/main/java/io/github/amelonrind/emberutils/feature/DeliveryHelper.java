package io.github.amelonrind.emberutils.feature;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.amelonrind.emberutils.*;
import io.github.amelonrind.emberutils.config.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.amelonrind.emberutils.EmberUtils.mc;

public class DeliveryHelper {
    private static final String TITLE = "\uE500\uE500\uE500\uE500\uE500\uE500\uE500\uE500߈";
    private static final Text DUPLICATES_TEXT = EmberUtils.translatable("delivery_duplicates");
    private static final Text DONE_TEXT = EmberUtils.translatable("delivery_done");
    private static final Text ILLEGAL_TIME_TEXT = EmberUtils.translatable("illegal_time");
    private static final int[] emptySlots = new int[]{4, 36, 40, 41, 42, 43, 44};
    private static final int[] buttonSlots = new int[]{0, 1, 2, 3, 5, 6, 7, 8, 37, 38, 39}; // cmd 10007
    private static final int[] possibleDeliverySlots = new int[]{ // cmd 5831
            9,  10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35
    };
    private static final Map<String, Integer> illegalTimes = Map.of(
            "標準", 60 * 60000,
            "快速", 10 * 60000,
            "閃電", 60000
    );

    @SuppressWarnings("unused")
    public static boolean disableGuiCheck = false;
    private static List<Delivery> current = new ArrayList<>();
    private static ItemRecord duplicates = new ItemRecord();
    private static ItemRecord items = new ItemRecord();
    private static Set<Item> blacklist = Set.of();
    private static boolean isChatOpen = false;
    private static boolean invDirty = false;
    private static boolean isSneaking = false;

    public static void onConfigChanged(Config cfg) {
        if (!cfg.deliveryHelper && !current.isEmpty()) {
            updateCurrent(new ArrayList<>(), 0L);
        }
        multiplier = isSneaking ? 1 : cfg.deliveryItemMultiplier;
        blacklist = Set.copyOf(cfg.deliveryBlacklist);
        if (!current.isEmpty()) {
            List<Delivery> filtered = new ArrayList<>(current);
            if (filtered.removeIf(Delivery::isBlackListed)) {
                updateCurrent(filtered, System.currentTimeMillis());
            }
        }
    }

    public static void onInventoryUpdate() {
        invDirty = true;
    }

    public static void onOpenScreen(Screen screen) {
        isChatOpen = (screen instanceof ChatScreen);
    }

    public static void onCloseGui(String title, GenericContainerScreenHandler handler) {
        if (!Config.get().deliveryHelper || disableGuiCheck) return;
        if (!TITLE.equals(title)) return;

        Inventory inv = handler.getInventory();
        for (int slot : buttonSlots) {
            ItemStack item = inv.getStack(slot);
            if (item.isEmpty() || !item.isOf(Items.PAPER)) return;
            NbtCompound nbt = item.getNbt();
            if (nbt == null || nbt.getInt("CustomModelData") != 10007) return;
        }
        for (int slot : emptySlots) if (!inv.getStack(slot).isEmpty()) return;

        long now = System.currentTimeMillis();
        List<Delivery> deliveries = new ArrayList<>();
        boolean hasIllegalTime = false;
        for (int slot : possibleDeliverySlots) {
            ItemStack item = inv.getStack(slot);
            if (item.isEmpty()) continue;
            Delivery deli = Delivery.fromItem(item, now);
            if (deli == null) {
                EmberUtils.logTranslatableChat("cant_parse_delivery", slot);
                StringBuilder msg = new StringBuilder("cannot parse delivery\n");
                msg.append(item.getName().getString());
                for (Text text : LoreHelper.from(item)) msg.append("\n").append(text.getString());
                EmberUtils.LOGGER.warn(msg.toString());
                continue;
            }
            if (!hasIllegalTime) {
                long time = deli.getNearestTime(now) - now - 10000;
                if (time > illegalTimes.getOrDefault(deli.tier, 60 * 60000)) hasIllegalTime = true;
            }
            if (!deli.isBlackListed()) deliveries.add(deli);
        }
        if (hasIllegalTime) EmberUtils.logChat(ILLEGAL_TIME_TEXT);
        if (!deliveries.isEmpty()) updateCurrent(deliveries, now);
    }

    public static void tick() {
        if (mc.player != null && mc.player.isSneaking()) {
            if (!isSneaking) {
                isSneaking = true;
                multiplier = 1;
            }
        } else if (isSneaking) {
            isSneaking = false;
            multiplier = Config.get().deliveryItemMultiplier;
            if (!idCache.isEmpty()) idCache.clear();
        }
        if (invDirty) {
            invDirty = false;
            if (mc.player != null) {
                ItemRecord res = new ItemRecord();
                for (ItemStack item : mc.player.getInventory().main) {
                    if (item.isEmpty()) continue;
                    res.add(item.getItem(), item.getCount());
                }
                items = res;
            }
        }
    }

    static boolean shouldUpdateCountdown = true;
    static long lastCountdownSec = 0;
    public static void halfSec(long halfSecs) {
        long countdownSec = halfSecs / 2;
        if (lastCountdownSec != countdownSec) {
            lastCountdownSec = countdownSec;
            shouldUpdateCountdown = true;
        }
    }

    public static void onResolutionChanged() {
        idScale = 1.0f / EmberUtils.guiScale;
        h = EmberUtils.screenHeight / 3;
    }

    static int h = 69;
    static int multiplier = 1;
    static int x2Cache = 0;
    static int y2Cache = 0;
    static float idScale = 1.0f;
    public static void render(DrawContext context) {
        if (current.isEmpty() || isChatOpen) return;
        long now = System.currentTimeMillis();
        int x = 10;

        drawLeftSideRect(context, h - 10, x2Cache, h + y2Cache);
        for (Delivery deli : current) {
            deli.render(context, x, h, now);
            x += 80;
        }
        if (!duplicates.isEmpty()) {
            int y = h;
            context.drawTextWithShadow(mc.textRenderer, DUPLICATES_TEXT, x, y, 0xFFFFFF);
            y += 16;
            for (var ent : duplicates.entrySet()) {
                int goal = ent.getValue() * multiplier;
                int left = goal - items.getOrDefault(ent.getKey(), 0); // potential optimization here?
                drawItem(context, ent.getKey(), x, y, goal, left);
                y += 20;
            }
        }
        shouldUpdateCountdown = false;
    }

    private static final Map<Item, String> idCache = new HashMap<>();
    private static void drawItem(@NotNull DrawContext context, @NotNull Item item, int x, int y, int goal, int left) {
        MatrixStack mat = context.getMatrices();
        mat.push();
        context.drawItem(item.getDefaultStack(), x, y);
        if (left > 0) context.drawTextWithShadow(mc.textRenderer, "-" + left, x + 24, y + 4, 0xFF7F00);
        else context.drawTextWithShadow(mc.textRenderer, DONE_TEXT, x + 24, y + 4, 0x00FF00);
        mat.translate(0.0F, 0.0F, 200.0F);
        String goalStr = Integer.toString(goal);
        context.drawTextWithShadow(mc.textRenderer, goalStr, Math.max(0, x + 17 - mc.textRenderer.getWidth(goalStr)), y + 9, 0xFFFFFF);
        if (isSneaking) {
            mat.translate(x + 24, y + 15, 0);
            mat.scale(idScale, idScale, idScale);
            context.drawText(mc.textRenderer, idCache.computeIfAbsent(item, it -> Registries.ITEM.getId(it).getPath()), 0, 0, 0xFFFFFF, false);
        }
        mat.pop();
    }

    // stolen code
    private static void drawLeftSideRect(@NotNull DrawContext context, int y1, int x2, int y2) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        buf.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        buf.vertex(matrix, 0, y2, 0).color(0x30, 0x38, 0x42, 0x7F).next();
        buf.vertex(matrix, x2, y2, 0).color(0x30, 0x38, 0x42, 0x7F).next();
        buf.vertex(matrix, 0, y1, 0).color(0x30, 0x38, 0x42, 0x7F).next();
        buf.vertex(matrix, x2, y1, 0).color(0x30, 0x38, 0x42, 0x7F).next();
        tess.draw();

        RenderSystem.disableBlend();

        matrices.pop();
    }

    public static void updateCurrent(@NotNull List<Delivery> deliveries, long now) {
        duplicates.clear();
        if (deliveries.isEmpty()) {
            current = deliveries;
            return;
        }
        deliveries.sort(Comparator.comparingLong(d -> d.getNearestTime(now)));
        ItemRecord occurrences = new ItemRecord();
        ItemRecord counts = new ItemRecord();
        for (Delivery d : deliveries) {
            for (var ent : d.items.entrySet()) {
                occurrences.add(ent.getKey(), 1);
                counts.add(ent.getKey(), ent.getValue());
            }
        }
        for (var ent : occurrences.entrySet()) {
            if (ent.getValue() <= 1) counts.remove(ent.getKey());
        }
        current = deliveries;
        duplicates = counts;
        x2Cache = current.size() * 80 + (duplicates.isEmpty() ? 0 : 48) + 20;
        y2Cache = 12
                + 12
                + 12
                + 4
                + current.stream().mapToInt(d -> d.rewards.length * 12 + d.items.size() * 20).max().orElse(0)
                + 10;
        shouldUpdateCountdown = true;
    }

    @Contract(pure = true)
    @SuppressWarnings("unused")
    public static List<Delivery> getCurrent() {
        return List.copyOf(current);
    }

    public static class Delivery {
        private static final Pattern itemPattern = Pattern.compile("^ ▪ (\\d+)x (.+?)((?: .\\uF801)+| mc_\\w+_NOT_FOUND| ?)$");
        private static final Pattern timePattern = Pattern.compile("^(?:結束時間|到期): (-?)(?:(\\d+)m)?\\s?(?:(\\d+)s)?$");
        private static final Map<String, Item> pluginIdentifierExceptions = Map.of("grass", Items.SHORT_GRASS);
        public final String tier;
        public final String scale;
        /**
         * time of day in minutes in GMT time
         */
        public final int time;
        public final ItemRecord items;
        public final String[] rewards;

        private final int tierColor;
        private final String timeString;

        @Nullable
        @SuppressWarnings("unused")
        public static Delivery fromJs(String tier, String scale, int time, Map<String, Integer> items, String rewards) {
            ItemRecord convert = new ItemRecord();
            for (var ent : items.entrySet()) {
                Item item = Registries.ITEM.get(new Identifier(ent.getKey()));
                if (item == Items.AIR) return null;
                convert.put(item, ent.getValue());
            }
            return new Delivery(tier, scale, time, convert, rewards.split("\n"), getTierColor(tier));
        }

        @Nullable
        public static Delivery fromItem(@NotNull ItemStack item, long now) {
            Text name = item.getName();
            int tierColor = name.visit((style, str) -> str.isEmpty() ? Optional.empty() : Optional.of(style), Style.EMPTY)
                    .map(s -> {
                        TextColor cl = s.getColor();
                        return cl == null ? null : cl.getRgb();
                    }).orElse(0xFFFFFF);
            String nameStr = name.getString();
            if (!nameStr.contains("任務")) return null;
            String tier = nameStr.substring(0, 2);
            if (tierColor == 0xFFFFFF) tierColor = getTierColor(tier);

            LoreHelper lore = LoreHelper.from(item);
            if (lore.size < 9) return null; // too short
            Iterator<Text> it = lore.iterator();

            if (!it.hasNext() || !it.next().getString().isEmpty() || !it.hasNext()) return null;
            String temp = it.next().getString();
            if (!it.hasNext()) return null;
            if (temp.startsWith("限時")) temp = temp.substring("限時".length());
            if (!temp.startsWith("任務: ")) return null;
            String scale = temp.substring("任務: ".length());

            ItemRecord items = new ItemRecord();
            temp = it.next().getString();
            if (!it.hasNext()) return null;
            while (temp.startsWith(" ▪ ")) {
                Matcher match = itemPattern.matcher(temp);
                if (!match.matches()) return null;
                int count = Integer.parseInt(match.group(1));
                Item id = null;
                for (String str : match.group(3).split(" ")) {
                    if (str.isBlank()) continue;
                    if (str.length() == 2 && str.charAt(1) == '\uF801') {
                        id = IconLookup.lookup(str.charAt(0));
                        if (id != null) break;
                    } else if (str.startsWith("mc_") && str.endsWith("_NOT_FOUND")) {
                        String sliced = str.substring("mc_".length(), str.length() - "_NOT_FOUND".length());
                        id = Registries.ITEM.get(new Identifier(sliced));
                        if (id == Items.AIR) {
                            id = pluginIdentifierExceptions.get(sliced);
                            if (id != null) break;
                        }
                        else break;
                    }
                }
                if (id == null) {
                    id = NameLookup.lookup(match.group(2));
                    if (id == null) {
                        EmberUtils.LOGGER.info("Unknown id for " + match.group(2));
                        return null;
                    }
                }
                if (items.containsKey(id)) {
                    EmberUtils.LOGGER.info("Duplicate item " + Registries.ITEM.getId(id));
                    return null;
                }
                items.put(id, count);
                temp = it.next().getString();
                if (!it.hasNext()) return null;
            }

            if (!temp.isEmpty()) return null;
            if (!it.next().getString().equals("獎勵:") || !it.hasNext()) return null;

            temp = it.next().getString();
            if (!it.hasNext()) return null;
            List<String> rewards = new ArrayList<>(3);
            while (temp.startsWith(" ▪ ")) {
                rewards.add(temp.substring(" ▪ ".length()).replaceAll("(?<=\\d\\d)\\d\\d\\d(?=-|$)", "k"));
                temp = it.next().getString();
                if (!it.hasNext()) return null;
            }

            if (!temp.isEmpty()) return null;
            temp = it.next().getString();
            if (!it.hasNext()) return null;
            int time = 0;
            Matcher match = timePattern.matcher(temp);
            if (match.matches()) {
                temp = match.group(2);
                if (temp != null) {
                    int minutes = Integer.parseInt(temp);
                    time = minutes * 60;
                    if (Objects.equals(match.group(1), "-")) time = -time;
                }
                temp = match.group(3);
                if (temp != null) {
                    int seconds = Integer.parseInt(temp);
                    time += seconds;
                }
                time *= 1000;
                time = (int) Math.round((now + time + 500.0) / 60000) % 1440; // convert to minutes in day
            }

            if (!it.next().getString().equals("➥ ᎘ 遞交任務")) return null;

            if (it.hasNext()) return null;

            return new Delivery(tier, scale, time, items, rewards.toArray(String[]::new), tierColor);
        }

        @Contract(pure = true)
        private static int getTierColor(@NotNull String tier) {
            return switch (tier) {
                case "標準" -> 0x00FFFF;
                case "快速" -> 0xFF0000;
                case "閃電" -> 0xFFFF00;
                default -> 0xFFFFFF;
            };
        }

        @Contract(pure = true)
        public Delivery(@NotNull String tier, String scale, int time, ItemRecord items, String[] rewards, int tierColor) {
            this.tier = tier;
            this.scale = scale;
            this.time = time;
            this.items = items;
            this.rewards = rewards;
            this.tierColor = tierColor;

            StringBuilder b = new StringBuilder();
            int temp = time / 60 + (time >= 960 ? -16 : 8);
            if (temp < 10) b.append("0");
            b.append(temp).append(":");
            temp = time % 60;
            if (temp < 10) b.append("0");
            b.append(temp);
            timeString = b.toString();
        }

        @SuppressWarnings("unused")
        public String toJson() {
            JsonObject res = new JsonObject();
            res.addProperty("tier", tier);
            res.addProperty("scale", scale);
            res.addProperty("time", time);
            JsonObject its = new JsonObject();
            for (var ent : items.entrySet()) {
                its.addProperty(Registries.ITEM.getId(ent.getKey()).toString(), ent.getValue());
            }
            res.add("items", its);
            res.addProperty("rewards", String.join("\n", rewards));
            res.addProperty("lastSeen", 0);
            res.addProperty("lastSubmit", 0);
            res.add("history", null);
            return res.toString();
        }

        long nearestTimeCache = 0;
        public long getNearestTime(long now) {
            if (Math.abs(nearestTimeCache - now) < 12 * 60 * 60 * 1000) return nearestTimeCache;
            //noinspection IntegerDivisionInFloatingPointContext
            return nearestTimeCache = (Math.round((now / 60000 - time) / 1440.0) * 1440 + time) * 60000;
        }

        public boolean isBlackListed() {
            for (Item item : items.keySet()) {
                if (blacklist.contains(item)) return true;
            }
            return false;
        }

        String countdownText = "";
        int countdownColor = 0xFFFFFF;
        private void render(@NotNull DrawContext context, int x, int y, long now) {
            context.drawTextWithShadow(mc.textRenderer, tier + " " + scale, x, y, tierColor);
            y += 12;
            context.drawTextWithShadow(mc.textRenderer, timeString, x, y, 0xFFFFFF);
            y += 12;
            if (shouldUpdateCountdown) {
                int secs = (int) ((getNearestTime(now) - now) / 1000);
                countdownColor =
                        secs <    0 ? 0xAF3333 :
                        secs <  300 ? 0xFF0000 :
                        secs <  600 ? 0xFF7F00 :
                        secs < 3600 ? 0xFFFFFF :
                        0xFF7F00;
                if (secs < 0) {
                    countdownText = "-";
                    secs = -secs;
                } else countdownText = "";
                if (secs < 60) {
                    countdownText += secs + "s";
                } else {
                    countdownText += (secs / 60) + "m";
                    secs %= 60;
                    if (secs > 0) countdownText += " " + secs + "s";
                }
            }
            context.drawTextWithShadow(mc.textRenderer, countdownText, x, y, countdownColor);

            y += 12;
            for (String reward : rewards) {
                context.drawTextWithShadow(mc.textRenderer, reward, x, y, 0xFFFFFF);
                y += 12;
            }
            y += 4;
            for (var ent : items.entrySet()) {
                int goal = ent.getValue() * multiplier;
                int left = goal - DeliveryHelper.items.getOrDefault(ent.getKey(), 0);
                drawItem(context, ent.getKey(), x, y, goal, left);
                y += 20;
            }
        }

    }

}
