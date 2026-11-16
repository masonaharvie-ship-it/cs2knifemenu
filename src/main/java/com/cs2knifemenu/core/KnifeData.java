package com.cs2knifemenu.core;

import java.io.*;
import java.util.*;

/** Parsed knife model: bone hierarchy, one mesh per bone and all animations. No Minecraft dependencies. */
public final class KnifeData {
    public static final class Bone {
        public String name;
        public int parent;
        public float[] pivot = new float[3], rot = new float[3];
        /** 8 floats per vertex: pos(3) normal(3) uv(2). */
        public float[] verts = new float[0];
        /** Quads: 4 indices per primitive (triangles repeat their last vertex). */
        public int[] indices = new int[0];
        public boolean hasMesh() { return indices.length > 0; }
    }

    public static final class Channel {
        public int bone, type; // type: 0 rotation, 1 position, 2 scale
        public float[] time;
        public float[] pre, post; // 3 per key
        public boolean[] smooth;
    }

    public static final class Anim {
        public String name;
        public float length;
        public float[] soundTime = new float[0];
        public String[] soundName = new String[0];
        public Channel[] channels = new Channel[0];
    }

    public Bone[] bones;
    public final Map<String, Anim> anims = new LinkedHashMap<>();

    public static KnifeData read(InputStream raw) throws IOException {
        DataInputStream in = new DataInputStream(new BufferedInputStream(raw, 1 << 16));
        if (in.readInt() != 0x434B4E32) throw new IOException("bad knife model magic");
        KnifeData d = new KnifeData();
        d.bones = new Bone[in.readShort()];
        for (int i = 0; i < d.bones.length; i++) {
            Bone b = d.bones[i] = new Bone();
            b.name = in.readUTF();
            b.parent = in.readShort();
            for (int k = 0; k < 3; k++) b.pivot[k] = in.readFloat();
            for (int k = 0; k < 3; k++) b.rot[k] = in.readFloat();
            int vc = in.readInt(), ic = in.readInt();
            b.verts = new float[vc * 8];
            for (int k = 0; k < b.verts.length; k++) b.verts[k] = in.readFloat();
            b.indices = new int[ic];
            for (int k = 0; k < ic; k++) b.indices[k] = in.readInt();
        }
        int ac = in.readShort();
        for (int a = 0; a < ac; a++) {
            Anim an = new Anim();
            an.name = in.readUTF();
            an.length = in.readFloat();
            int sc = in.readShort();
            an.soundTime = new float[sc];
            an.soundName = new String[sc];
            for (int s = 0; s < sc; s++) { an.soundTime[s] = in.readFloat(); an.soundName[s] = in.readUTF(); }
            an.channels = new Channel[in.readShort()];
            for (int c = 0; c < an.channels.length; c++) {
                Channel ch = an.channels[c] = new Channel();
                ch.bone = in.readShort();
                ch.type = in.readUnsignedByte();
                int kc = in.readShort();
                ch.time = new float[kc]; ch.pre = new float[kc * 3]; ch.post = new float[kc * 3]; ch.smooth = new boolean[kc];
                for (int k = 0; k < kc; k++) {
                    ch.time[k] = in.readFloat();
                    for (int j = 0; j < 3; j++) ch.pre[k * 3 + j] = in.readFloat();
                    for (int j = 0; j < 3; j++) ch.post[k * 3 + j] = in.readFloat();
                    ch.smooth[k] = in.readUnsignedByte() == 1;
                }
            }
            d.anims.put(an.name, an);
        }
        return d;
    }
}
