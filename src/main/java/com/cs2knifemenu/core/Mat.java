package com.cs2knifemenu.core;

/** Tiny row-major 4x4 helpers (no JOML so the core stays testable outside Minecraft). */
public final class Mat {
    private Mat() {}

    public static void identity(float[] m) {
        java.util.Arrays.fill(m, 0); m[0] = m[5] = m[10] = m[15] = 1;
    }

    /** Bedrock/Blockbench order: R = Rz * Ry * Rx, angles in degrees. */
    public static void rotationZYX(float[] m, float xd, float yd, float zd) {
        double x = Math.toRadians(xd), y = Math.toRadians(yd), z = Math.toRadians(zd);
        float cx = (float) Math.cos(x), sx = (float) Math.sin(x), cy = (float) Math.cos(y), sy = (float) Math.sin(y),
                cz = (float) Math.cos(z), sz = (float) Math.sin(z);
        m[0] = cz * cy; m[1] = cz * sy * sx - sz * cx; m[2] = cz * sy * cx + sz * sx; m[3] = 0;
        m[4] = sz * cy; m[5] = sz * sy * sx + cz * cx; m[6] = sz * sy * cx - cz * sx; m[7] = 0;
        m[8] = -sy;     m[9] = cy * sx;                m[10] = cy * cx;                m[11] = 0;
        m[12] = 0; m[13] = 0; m[14] = 0; m[15] = 1;
    }

    /** out = a * b (out may not alias a or b unless offsets differ). */
    public static void mul(float[] out, int o, float[] a, int ao, float[] b, int bo) {
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++)
                out[o + r * 4 + c] = a[ao + r * 4] * b[bo + c] + a[ao + r * 4 + 1] * b[bo + 4 + c]
                        + a[ao + r * 4 + 2] * b[bo + 8 + c] + a[ao + r * 4 + 3] * b[bo + 12 + c];
    }
}
