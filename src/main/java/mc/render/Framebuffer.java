package mc.render;

import static org.lwjgl.opengl.GL33C.*;

/** A framebuffer with an optional colour texture and optional depth texture. */
public final class Framebuffer {
    public final int fbo;
    public int color, depth;
    public int width, height;
    private final int colorFormat;
    private final boolean hasDepth, depthCompare;

    public Framebuffer(int width, int height, int colorFormat, boolean hasDepth, boolean depthCompare) {
        this.colorFormat = colorFormat;
        this.hasDepth = hasDepth;
        this.depthCompare = depthCompare;
        fbo = glGenFramebuffers();
        resize(width, height);
    }

    public void resize(int w, int h) {
        width = Math.max(1, w);
        height = Math.max(1, h);
        if (color != 0) glDeleteTextures(color);
        if (depth != 0) glDeleteTextures(depth);
        color = depth = 0;
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        if (colorFormat != 0) {
            color = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, color);
            glTexImage2D(GL_TEXTURE_2D, 0, colorFormat, width, height, 0, GL_RGBA, GL_FLOAT, (java.nio.FloatBuffer) null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, color, 0);
        } else {
            glDrawBuffer(GL_NONE);
            glReadBuffer(GL_NONE);
        }
        if (hasDepth) {
            depth = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, depth);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, width, height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (java.nio.FloatBuffer) null);
            int filter = depthCompare ? GL_LINEAR : GL_NEAREST;
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            if (depthCompare) {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
            }
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depth, 0);
        }
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) throw new IllegalStateException("Framebuffer incomplete: 0x" + Integer.toHexString(status));
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, width, height);
    }
}
