package mc.render;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33C.*;

public final class ShaderProgram {
    private final int id;
    private final Map<String, Integer> uniforms = new HashMap<>();

    public ShaderProgram(String name) {
        this(name, name);
    }

    public ShaderProgram(String vertex, String fragment) {
        String name = fragment;
        int vs = compile(GL_VERTEX_SHADER, load("/shaders/" + vertex + ".vsh"), vertex + ".vsh");
        int fs = compile(GL_FRAGMENT_SHADER, load("/shaders/" + fragment + ".fsh"), fragment + ".fsh");
        id = glCreateProgram();
        glAttachShader(id, vs);
        glAttachShader(id, fs);
        glLinkProgram(id);
        if (glGetProgrami(id, GL_LINK_STATUS) == GL_FALSE)
            throw new IllegalStateException("Failed to link " + name + ": " + glGetProgramInfoLog(id));
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private static String load(String path) {
        try (InputStream in = ShaderProgram.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Missing shader " + path);
            String src = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            // Minimal #include "file" support for shared GLSL code
            StringBuilder out = new StringBuilder();
            for (String line : src.split("\n", -1)) {
                String t = line.trim();
                if (t.startsWith("#include")) out.append(load("/shaders/" + t.substring(t.indexOf('"') + 1, t.lastIndexOf('"')))).append('\n');
                else out.append(line).append('\n');
            }
            return out.toString();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static int compile(int type, String src, String name) {
        int s = glCreateShader(type);
        glShaderSource(s, src);
        glCompileShader(s);
        if (glGetShaderi(s, GL_COMPILE_STATUS) == GL_FALSE)
            throw new IllegalStateException("Failed to compile " + name + ": " + glGetShaderInfoLog(s));
        return s;
    }

    public void bind() { glUseProgram(id); }

    private int loc(String name) {
        return uniforms.computeIfAbsent(name, n -> glGetUniformLocation(id, n));
    }

    public void set(String name, float v) { glUniform1f(loc(name), v); }
    public void set(String name, int v) { glUniform1i(loc(name), v); }
    public void set(String name, float x, float y) { glUniform2f(loc(name), x, y); }
    public void set(String name, float x, float y, float z) { glUniform3f(loc(name), x, y, z); }
    public void set(String name, float x, float y, float z, float w) { glUniform4f(loc(name), x, y, z, w); }

    public void set(String name, Matrix4f m) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(loc(name), false, m.get(stack.mallocFloat(16)));
        }
    }
}
