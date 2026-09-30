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

out vec2 vUV;
out vec4 vColor;
out vec2 vLight;
out float vShade;
out float vDist;

void main() {
    vec3 p = (aPos / 16.0 + uOffset) - uCamFrac;
    gl_Position = uProjView * vec4(p, 1.0);
    vUV = aUV / 4096.0;
    vLight = aLight.xy / 240.0;
    vShade = aLight.z / 255.0;
    vColor = aColor;
    vDist = max(length(p.xz), abs(p.y));
}
