package com.cs2knifemenu;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.nio.file.*;

/** Persistent settings (config/cs2knifemenu.json). */
public final class Config {
    public static final String[] SHOW_MODES = {"Swords", "Any item", "Always"};

    public boolean enabled = true;
    public String knife = "karambit";
    public int showMode = 0;
    public int menuKey = GLFW.GLFW_KEY_K;
    public int inspectKey = GLFW.GLFW_KEY_Y;
    public float volume = 0.8f;
    // viewmodel placement (eye space, blocks / degrees)
    public float x = 0.25f, y = -0.30f, z = -0.60f, yaw = -15f, pitch = 15f, scale = 1.5f;
    public boolean mirror = false;

    private static Config INSTANCE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("cs2knifemenu.json"); }

    public static Config get() {
        if (INSTANCE == null) {
            try {
                if (Files.exists(file())) INSTANCE = GSON.fromJson(Files.readString(file()), Config.class);
            } catch (Exception e) { System.err.println("[cs2knifemenu] could not read config: " + e); }
            if (INSTANCE == null) INSTANCE = new Config();
        }
        return INSTANCE;
    }

    public void save() {
        try { Files.writeString(file(), GSON.toJson(this)); }
        catch (Exception e) { System.err.println("[cs2knifemenu] could not save config: " + e); }
    }

    public void resetPlacement() {
        Config d = new Config();
        x = d.x; y = d.y; z = d.z; yaw = d.yaw; pitch = d.pitch; scale = d.scale; mirror = d.mirror;
    }
}
