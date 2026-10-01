#version 330 core
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec4 aColor;

uniform mat4 uProjView;
// Tile origin relative to the camera
uniform vec3 uOffset;

out vec3 vPos;
out vec3 vColor;
flat out int vFace;
flat out float vDepth;

void main() {
    vec3 p = aPos + uOffset;
    int extra = int(aColor.a + 0.5);
    vFace = extra & 7;
    vDepth = float(extra >> 3);
    vColor = aColor.rgb / 255.0;
    vPos = p;
    gl_Position = uProjView * vec4(p, 1.0);
}
