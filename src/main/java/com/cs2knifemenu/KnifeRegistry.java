package com.cs2knifemenu;

import com.cs2knifemenu.core.KnifeData;
import com.google.gson.*;
import net.minecraft.resources.Identifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Knife list (knives.json) and lazily-loaded models. Everything is read from the mod jar's resources. */
public final class KnifeRegistry {
    public static final class Info {
        public String id, name, model;
        public boolean skin, hasIcon;
        public int iconW, iconH;
        public char color; // minecraft colour code of skins, or 0
        public Identifier texture, icon;
    }

    private static final List<Info> ALL = new ArrayList<>();
    private static final Map<String, Info> BY_ID = new HashMap<>();
    private static final Map<String, KnifeData> MODELS = new HashMap<>();

    public static void load() {
        try (InputStream in = KnifeRegistry.class.getResourceAsStream("/assets/" + Ids.NS + "/knives.json")) {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement e : root.getAsJsonArray("knives")) {
                JsonObject o = e.getAsJsonObject();
                Info i = new Info();
                i.id = o.get("id").getAsString();
                i.name = o.get("name").getAsString();
                i.model = o.get("model").getAsString();
                i.skin = o.get("skin").getAsBoolean();
                i.hasIcon = o.get("icon").getAsBoolean();
                i.iconW = o.get("iw").getAsInt();
                i.iconH = o.get("ih").getAsInt();
                JsonElement col = o.get("color");
                i.color = (col == null || col.isJsonNull()) ? 0 : col.getAsString().charAt(0);
                i.texture = Ids.of("textures/knife/" + i.id + ".png");
                i.icon = Ids.of("textures/icon/" + i.id + ".png");
                ALL.add(i);
                BY_ID.put(i.id, i);
            }
        } catch (Exception ex) {
            throw new RuntimeException("cs2knifemenu: could not read knives.json", ex);
        }
    }

    public static List<Info> all() { return ALL; }
    public static Info get(String id) { return BY_ID.get(id); }
    public static Info first() { return ALL.get(0); }

    public static synchronized KnifeData model(Info info) {
        return MODELS.computeIfAbsent(info.model, m -> {
            try (InputStream in = KnifeRegistry.class.getResourceAsStream("/assets/" + Ids.NS + "/knives/" + m + ".bin")) {
                return KnifeData.read(in);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
    }
}
