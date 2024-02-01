package io.github.amelonrind.emberutils.feature;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import io.github.amelonrind.emberutils.EmberUtils;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.github.amelonrind.emberutils.EmberUtils.LOGGER;

public class Notifier {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final File file = FabricLoader.getInstance().getConfigDir().resolve(EmberUtils.MOD_ID).resolve("notifier.json").toFile();
    private static final Notifier def = new Notifier();
    private static Notifier instance = def;
    public final Map<String, Long> factories = new HashMap<>();
    public final Map<String, List<Long>> smiths = new HashMap<>();
    private static long soonest = Long.MAX_VALUE;

    public static Notifier instance() {
        return instance;
    }

    public static void load() {
        try (FileReader reader = new FileReader(file)) {
            instance = gson.fromJson(reader, Notifier.class);
        } catch (FileNotFoundException e) {
            instance = def;
            save();
        } catch (IOException | JsonSyntaxException | JsonIOException e) {
            LOGGER.error("[EmberUtils Notifier] Couldn't read file, loading default.", e);
            instance = def;
        }
        updateSoonest();
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(instance, writer);
        } catch (IOException ignore) {}
    }

    public static void tick(long now) {
        if (now < soonest) return;
        if (!FactoryNotifier.check(now)) SmithingNotifier.check(now);
        updateSoonest();
    }

    public static void minSoonest(long value) {
        if (value < soonest) soonest = value;
    }

    public static void updateSoonest() {
        soonest = Stream.concat(
                instance.factories.values().stream(),
                instance.smiths.values().stream().flatMap(m -> m.stream().filter(t -> t != 0))
        ).min(Long::compareTo).orElse(Long.MAX_VALUE);
    }

}
