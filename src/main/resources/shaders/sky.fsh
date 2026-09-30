#version 330 core
#include "common.glsl"
in vec2 vNdc;

uniform mat4 uInvProjView;
uniform float uStarAngle;
uniform vec3 uCamPos;
uniform int uClouds;

out vec4 fragColor;

float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float hash3(vec3 p) { return fract(sin(dot(p, vec3(12.9898, 78.233, 37.719))) * 43758.5453); }

float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), f.x), mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 5; i++) { v += noise(p) * a; p = p * 2.03 + vec2(1.7, 9.2); a *= 0.5; }
    return v;
}

float cloudDensity(vec2 p) {
    float coverage = 0.52 - uRain * 0.3;
    float n = fbm(p * 0.0035 + vec2(uTime * 0.004, uTime * 0.0015));
    return smoothstep(coverage, coverage + 0.28, n);
}

void main() {
    vec4 p = uInvProjView * vec4(vNdc, 1.0, 1.0);
    vec3 d = normalize(p.xyz / p.w);
    vec3 col = atmosphere(d);
    float night = smoothstep(0.05, -0.2, uSunDir.y);

    // Stars rotate with the sky
    if (night > 0.0 && d.y > -0.05) {
        float c = cos(uStarAngle), s = sin(uStarAngle);
        vec3 rd = vec3(c * d.x - s * d.y, s * d.x + c * d.y, d.z);
        vec3 q = rd * 220.0;
        vec3 cell = floor(q);
        float hs = hash3(cell);
        if (hs > 0.9965) {
            float star = 1.0 - smoothstep(0.05, 0.3, length(fract(q) - 0.5));
            float twinkle = 0.7 + 0.3 * sin(uTime * 3.0 + hs * 100.0);
            col += vec3(0.9, 0.95, 1.0) * star * twinkle * night * (hs - 0.9965) * 900.0 * 0.02 * (1.0 - uRain);
        }
    }

    // Sun disc with limb darkening
    float cosS = dot(d, uSunDir);
    float sunR = 0.99988;
    if (cosS > sunR) {
        float x = (cosS - sunR) / (1.0 - sunR);
        col += sunlightColor() * 60.0 * (0.4 + 0.6 * sqrt(x)) * (1.0 - uRain);
    }
    // Moon disc with craters
    float cosM = dot(d, -uSunDir);
    if (cosM > 0.99982) {
        vec3 t1 = normalize(cross(-uSunDir, vec3(0, 0, 1)));
        vec3 t2 = cross(t1, -uSunDir);
        vec2 mp = vec2(dot(d, t1), dot(d, t2)) * 400.0;
        float crater = 0.75 + 0.25 * fbm(mp * 1.5 + 3.0);
        col += vec3(0.8, 0.82, 0.86) * crater * 0.6 * (0.3 + 0.7 * night) * (1.0 - uRain);
    }

    // Soft procedural cloud layer
    if (uClouds == 1) {
        float h = 230.0;
        float t = (h - uCamPos.y) / d.y;
        if (t > 0.0 && t < 12000.0) {
            vec2 wp = uCamPos.xz + d.xz * t;
            float dens = cloudDensity(wp);
            if (dens > 0.001) {
                // Self shadowing: sample towards the sun
                float toward = cloudDensity(wp + uSunDir.xz * 60.0);
                float lit = exp(-toward * 1.8);
                vec3 sunC = sunlightColor();
                float silver = pow(max(cosS, 0.0), 6.0) * 1.5;
                vec3 cloudCol = sunC * (lit * 0.9 + silver * (1.0 - dens)) + skyAmbient() * 0.9 + moonlightColor() * 0.8;
                cloudCol = mix(cloudCol, vec3(dot(cloudCol, vec3(0.33))) * 0.6, uRain * 0.7);
                float fade = exp(-t * 0.00025) * smoothstep(0.0, 0.08, abs(d.y));
                col = mix(col, cloudCol, clamp(dens * fade * (0.85 + uRain * 0.15), 0.0, 1.0));
            }
        }
    }
    fragColor = vec4(col, 1.0);
}
