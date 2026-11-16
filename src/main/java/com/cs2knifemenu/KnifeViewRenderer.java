package com.cs2knifemenu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.cs2knifemenu.core.KnifeData;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * VERSION-SENSITIVE: draws the knife in first person. This is one of only two files that touch
 * Minecraft's rendering internals (the other is ItemInHandRendererMixin).
 */
public final class KnifeViewRenderer {
    private KnifeViewRenderer() {}
    private static float[] scratch = new float[0];

    /** @return true if the knife was drawn and vanilla hand rendering should be skipped. */
    public static boolean render(PoseStack ps, SubmitNodeCollector collector, int light) {
        KnifeController.Loaded l = KnifeController.I.active();
        if (l == null) return false;
        l.player.update(KnifeController.I.frameDt());
        final float[] bones = l.player.matrices().clone();
        final KnifeData data = l.player.data;
        Config c = Config.get();

        ps.pushPose();
        ps.translate(c.x, c.y, c.z);
        ps.mulPose(Axis.YP.rotationDegrees(c.yaw));
        ps.mulPose(Axis.XP.rotationDegrees(c.pitch));
        float s = c.scale / 16f;
        ps.scale(c.mirror ? -s : s, s, s);
        ps.translate(-l.ax, -l.ay, -l.az);
        collector.submitCustomGeometry(ps, RenderType.entityCutoutNoCull(l.info.texture),
                (pose, vc) -> emit(data, bones, pose, vc, light));
        ps.popPose();
        return true;
    }

    private static void emit(KnifeData data, float[] m, PoseStack.Pose pose, VertexConsumer vc, int light) {
        for (int b = 0; b < data.bones.length; b++) {
            KnifeData.Bone bone = data.bones[b];
            if (!bone.hasMesh()) continue;
            int o = b * 16, nv = bone.verts.length / 8;
            if (scratch.length < nv * 6) scratch = new float[nv * 6];
            float[] t = scratch, v = bone.verts;
            for (int i = 0; i < nv; i++) {
                int s = i * 8;
                float x = v[s], y = v[s + 1], z = v[s + 2], nx = v[s + 3], ny = v[s + 4], nz = v[s + 5];
                t[i * 6]     = m[o]     * x + m[o + 1] * y + m[o + 2]  * z + m[o + 3];
                t[i * 6 + 1] = m[o + 4] * x + m[o + 5] * y + m[o + 6]  * z + m[o + 7];
                t[i * 6 + 2] = m[o + 8] * x + m[o + 9] * y + m[o + 10] * z + m[o + 11];
                t[i * 6 + 3] = m[o]     * nx + m[o + 1] * ny + m[o + 2]  * nz;
                t[i * 6 + 4] = m[o + 4] * nx + m[o + 5] * ny + m[o + 6]  * nz;
                t[i * 6 + 5] = m[o + 8] * nx + m[o + 9] * ny + m[o + 10] * nz;
            }
            for (int idx : bone.indices) {
                int s = idx * 8, k = idx * 6;
                vc.addVertex(pose, t[k], t[k + 1], t[k + 2])
                        .setColor(255, 255, 255, 255)
                        .setUv(v[s + 6], v[s + 7])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light)
                        .setNormal(pose, t[k + 3], t[k + 4], t[k + 5]);
            }
        }
    }
}
