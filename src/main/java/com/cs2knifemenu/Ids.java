package com.cs2knifemenu;

import net.minecraft.resources.Identifier;

/**
 * Single place that knows the name of Minecraft's identifier class.
 * If your mappings call it `Identifier` instead of `Identifier`, change it here and in the
 * 3 files that import it (see README "Version notes").
 */
public final class Ids {
    public static final String NS = "cs2knifemenu";
    private Ids() {}
    public static Identifier of(String path) { return Identifier.fromNamespaceAndPath(NS, path); }
}
