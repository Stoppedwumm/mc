#version 330 core
#include "common.glsl"

in vec3 vPos;
in vec3 vColor;
flat in int vFace;
flat in float vDepth;

uniform vec3 uLightDir;
uniform vec3 uCamPos;
uniform float uFogEnd;
uniform float uFogDensity;
uniform float uBrightness;
uniform float uNightVision;
uniform int uWater;
// One texel per chunk around the camera: where real chunks are drawn, the LOD is hidden
uniform sampler2D uMask;
uniform vec2 uMaskOrigin;
uniform float uMaskSize;

out vec4 fragColor;

const vec3 NORMALS[7] = vec3[](vec3(0, 1, 0), vec3(0, -1, 0), vec3(0, 0, -1), vec3(0, 0, 1), vec3(-1, 0, 0), vec3(1, 0, 0), vec3(0, 1, 0));

vec3 fog(vec3 col, float dist, vec3 viewDir, out float amount) {
    float f = 1.0 - exp(-dist * (uFogDensity + uRain * 0.006));
    f = max(f, smoothstep(uFogEnd * 0.8, uFogEnd, dist));
    vec3 fogDir = normalize(vec3(viewDir.x, max(viewDir.y, 0.02), viewDir.z));
    amount = f;
    return mix(col, atmosphere(fogDir), f);
}

void main() {
    // Skip pixels inside chunks that are rendered at full detail (with a hair of overlap at their edge)
    vec2 world = uCamPos.xz + vPos.xz - NORMALS[vFace].xz * 0.01;
    vec2 cell = floor(world / 16.0) - uMaskOrigin;
    if (cell.x >= 0.0 && cell.y >= 0.0 && cell.x < uMaskSize && cell.y < uMaskSize) {
        if (texelFetch(uMask, ivec2(cell), 0).r > 0.5) discard;
    }
    vec3 viewDir = normalize(vPos);
    float dist = length(vPos);
    vec3 n = NORMALS[vFace];
    vec3 lightCol = uSunDir.y > -0.05 ? sunlightColor() : moonlightColor();
    float fogAmount;

    if (uWater == 1) {
        // Sky reflection with Fresnel over a body colour that darkens with depth
        float ndv = abs(dot(vec3(0.0, 1.0, 0.0), -viewDir));
        float fresnel = 0.02 + 0.98 * pow(1.0 - ndv, 5.0);
        vec3 r = reflect(viewDir, vec3(0.0, 1.0, 0.0));
        vec3 refl = atmosphere(r);
        refl += lightCol * pow(max(dot(r, uLightDir), 0.0), 400.0) * 30.0;
        vec3 body = toLinear(vColor) * (skyAmbient() * 0.9 + lightCol * 0.35 * max(uLightDir.y, 0.0));
        vec3 col = mix(body, refl, fresnel);
        float a = mix(0.62, 0.97, clamp(vDepth / 10.0, 0.0, 1.0));
        a = max(a, fresnel);
        col = fog(col, dist, viewDir, fogAmount);
        fragColor = vec4(col, a);
        return;
    }

    vec3 albedo = toLinear(vColor);
    float ndl = max(dot(n, uLightDir), 0.0);
    vec3 direct = lightCol * ndl * 1.6;
    vec3 ambient = skyAmbient() * (0.75 + 0.25 * n.y);
    vec3 minLight = vec3(0.012, 0.013, 0.016) * (0.5 + uBrightness);
    vec3 col = albedo * (direct + ambient + minLight + vec3(0.55, 0.57, 0.6) * uNightVision);
    col = fog(col, dist, viewDir, fogAmount);
    fragColor = vec4(col, 1.0);
}
