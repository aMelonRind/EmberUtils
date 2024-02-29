package io.github.amelonrind.emberutils.feature;

import io.github.amelonrind.emberutils.EmberUtils;
import io.github.amelonrind.emberutils.config.Config;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class VisibleBossBar {
    private static final PacketConsumer packetConsumer = new PacketConsumer();
    private static final Set<String> strings = new HashSet<>();
    private static final Set<Pattern> patterns = new HashSet<>();
    private static Map<UUID, ClientBossBar> bossbars;

    public static void onConfigChanged(Config cfg) {
        strings.clear();
        patterns.clear();
        for (String str : cfg.visibleBossBarNames) {
            if (str.startsWith("/") && str.endsWith("/") && !str.equals("/")) {
                try {
                    Pattern pat = Pattern.compile(str.substring(1, str.length() - 1));
                    patterns.add(pat);
                } catch (Exception e) {
                    EmberUtils.LOGGER.error("Failed to compile regex", e);
                }
            } else {
                strings.add(str);
            }
        }
    }

    public static void onPacket(BossBarS2CPacket packet, Map<UUID, ClientBossBar> bossbars) {
        if (!Config.get().visibleBossBar) return;
        VisibleBossBar.bossbars = bossbars;
        packet.accept(packetConsumer);
    }

    private static boolean checkName(String name) {
        if (name == null) return false;
        if (strings.contains(name)) return true;
        for (Pattern pat : patterns) if (pat.matcher(name).matches()) return true;
        return false;
    }

    private static void modify(BossBar bb) {
        bb.setColor(BossBar.Color.YELLOW);
        bb.setStyle(BossBar.Style.NOTCHED_10);
    }

    static class PacketConsumer implements BossBarS2CPacket.Consumer {

        @Override
        public void add(UUID uuid, Text name, float percent, BossBar.Color color, BossBar.Style style, boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
            BossBar bb = bossbars.get(uuid);
            if (bb != null && checkName(name.getString())) modify(bb);
        }

        @Override
        public void updateStyle(UUID id, BossBar.Color color, BossBar.Style style) {
            BossBar bb = bossbars.get(id);
            if (bb != null && checkName(bb.getName().getString())) modify(bb);
        }

    }

}
