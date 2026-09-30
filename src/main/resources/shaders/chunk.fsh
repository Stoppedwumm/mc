#version 330 core
#include "common.glsl"

in vec2 vUV;
in vec4 vColor;
in vec2 vLight;
in float vAO;
in vec3 vPos;
in vec3 vWorld;
in vec3 vNormal;
in vec4 vShadowPos;
flat in int vFlags;

uniform sampler2D uTex;
uniform sampler2DShadow uShadowMap;
uniform sampler2D uDepthTex;
uniform int uShadows;
uniform vec3 uLightDir;
uniform int uTranslucent;
uniform float uFogEnd;
uniform int uUnderwater;
uniform vec2 uScreen;
uniform float uNear;
uniform float uFar;
uniform float uBrightness;

out vec4 fragColor;

const vec2 POISSON[8] = vec2[](vec2(-0.613, 0.617), vec2(0.170, -0.040), vec2(-0.299, -0.792), vec2(0.645, 0.493),
                               vec2(-0.651, -0.170), vec2(0.421, -0.573), vec2(-0.080, 0.180), vec2(0.853, -0.062));

float shadow() {
    if (uShadows == 0) return 1.0;
    vec3 s = vShadowPos.xyz / vShadowPos.w * 0.5 + 0.5;
    if (s.x <= 0.0 || s.x >= 1.0 || s.y <= 0.0 || s.y >= 1.0 || s.z >= 1.0) return 1.0;
    float sum = 0.0;
    float radius = 1.2 / 2048.0;
    for (int i = 0; i < 8; i++) sum += texture(uShadowMap, vec3(s.xy + POISSON[i] * radius, s.z - 0.0004));
    float edge = max(abs(s.x - 0.5), abs(s.y - 0.5));
    return mix(sum / 8.0, 1.0, smoothstep(0.42, 0.5, edge));
}

float linearDepth(float d) {
    float z = d * 2.0 - 1.0;
    return 2.0 * uNear * uFar / (uFar + uNear - z * (uFar - uNear));
}

vec3 fog(vec3 col, float dist, vec3 viewDir, out float amount) {
    if (uUnderwater == 1) {
        vec3 water = vec3(0.01, 0.05, 0.08) * (length(sunlightColor()) * 0.6 + 0.08);
        amount = 1.0 - exp(-dist * 0.08);
        return mix(col * exp(-vec3(0.4, 0.08, 0.05) * dist * 0.4), water, amount);
    }
    float density = 0.0025 + uRain * 0.012;
    float f = 1.0 - exp(-dist * density);
    // Hide the edge of the loaded world
    f = max(f, smoothstep(uFogEnd * 0.78, uFogEnd, dist));
    vec3 fogDir = normalize(vec3(viewDir.x, max(viewDir.y, 0.02), viewDir.z));
    vec3 fc = atmosphere(fogDir);
    amount = f;
    return mix(col, fc, f);
}

vec3 lighting(vec3 albedo, vec3 n, float sky, float blk, float ao, bool foliage) {
    float sh = shadow();
    float ndl = foliage ? 0.65 : max(dot(n, uLightDir), 0.0);
    vec3 lightCol = uSunDir.y > -0.05 ? sunlightColor() : moonlightColor();
    // Direct light only where the sky is visible (the shadow map doesn't cover everything)
    float access = smoothstep(0.55, 0.95, sky);
    vec3 direct = lightCol * ndl * sh * access * 1.6;
    float dirAmb = foliage ? 0.9 : (0.75 + 0.25 * n.y);
    vec3 ambient = skyAmbient() * (sky * sky) * dirAmb * ao;
    float b = blk * blk;
    vec3 torch = vec3(1.0, 0.62, 0.3) * (b * b * 2.2 + b * 0.35) * ao;
    vec3 minLight = vec3(0.012, 0.013, 0.016) * (0.5 + uBrightness);
    return albedo * (direct + ambient + torch + minLight * ao);
}

