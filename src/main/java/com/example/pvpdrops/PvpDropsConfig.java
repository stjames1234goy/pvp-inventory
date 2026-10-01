package com.example.pvpdrops;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Settings stored in config/pvpdrops.json (created on first launch).
 * Edit the file and restart, or run "/pvpdrops reload" in game (op only).
 */
public class PvpDropsConfig {
    /** Master switch. false = the mod does nothing (plain vanilla keepInventory). */
    public boolean enabled = true;

    /** Drop the main inventory + hotbar (36 slots) when killed by a player. */
    public boolean dropMainInventory = true;

    /** Drop worn armor when killed by a player. */
    public boolean dropArmor = true;

    /** Drop the offhand item when killed by a player. */
    public boolean dropOffhand = true;

    /** Drop experience (vanilla amount: 7 points per level, max 100) and reset the victim's XP to 0. */
    public boolean dropExperience = true;

    /** Items with Curse of Vanishing are destroyed instead of dropped (vanilla behaviour). */
    public boolean destroyVanishingCurseItems = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("pvpdrops.json");
    }

    /** Loads the config, falling back to defaults on any problem, and rewrites the file so new options appear. */
    public static PvpDropsConfig load() {
        PvpDropsConfig config = new PvpDropsConfig();
        Path file = path();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                PvpDropsConfig loaded = GSON.fromJson(reader, PvpDropsConfig.class);
                if (loaded != null) {
                    config = loaded;
                }
            } catch (Exception e) {
                System.err.println("[pvpdrops] Could not read config, using defaults: " + e);
            }
        }
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(path().getParent());
            try (Writer writer = Files.newBufferedWriter(path())) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("[pvpdrops] Could not write config: " + e);
        }
    }
}
