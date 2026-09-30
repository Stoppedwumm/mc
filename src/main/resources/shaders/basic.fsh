#version 330 core
in vec2 vUV;
in vec4 vColor;
in float vDist;

uniform sampler2D uTex;
uniform int uUseTex;
uniform float uAlphaCut;
uniform int uFog;
uniform vec3 uFogColor;
uniform float uFogStart;
uniform float uFogEnd;

out vec4 fragColor;

void main() {
    vec4 c = vColor;
    if (uUseTex == 1) c *= texture(uTex, vUV);
    if (c.a < uAlphaCut) discard;
    if (uFog != 0) {
        float f = clamp((vDist - uFogStart) / (uFogEnd - uFogStart), 0.0, 1.0);
        if (uFog == 1) c.rgb = mix(c.rgb, uFogColor, f);
        else c.a *= 1.0 - f;
    }
    fragColor = c;
}
