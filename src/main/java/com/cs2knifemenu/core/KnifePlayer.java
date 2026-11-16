package com.cs2knifemenu.core;

import java.util.function.Consumer;

/**
 * Plays knife animations: a looping idle base plus one-shot animations (draw / inspect / attacks)
 * that cross-fade in and fall back to the idle loop. Time is in seconds.
 */
public final class KnifePlayer {
    public final KnifeData data;
    private final Pose pose, from, scratch;
    private final float[] matrices, tmp = new float[16];
    private KnifeData.Anim idle, main;
    private float idleTime, mainTime, prevMainTime, fade, fadeLen;
    private boolean snapshotPending;
    private Consumer<String> soundSink = s -> {};

    public KnifePlayer(KnifeData data) {
        this.data = data;
        int n = data.bones.length;
        pose = new Pose(n); from = new Pose(n); scratch = new Pose(n);
        matrices = new float[n * 16];
        idle = data.anims.get("static_idle");
        if (idle == null) idle = data.anims.get("idle");
    }

    public void setSoundSink(Consumer<String> sink) { this.soundSink = sink; }
    public boolean has(String anim) { return data.anims.containsKey(anim); }
    public boolean isBusy() { return main != null; }
    public String current() { return main == null ? "static_idle" : main.name; }

    /** Starts a one-shot animation. {@code blend} = cross-fade seconds from the pose currently shown. */
    public boolean play(String name, float blend) {
        KnifeData.Anim a = data.anims.get(name);
        if (a == null) return false;
        from.copyFrom(pose);
        fade = 0; fadeLen = blend;
        main = a; mainTime = 0; prevMainTime = -1e-4f;
        return true;
    }

    public void stop() { main = null; fadeLen = 0; }

    public void update(float dt) {
        if (idle != null) {
            idleTime += dt;
            if (idle.length > 0) idleTime %= idle.length;
        }
        if (main != null) {
            prevMainTime = mainTime;
            mainTime += dt;
            fade += dt;
            for (int i = 0; i < main.soundTime.length; i++)
                if (main.soundTime[i] > prevMainTime && main.soundTime[i] <= mainTime) soundSink.accept(main.soundName[i]);
            if (mainTime >= main.length) {
                // hand over to idle with a short fade so the end pose doesn't pop
                from.copyFrom(pose);
                fade = 0; fadeLen = 0.12f; main = null;
            }
        } else if (fadeLen > 0) {
            fade += dt;
            if (fade >= fadeLen) fadeLen = 0;
        }
        evaluate();
    }

    private void evaluate() {
        pose.reset();
        if (idle != null) pose.sample(idle, idleTime);
        if (main != null) pose.sample(main, Math.min(mainTime, main.length));
        if (fadeLen > 0 && fade < fadeLen) {
            float a = fade / fadeLen; a = a * a * (3 - 2 * a);
            pose.blendFrom(from, a);
        }
        pose.buildMatrices(data, matrices, tmp);
    }

    /** Row-major bone matrices in model space; valid after {@link #update}. */
    public float[] matrices() { return matrices; }
}
