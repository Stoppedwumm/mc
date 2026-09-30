#version 330 core
in vec2 vNdc;

uniform mat4 uInvProjView;
uniform vec3 uSunDir;
uniform vec3 uZenith;
uniform vec3 uHorizon;
uniform vec3 uSunsetColor;
uniform float uSunset;
uniform float uNight;
uniform float uStarAngle;
uniform float uMoonPhase;

out vec4 fragColor;

float hash(vec3 p) {
    return fract(sin(dot(p, vec3(12.9898, 78.233, 37.719))) * 43758.5453);
}

void main() {
    vec4 p = uInvProjView * vec4(vNdc, 1.0, 1.0);
    vec3 d = normalize(p.xyz / p.w);
    float h = d.y;

    vec3 col = mix(uHorizon, uZenith, smoothstep(-0.02, 0.5, h));
    col = mix(col, uHorizon * 0.75, smoothstep(0.0, -0.4, h));

    // Sunrise / sunset glow along the horizon towards the sun
    float side = uSunDir.x >= 0.0 ? 1.0 : -1.0;
    float toward = pow(max(0.0, d.x * side * 0.5 + 0.5), 3.0);
    float band = exp(-abs(h - 0.02) * 7.0);
    col = mix(col, uSunsetColor, clamp(uSunset * band * toward * 1.3, 0.0, 1.0));

    // Stars rotate with the sky
    if (uNight > 0.01 && h > -0.1) {
        float c = cos(uStarAngle), s = sin(uStarAngle);
        vec3 rd = vec3(c * d.x - s * d.y, s * d.x + c * d.y, d.z);
        vec3 q = rd * 150.0;
        vec3 cell = floor(q);
        float hs = hash(cell);
        if (hs > 0.996) {
            vec3 f = fract(q) - 0.5;
            float star = 1.0 - smoothstep(0.08, 0.22, length(f));
            col += vec3(star * uNight * (0.4 + (hs - 0.996) * 150.0));
        }
    }

    // Square sun and moon like Minecraft
    vec3 t1 = normalize(cross(uSunDir, vec3(0.0, 0.0, 1.0)));
    vec3 t2 = vec3(0.0, 0.0, 1.0);
    float sd = dot(d, uSunDir);
    if (sd > 0.0) {
        vec2 q = vec2(dot(d, t1), dot(d, t2)) / sd;
        float m = max(abs(q.x), abs(q.y));
        col += vec3(1.0, 0.85, 0.55) * 0.35 * exp(-m * 9.0) * (1.0 - uNight * 0.8);
        if (m < 0.085) col = mix(vec3(1.0, 0.98, 0.8), vec3(1.0, 1.0, 0.95), step(m, 0.06)) * 1.1;
    } else {
        vec2 q = vec2(dot(d, -t1), dot(d, t2)) / -sd;
        float m = max(abs(q.x), abs(q.y));
        col += vec3(0.6, 0.7, 0.9) * 0.1 * exp(-m * 12.0) * uNight;
        if (m < 0.06) {
            vec2 pix = floor((q + 0.06) / 0.12 * 8.0);
            float crater = hash(vec3(pix, 3.0)) > 0.75 ? 0.8 : 1.0;
            // Phase: shade part of the disc
            float lit = step(uMoonPhase * 2.0 - 1.0, (q.x / 0.06));
            col = vec3(0.86, 0.88, 0.92) * crater * mix(0.25, 1.0, lit);
        }
    }
    fragColor = vec4(col, 1.0);
}
