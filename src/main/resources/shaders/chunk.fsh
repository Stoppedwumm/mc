#version 330 core
in vec2 vUV;
in vec4 vColor;
in vec2 vLight;
in float vShade;
in float vDist;

uniform sampler2D uTex;
uniform float uDaylight;
uniform vec3 uSkyTint;
uniform vec3 uFogColor;
uniform float uFogStart;
uniform float uFogEnd;
uniform int uTranslucent;
uniform float uGamma;

out vec4 fragColor;

float brightness(float l) {
    float b = l / (4.0 - 3.0 * l);
    float g = 1.0 - pow(1.0 - b, 4.0);
    return mix(b, g, uGamma);
}

void main() {
    vec4 t = texture(uTex, vUV);
    vec3 col;
    float alpha;
    if (uTranslucent == 1) {
        col = t.rgb;
        alpha = t.a;
    } else {
        if (t.a < 0.3) discard;
        float tintAmount = vColor.a > 0.5 ? 1.0 : 1.0 - smoothstep(0.7, 0.95, t.a);
        col = t.rgb * mix(vec3(1.0), vColor.rgb, tintAmount);
        alpha = 1.0;
    }
    float sky = brightness(vLight.x * uDaylight);
    float blk = brightness(vLight.y);
    vec3 light = max(sky * uSkyTint, blk * vec3(1.0, 0.86, 0.66));
    light = max(light, vec3(0.03));
    col *= light * vShade;
    float f = clamp((vDist - uFogStart) / (uFogEnd - uFogStart), 0.0, 1.0);
    col = mix(col, uFogColor, f);
    fragColor = vec4(col, alpha);
}
