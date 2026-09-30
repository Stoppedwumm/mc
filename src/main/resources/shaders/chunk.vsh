#version 330 core
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aUV;
layout(location = 2) in vec4 aLight;
layout(location = 3) in vec4 aColor;

uniform mat4 uProjView;
// Chunk origin minus the integer camera position (exact integers), then the fractional camera part.
// Splitting them keeps vertices on shared chunk edges bit-identical, which avoids pixel cracks.
uniform vec3 uOffset;
uniform vec3 uCamFrac;
uniform vec3 uCamInt;
uniform mat4 uShadowMatrix;
uniform float uTime;
uniform float uRain;

out vec2 vUV;
out vec4 vColor;
out vec2 vLight;
out float vAO;
out vec3 vPos;
out vec3 vWorld;
out vec3 vNormal;
out vec4 vShadowPos;
flat out int vFlags;

const vec3 NORMALS[7] = vec3[](vec3(0, 1, 0), vec3(0, -1, 0), vec3(0, 0, -1), vec3(0, 0, 1), vec3(-1, 0, 0), vec3(1, 0, 0), vec3(0, 1, 0));

void main() {
    vec3 local = aPos / 16.0;
    vec3 p = (local + uOffset) - uCamFrac;
    vec3 world = local + uOffset + uCamInt;
    int flags = int(aLight.w + 0.5);
    float windStrength = 1.0 + uRain * 1.5;
    if ((flags & 8) != 0) {
        // Leaves sway gently
        float t = uTime * 1.7 + world.x * 0.6 + world.z * 0.45 + world.y * 0.3;
        p += vec3(sin(t), sin(t * 1.3 + 1.0) * 0.5, cos(t * 0.9)) * 0.035 * windStrength;
    } else if ((flags & 16) != 0) {
        // Top vertices of plants bend in the wind
        float t = uTime * 2.2 + world.x * 0.9 + world.z * 0.7;
        p.xz += vec2(sin(t), cos(t * 1.2)) * 0.07 * windStrength;
    } else if ((flags & 32) != 0 && (flags & 7) == 0) {
        // Water surface bobs slightly
        p.y += (sin(uTime * 1.5 + world.x * 0.8) + cos(uTime * 1.2 + world.z * 0.9)) * 0.012 - 0.02;
    }
    int face = flags & 7;
    vNormal = NORMALS[min(face, 6)];
    vFlags = flags;
    gl_Position = uProjView * vec4(p, 1.0);
    vUV = aUV / 4096.0;
    vLight = aLight.xy / 240.0;
    vAO = aLight.z / 255.0;
    vColor = aColor;
    vPos = p;
    vWorld = world;
    // Normal offset reduces shadow acne
    vShadowPos = uShadowMatrix * vec4(p + vNormal * 0.06, 1.0);
}
