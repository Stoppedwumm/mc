package mc.client;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;

public final class Window {
    public final long handle;
    public int width, height;
    private boolean fullscreen;
    private int windowedX, windowedY, windowedW, windowedH;

    public Window(String title, int w, int h) {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Unable to initialise GLFW");
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        handle = glfwCreateWindow(w, h, title, 0, 0);
        if (handle == 0) throw new IllegalStateException("Failed to create window (OpenGL 3.3 required)");
        GLFWVidMode mode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (mode != null) glfwSetWindowPos(handle, (mode.width() - w) / 2, (mode.height() - h) / 2);
        glfwMakeContextCurrent(handle);
        GL.createCapabilities();
        glfwSwapInterval(1);
        glfwShowWindow(handle);
        updateSize();
    }

    public void updateSize() {
        try (MemoryStack s = MemoryStack.stackPush()) {
            IntBuffer w = s.mallocInt(1), h = s.mallocInt(1);
            glfwGetFramebufferSize(handle, w, h);
            width = Math.max(1, w.get(0));
            height = Math.max(1, h.get(0));
        }
    }

    public void setVsync(boolean on) { glfwSwapInterval(on ? 1 : 0); }

    public void toggleFullscreen() {
        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode mode = glfwGetVideoMode(monitor);
        if (mode == null) return;
        if (!fullscreen) {
            try (MemoryStack s = MemoryStack.stackPush()) {
                IntBuffer a = s.mallocInt(1), b = s.mallocInt(1);
                glfwGetWindowPos(handle, a, b);
                windowedX = a.get(0); windowedY = b.get(0);
                glfwGetWindowSize(handle, a, b);
                windowedW = a.get(0); windowedH = b.get(0);
            }
            glfwSetWindowMonitor(handle, monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate());
        } else {
            glfwSetWindowMonitor(handle, 0, windowedX, windowedY, windowedW, windowedH, GLFW_DONT_CARE);
        }
        fullscreen = !fullscreen;
        glfwSwapInterval(1);
    }

    public boolean shouldClose() { return glfwWindowShouldClose(handle); }

    public void swap() {
        glfwSwapBuffers(handle);
    }

    public void pollEvents() {
        glfwPollEvents();
    }

    public void destroy() {
        glfwDestroyWindow(handle);
        glfwTerminate();
    }
}
