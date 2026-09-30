package mc.render;

import static org.lwjgl.opengl.GL33C.*;

/** HDR scene target, depth copy for water, bloom chain and the final tonemapping composite. */
public final class PostProcess {
    public final Framebuffer scene, depthCopy, bloomA, bloomB;
    private final ShaderProgram bright, blur, composite;
    private final int vao;
    public float exposure = 0.3f;

    public PostProcess(int w, int h) {
        scene = new Framebuffer(w, h, GL_RGBA16F, true, false);
        depthCopy = new Framebuffer(w, h, 0, true, false);
        bloomA = new Framebuffer(w / 4, h / 4, GL_RGBA16F, false, false);
        bloomB = new Framebuffer(w / 4, h / 4, GL_RGBA16F, false, false);
        bright = new ShaderProgram("post", "bright");
        blur = new ShaderProgram("post", "blur");
        composite = new ShaderProgram("post", "composite");
        vao = glGenVertexArrays();
    }

    public void resize(int w, int h) {
        if (w == scene.width && h == scene.height) return;
        scene.resize(w, h);
        depthCopy.resize(w, h);
        bloomA.resize(w / 4, h / 4);
        bloomB.resize(w / 4, h / 4);
    }

    public void beginScene() {
        scene.bind();
    }

    /** Copies the scene depth so translucent shaders can read the depth of what lies behind them. */
    public void copyDepth() {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, scene.fbo);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, depthCopy.fbo);
        glBlitFramebuffer(0, 0, scene.width, scene.height, 0, 0, scene.width, scene.height, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        scene.bind();
    }

    private void fullscreen() {
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
    }

    public void finish(int screenW, int screenH, float time, boolean underwater, float damage) {
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
        glDisable(GL_CULL_FACE);
        glActiveTexture(GL_TEXTURE0);

        bloomA.bind();
        bright.bind();
        bright.set("uTex", 0);
        bright.set("uExposure", exposure);
        glBindTexture(GL_TEXTURE_2D, scene.color);
        fullscreen();
        for (int i = 0; i < 2; i++) {
            bloomB.bind();
            blur.bind();
            blur.set("uTex", 0);
            blur.set("uDir", 1f, 0f);
            glBindTexture(GL_TEXTURE_2D, bloomA.color);
            fullscreen();
            bloomA.bind();
            blur.set("uDir", 0f, 1f);
            glBindTexture(GL_TEXTURE_2D, bloomB.color);
            fullscreen();
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, screenW, screenH);
        composite.bind();
        composite.set("uScene", 0);
        composite.set("uBloom", 1);
        composite.set("uExposure", exposure);
        composite.set("uTime", time);
        composite.set("uUnderwater", underwater ? 1 : 0);
        composite.set("uBloomStrength", 0.12f);
        composite.set("uDamage", damage);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, bloomA.color);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, scene.color);
        fullscreen();
        glBindVertexArray(0);
    }
}
