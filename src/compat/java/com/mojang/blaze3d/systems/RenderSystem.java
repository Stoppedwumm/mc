package com.mojang.blaze3d.systems;

/** Render state calls from mod screens (reamc-compat: reamc's GUI already blends, so these only track the colour). */
public final class RenderSystem {
    private RenderSystem() { }

    private static final float[] COLOR = {1, 1, 1, 1};

    public static void setShaderColor(float r, float g, float b, float a) { COLOR[0] = r; COLOR[1] = g; COLOR[2] = b; COLOR[3] = a; }
    public static float[] getShaderColor() { return COLOR; }
    public static void enableBlend() { }
    public static void disableBlend() { }
    public static void defaultBlendFunc() { }
    public static void enableDepthTest() { }
    public static void disableDepthTest() { }
    public static void setShaderTexture(int unit, Object texture) { }
}