void main() {
    vec4 t = texture(uTex, vUV);
    vec3 viewDir = normalize(vPos);
    float dist = length(vPos);
    bool water = (vFlags & 32) != 0;
    float fogAmount;

    if (uTranslucent == 1 && water) {
        vec3 n = vNormal;
        if ((vFlags & 7) == 0) {
            // Procedural waves
            vec2 p = vWorld.xz;
            float tt = uTime;
            vec2 grad = vec2(0.0);
            grad += vec2(cos(p.x * 0.9 + tt * 1.3), 0.0) * 0.9 * 0.05;
            grad += vec2(0.0, cos(p.y * 1.1 + tt * 1.1)) * 1.1 * 0.05;
            grad += vec2(cos((p.x + p.y) * 2.3 + tt * 2.1)) * 2.3 * 0.012;
            grad += vec2(cos((p.x - p.y) * 3.1 - tt * 2.7), -cos((p.x - p.y) * 3.1 - tt * 2.7)) * 3.1 * 0.008;
            n = normalize(vec3(-grad.x, 1.0, -grad.y));
            if (uUnderwater == 1) n = -n;
        }
        vec3 v = -viewDir;
        float ndv = abs(dot(n, v));
        float fresnel = 0.02 + 0.98 * pow(1.0 - ndv, 5.0);
        vec3 r = reflect(viewDir, n);
        r.y = abs(r.y);
        float sky = vLight.x;
        vec3 refl = atmosphere(r) * (0.15 + 0.85 * sky * sky);
        float sh = shadow();
        vec3 lightCol = uSunDir.y > -0.05 ? sunlightColor() : moonlightColor();
        refl += lightCol * pow(max(dot(r, uLightDir), 0.0), 700.0) * 60.0 * sh * smoothstep(0.6, 1.0, sky);
        // Absorption through the water column using the opaque depth buffer
        float sceneDepth = linearDepth(texture(uDepthTex, gl_FragCoord.xy / uScreen).r);
        float depth = max(sceneDepth - linearDepth(gl_FragCoord.z), 0.0);
        if (uUnderwater == 1) depth = 0.5;
        vec3 trans = exp(-vec3(0.46, 0.1, 0.07) * depth);
        vec3 scatterCol = vec3(0.012, 0.05, 0.06) * (skyAmbient() * sky * sky + lightCol * 0.4 * sky + vec3(0.02));
        vec3 src = refl * fresnel + (1.0 - fresnel) * scatterCol * (1.0 - trans);
        float a = (1.0 - fresnel) * dot(trans, vec3(0.333));
        vec3 fogged = fog(src, dist, viewDir, fogAmount);
        a *= 1.0 - fogAmount;
        fragColor = vec4(fogged, a);
        return;
    }

    vec3 albedo;
    float alpha = 1.0;
    if (uTranslucent == 1) {
        albedo = toLinear(t.rgb);
        alpha = t.a;
    } else {
        if (t.a < 0.3) discard;
        float tintAmount = vColor.a > 0.5 ? 1.0 : 1.0 - smoothstep(0.7, 0.95, t.a);
        albedo = toLinear(t.rgb * mix(vec3(1.0), vColor.rgb, tintAmount));
    }
    bool foliage = (vFlags & 24) != 0 || (vFlags & 7) == 6;
    vec3 col;
    if ((vFlags & 64) != 0) col = albedo * 3.0; // emissive (lava, torch flame)
    else col = lighting(albedo, vNormal, vLight.x, vLight.y, vAO, foliage);
    col = fog(col, dist, viewDir, fogAmount);
    if (uTranslucent == 1) {
        // Premultiplied output for the (ONE, SRC_ALPHA) blend used by the translucent pass
        fragColor = vec4(col * alpha, 1.0 - alpha);
    } else {
        fragColor = vec4(col, 1.0);
    }
}
