#version 330 core
in vec2 vUV;
uniform sampler2D uScene;
uniform sampler2D uBloom;
uniform float uExposure;
uniform float uTime;
uniform int uUnderwater;
uniform float uBloomStrength;
uniform float uDamage;
out vec4 fragColor;

// ACES filmic approximation (Narkowicz)
vec3 aces(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

void main() {
    vec2 uv = vUV;
    if (uUnderwater == 1) uv += vec2(sin(uv.y * 30.0 + uTime * 2.0), cos(uv.x * 25.0 + uTime * 1.7)) * 0.0025;
    vec3 c = texture(uScene, uv).rgb * uExposure;
    c += texture(uBloom, uv).rgb * uBloomStrength;
    c = aces(c);
    // Mild saturation boost and vignette
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    c = mix(vec3(l), c, 1.02);
    vec2 v = vUV - 0.5;
    c *= 1.0 - dot(v, v) * 0.45;
    c = mix(c, vec3(0.6, 0.0, 0.0), uDamage * 0.35 * smoothstep(0.1, 0.7, length(v)));
    c = pow(max(c, 0.0), vec3(1.0 / 2.2));
    fragColor = vec4(c, 1.0);
}
