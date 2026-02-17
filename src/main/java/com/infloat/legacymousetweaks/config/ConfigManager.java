package com.infloat.legacymousetweaks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static MouseTweaksConfig config = new MouseTweaksConfig();

    private ConfigManager() {
    }

    public static MouseTweaksConfig get() {
        if (config == null) {
            config = new MouseTweaksConfig();
        }
        return config;
    }

    public static void load() {
        File file = getConfigFile();
        if (!file.exists()) {
            config = new MouseTweaksConfig();
            save();
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            MouseTweaksConfig loaded = GSON.fromJson(reader, MouseTweaksConfig.class);
            config = loaded == null ? new MouseTweaksConfig() : loaded;
        } catch (IOException e) {
            System.err.println("[LegacyMouseTweaks] Failed to read config: " + e);
            config = new MouseTweaksConfig();
        }
    }

    public static void save() {
        File file = getConfigFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            System.err.println("[LegacyMouseTweaks] Failed to create config directory: " + parent);
            return;
        }

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(get(), writer);
        } catch (IOException e) {
            System.err.println("[LegacyMouseTweaks] Failed to save config: " + e);
        }
    }

    private static File getConfigFile() {
        MinecraftClient mc = MinecraftClient.getInstance();
        File gameDir = mc != null ? mc.runDirectory : new File(".");
        return new File(new File(gameDir, "config"), "legacymousetweaks.json");
    }
}
