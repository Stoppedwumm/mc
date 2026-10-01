#version 330 core
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aUV;
layout(location = 2) in vec4 aLight;

uniform mat4 uShadowMatrix;
uniform vec3 uOffset;
uniform vec3 uCamFrac;
uniform vec3 uCamInt;
uniform float uTime;
uniform float uRain;

out vec2 vUV;

void main() {
    vec3 local = aPos / 16.0;
    vec3 p = (local + uOffset) - uCamFrac;
    vec3 world = local + uOffset + uCamInt;
    int flags = int(aLight.w + 0.5);
    float windStrength = 1.0 + uRain * 1.5;
    if ((flags & 8) != 0) {
        float t = uTime * 1.7 + world.x * 0.6 + world.z * 0.45 + world.y * 0.3;
        p += vec3(sin(t), sin(t * 1.3 + 1.0) * 0.5, cos(t * 0.9)) * 0.035 * windStrength;
    } else if ((flags & 16) != 0) {
        float t = uTime * 2.2 + world.x * 0.9 + world.z * 0.7;
        p.xz += vec2(sin(t), cos(t * 1.2)) * 0.07 * windStrength;
    }
    vUV = aUV / 16384.0; // 1/16 texel units over the 64-tile-wide atlas (render.Atlas)
    gl_Position = uShadowMatrix * vec4(p, 1.0);
}
