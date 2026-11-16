package com.cs2knifemenu.core;

/** Per-bone animated values (rotation deg, position, scale) plus keyframe sampling and bone matrix building. */
public final class Pose {
    public final float[] rot, pos, scl;
    public final boolean[] hasRot, hasPos, hasScl;

    public Pose(int bones) {
        rot = new float[bones * 3]; pos = new float[bones * 3]; scl = new float[bones * 3];
        hasRot = new boolean[bones]; hasPos = new boolean[bones]; hasScl = new boolean[bones];
        reset();
    }

    public void reset() {
        java.util.Arrays.fill(rot, 0); java.util.Arrays.fill(pos, 0); java.util.Arrays.fill(scl, 1);
        java.util.Arrays.fill(hasRot, false); java.util.Arrays.fill(hasPos, false); java.util.Arrays.fill(hasScl, false);
    }

    public void copyFrom(Pose o) {
        System.arraycopy(o.rot, 0, rot, 0, rot.length); System.arraycopy(o.pos, 0, pos, 0, pos.length);
        System.arraycopy(o.scl, 0, scl, 0, scl.length);
        System.arraycopy(o.hasRot, 0, hasRot, 0, hasRot.length); System.arraycopy(o.hasPos, 0, hasPos, 0, hasPos.length);
        System.arraycopy(o.hasScl, 0, hasScl, 0, hasScl.length);
    }

    /** Overlay: values for channels animated by {@code anim} at {@code t} replace what is already in this pose. */
    public void sample(KnifeData.Anim anim, float t) {
        float[] out = new float[3];
        for (KnifeData.Channel c : anim.channels) {
            sampleChannel(c, t, out);
            int b = c.bone * 3;
            switch (c.type) {
                case 0 -> { System.arraycopy(out, 0, rot, b, 3); hasRot[c.bone] = true; }
                case 1 -> { System.arraycopy(out, 0, pos, b, 3); hasPos[c.bone] = true; }
                default -> { System.arraycopy(out, 0, scl, b, 3); hasScl[c.bone] = true; }
            }
        }
    }

    /** this = lerp(from, this, a). Used for cross-fading when switching animations. */
    public void blendFrom(Pose from, float a) {
        for (int i = 0; i < rot.length; i++) {
            rot[i] = from.rot[i] + (rot[i] - from.rot[i]) * a;
            pos[i] = from.pos[i] + (pos[i] - from.pos[i]) * a;
            scl[i] = from.scl[i] + (scl[i] - from.scl[i]) * a;
        }
    }

    static void sampleChannel(KnifeData.Channel c, float t, float[] out) {
        int n = c.time.length;
        if (n == 1 || t <= c.time[0]) { System.arraycopy(c.post, 0, out, 0, 3); return; }
        if (t >= c.time[n - 1]) { System.arraycopy(c.post, (n - 1) * 3, out, 0, 3); return; }
        int k = 0;
        while (k < n - 2 && t >= c.time[k + 1]) k++;
        float t0 = c.time[k], t1 = c.time[k + 1];
        float a = t1 > t0 ? (t - t0) / (t1 - t0) : 1f;
        for (int j = 0; j < 3; j++) {
            float p1 = c.post[k * 3 + j], p2 = c.pre[(k + 1) * 3 + j];
            if (c.smooth[k] || c.smooth[k + 1]) {
                float p0 = k > 0 ? c.post[(k - 1) * 3 + j] : p1;
                float p3 = k + 2 < n ? c.pre[(k + 2) * 3 + j] : p2;
                out[j] = 0.5f * ((2 * p1) + (-p0 + p2) * a + (2 * p0 - 5 * p1 + 4 * p2 - p3) * a * a
                        + (-p0 + 3 * p1 - 3 * p2 + p3) * a * a * a);
            } else {
                out[j] = p1 + (p2 - p1) * a;
            }
        }
    }

    /** Row-major 4x4 matrices (16 floats per bone) in model space (Bedrock units). */
    public void buildMatrices(KnifeData d, float[] m, float[] tmp) {
        for (int i = 0; i < d.bones.length; i++) {
            KnifeData.Bone b = d.bones[i];
            // local = T(pivot + pos) * R(rest + anim) * S * T(-pivot)
            Mat.rotationZYX(tmp, b.rot[0] + rot[i * 3], b.rot[1] + rot[i * 3 + 1], b.rot[2] + rot[i * 3 + 2]);
            float sx = scl[i * 3], sy = scl[i * 3 + 1], sz = scl[i * 3 + 2];
            for (int r = 0; r < 3; r++) { tmp[r * 4] *= sx; tmp[r * 4 + 1] *= sy; tmp[r * 4 + 2] *= sz; }
            // translation column = pivot + pos - R*S*pivot
            for (int r = 0; r < 3; r++) {
                tmp[r * 4 + 3] = b.pivot[r] + pos[i * 3 + r]
                        - (tmp[r * 4] * b.pivot[0] + tmp[r * 4 + 1] * b.pivot[1] + tmp[r * 4 + 2] * b.pivot[2]);
            }
            if (b.parent >= 0) Mat.mul(m, i * 16, m, b.parent * 16, tmp, 0);
            else System.arraycopy(tmp, 0, m, i * 16, 16);
        }
    }
}
