package com.cs2knifemenu;

import com.cs2knifemenu.core.KnifeData;
import com.cs2knifemenu.core.KnifePlayer;
import com.cs2knifemenu.screen.KnifeMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** Decides when the knife is shown and which animation plays (draw / attack / inspect). */
public final class KnifeController {
    public static final KnifeController I = new KnifeController();

    public static final class Loaded {
        public KnifePlayer player;
        public KnifeRegistry.Info info;
        public float ax, ay, az; // model-space centre of the knife, used as pivot for placement
    }

    private Loaded loaded;
    private boolean active;
    private ItemStack lastStack = ItemStack.EMPTY;
    private long lastNanos = System.nanoTime();
    private boolean pAttack, pUse, pInspect, pMenu;
    private int attackIdx, lockTicks;
    private final Map<String, SoundEvent> sounds = new HashMap<>();

    private KnifeController() {}

    /** Non-null when the custom knife should replace the vanilla hand rendering this frame. */
    public Loaded active() { return active ? loaded : null; }

    public float frameDt() {
        long now = System.nanoTime();
        float dt = (now - lastNanos) / 1e9f;
        lastNanos = now;
        return Math.max(0f, Math.min(dt, 0.1f));
    }

    public void tick(Minecraft mc) {
        Config cfg = Config.get();
        if (mc.player == null) { active = false; return; }
        boolean free = mc.screen == null;

        boolean menu = free && Input.key(cfg.menuKey);
        if (menu && !pMenu) mc.setScreen(new KnifeMenuScreen());
        pMenu = menu;

        ItemStack held = mc.player.getMainHandItem();
        boolean eligible = cfg.enabled && switch (cfg.showMode) {
            case 0 -> held.is(ItemTags.SWORDS);
            case 1 -> !held.isEmpty();
            default -> true;
        };
        if (!eligible) { active = false; lastStack = held; return; }

        boolean changed = ensureLoaded(cfg);
        if (!active || changed || held != lastStack) {
            loaded.player.play("draw", 0.05f);
            lockTicks = 8;
        }
        active = true;
        lastStack = held;
        if (lockTicks > 0) lockTicks--;

        boolean attack = free && mc.options.keyAttack.isDown();
        boolean use = free && mc.options.keyUse.isDown();
        boolean inspect = free && Input.key(cfg.inspectKey);
        KnifePlayer p = loaded.player;
        String cur = p.current();
        boolean drawing = cur.equals("draw");
        if (attack && !pAttack && !drawing && lockTicks == 0) {
            String a = "melee_" + (attackIdx % 2 + 1);
            if (p.play(a, 0.1f)) { attackIdx++; lockTicks = 6; }
        }
        if (use && !pUse && !drawing && lockTicks == 0) {
            if (p.play("melee_3", 0.1f)) lockTicks = 6;
        }
        if (inspect && !pInspect && !p.isBusy()) p.play("inspect", 0.2f);
        pAttack = attack; pUse = use; pInspect = inspect;
    }

    public void previewInspect() {
        if (loaded != null && active && !loaded.player.isBusy()) loaded.player.play("inspect", 0.2f);
    }

    private boolean ensureLoaded(Config cfg) {
        if (loaded != null && loaded.info.id.equals(cfg.knife)) return false;
        KnifeRegistry.Info info = KnifeRegistry.get(cfg.knife);
        if (info == null) { info = KnifeRegistry.first(); cfg.knife = info.id; }
        KnifeData data = KnifeRegistry.model(info);
        Loaded l = new Loaded();
        l.info = info;
        l.player = new KnifePlayer(data);
        l.player.setSoundSink(this::playSound);
        computeAnchor(l, data);
        loaded = l;
        return true;
    }

    private static void computeAnchor(Loaded l, KnifeData data) {
        KnifePlayer probe = new KnifePlayer(data);
        probe.update(0.0001f);
        float[] m = probe.matrices();
        float[] min = {1e9f, 1e9f, 1e9f}, max = {-1e9f, -1e9f, -1e9f};
        for (int b = 0; b < data.bones.length; b++) {
            KnifeData.Bone bone = data.bones[b];
            for (int v = 0; v < bone.verts.length; v += 8) {
                float x = bone.verts[v], y = bone.verts[v + 1], z = bone.verts[v + 2];
                for (int r = 0; r < 3; r++) {
                    float o = m[b * 16 + r * 4] * x + m[b * 16 + r * 4 + 1] * y + m[b * 16 + r * 4 + 2] * z + m[b * 16 + r * 4 + 3];
                    min[r] = Math.min(min[r], o); max[r] = Math.max(max[r], o);
                }
            }
        }
        l.ax = (min[0] + max[0]) / 2; l.ay = (min[1] + max[1]) / 2; l.az = (min[2] + max[2]) / 2;
    }

    private void playSound(String name) {
        Minecraft mc = Minecraft.getInstance();
        SoundEvent ev = sounds.computeIfAbsent(name, n -> SoundEvent.createVariableRangeEvent(Ids.of(n)));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ev, 1.0f, Config.get().volume));
    }
}
