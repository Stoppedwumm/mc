package mc.client;

import static org.lwjgl.glfw.GLFW.*;

/** Collects GLFW events into per-frame state. */
public final class Input {
    private final boolean[] keys = new boolean[GLFW_KEY_LAST + 1];
    private final boolean[] pressed = new boolean[GLFW_KEY_LAST + 1];
    private final boolean[] buttons = new boolean[8];
    private final boolean[] clicked = new boolean[8];
    public double mouseX, mouseY, dx, dy, scroll;
    private double lastX, lastY;
    private boolean first = true;
    private final int[] winW = new int[1], winH = new int[1], fbW = new int[1], fbH = new int[1];
    public boolean resized, focusLost, focusGained;
    public final StringBuilder typed = new StringBuilder();

    public Input(Window w) {
        glfwSetKeyCallback(w.handle, (win, key, sc, action, mods) -> {
            if (key < 0 || key > GLFW_KEY_LAST) return;
            if (action == GLFW_PRESS) { keys[key] = true; pressed[key] = true; }
            else if (action == GLFW_RELEASE) keys[key] = false;
        });
        glfwSetMouseButtonCallback(w.handle, (win, button, action, mods) -> {
            if (button < 0 || button >= buttons.length) return;
            if (action == GLFW_PRESS) { buttons[button] = true; clicked[button] = true; }
            else if (action == GLFW_RELEASE) buttons[button] = false;
        });
        glfwSetCursorPosCallback(w.handle, (win, x, y) -> {
            if (first) { lastX = x; lastY = y; first = false; }
            dx += x - lastX;
            dy += y - lastY;
            lastX = x; lastY = y;
            // The cursor is reported in window points, but the GUI is laid out in framebuffer pixels; on HiDPI
            // screens (Retina) these differ, so convert to pixels
            glfwGetWindowSize(win, winW, winH);
            glfwGetFramebufferSize(win, fbW, fbH);
            mouseX = winW[0] > 0 ? x * fbW[0] / winW[0] : x;
            mouseY = winH[0] > 0 ? y * fbH[0] / winH[0] : y;
        });
        glfwSetScrollCallback(w.handle, (win, x, y) -> scroll += y);
        glfwSetFramebufferSizeCallback(w.handle, (win, x, y) -> resized = true);
        glfwSetCharCallback(w.handle, (win, cp) -> typed.appendCodePoint(cp));
        glfwSetWindowFocusCallback(w.handle, (win, focused) -> {
            if (focused) focusGained = true; else { focusLost = true; releaseAll(); }
        });
        if (glfwRawMouseMotionSupported()) glfwSetInputMode(w.handle, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
    }

    public boolean down(int key) { return keys[key]; }

    public boolean pressed(int key) { return pressed[key]; }

    public boolean button(int b) { return buttons[b]; }

    public boolean clicked(int b) { return clicked[b]; }

    public void consumeClick(int b) { clicked[b] = false; }

    public void releaseAll() {
        java.util.Arrays.fill(keys, false);
        java.util.Arrays.fill(buttons, false);
    }

    /** Call at the end of every frame. */
    public void endFrame() {
        java.util.Arrays.fill(pressed, false);
        java.util.Arrays.fill(clicked, false);
        dx = dy = 0;
        scroll = 0;
        focusLost = focusGained = false;
        typed.setLength(0);
    }
}
