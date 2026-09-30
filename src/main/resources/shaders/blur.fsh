#version 330 core
in vec2 vUV;
uniform sampler2D uTex;
uniform vec2 uDir;
out vec4 fragColor;
void main() {
    vec2 px = uDir / vec2(textureSize(uTex, 0));
    float w[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);
    vec3 c = texture(uTex, vUV).rgb * w[0];
    for (int i = 1; i < 5; i++) {
        c += texture(uTex, vUV + px * float(i) * 1.5).rgb * w[i];
        c += texture(uTex, vUV - px * float(i) * 1.5).rgb * w[i];
    }
    fragColor = vec4(c, 1.0);
}
