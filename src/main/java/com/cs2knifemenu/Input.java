package com.cs2knifemenu;

import org.lwjgl.glfw.GLFW;

/** Raw GLFW polling so we don't depend on version-specific key / mouse event classes. */
public final class Input {
    private Input() {}
    private static long win() { return GLFW.glfwGetCurrentContext(); }
    public static boolean key(int k) { long w = win(); return w != 0 && k > 0 && GLFW.glfwGetKey(w, k) == GLFW.GLFW_PRESS; }
    public static boolean mouse(int b) { long w = win(); return w != 0 && GLFW.glfwGetMouseButton(w, b) == GLFW.GLFW_PRESS; }

    /** First key currently held, or -1. */
    public static int anyKey() {
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) if (key(k)) return k;
        return -1;
    }

    public static String keyName(int k) {
        if (k <= 0) return "None";
        String n = GLFW.glfwGetKeyName(k, GLFW.glfwGetKeyScancode(k));
        if (n != null) return n.toUpperCase();
        if (k >= GLFW.GLFW_KEY_F1 && k <= GLFW.GLFW_KEY_F25) return "F" + (k - GLFW.GLFW_KEY_F1 + 1);
        return switch (k) {
            case GLFW.GLFW_KEY_SPACE -> "Space"; case GLFW.GLFW_KEY_ESCAPE -> "Esc"; case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "L-Shift"; case GLFW.GLFW_KEY_RIGHT_SHIFT -> "R-Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "L-Ctrl"; case GLFW.GLFW_KEY_RIGHT_CONTROL -> "R-Ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT -> "L-Alt"; case GLFW.GLFW_KEY_RIGHT_ALT -> "R-Alt";
            case GLFW.GLFW_KEY_UP -> "Up"; case GLFW.GLFW_KEY_DOWN -> "Down";
            case GLFW.GLFW_KEY_LEFT -> "Left"; case GLFW.GLFW_KEY_RIGHT -> "Right";
            case GLFW.GLFW_KEY_BACKSPACE -> "Backspace"; case GLFW.GLFW_KEY_ENTER -> "Enter";
            default -> "Key " + k;
        };
    }
}
