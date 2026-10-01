package com.mojang.blaze3d.vertex;

import java.util.ArrayDeque;

/** GUI transform stack (reamc-compat: translation and scale only). */
public class PoseStack {
    private final ArrayDeque<float[]> stack = new ArrayDeque<>();
    private float[] top = {0, 0, 0, 1};

    public void pushPose() { stack.push(top.clone()); }
    public void popPose() { top = stack.isEmpty() ? new float[]{0, 0, 0, 1} : stack.pop(); }
    public void translate(float x, float y, float z) { top[0] += x * top[3]; top[1] += y * top[3]; top[2] += z; }
    public void translate(double x, double y, double z) { translate((float) x, (float) y, (float) z); }
    public void scale(float x, float y, float z) { top[3] *= x; }
    public float reamc$x() { return top[0]; }
    public float reamc$y() { return top[1]; }
    public float reamc$scale() { return top[3]; }
}
