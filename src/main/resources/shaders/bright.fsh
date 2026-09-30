#version 330 core
in vec2 vUV;
uniform sampler2D uTex;
uniform float uExposure;
out vec4 fragColor;
void main() {
    vec2 px = 1.0 / vec2(textureSize(uTex, 0));
    vec3 c = texture(uTex, vUV + px * vec2(-0.5, -0.5)).rgb + texture(uTex, vUV + px * vec2(0.5, -0.5)).rgb
           + texture(uTex, vUV + px * vec2(-0.5, 0.5)).rgb + texture(uTex, vUV + px * vec2(0.5, 0.5)).rgb;
    c *= 0.25 * uExposure;
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    fragColor = vec4(c * smoothstep(0.9, 2.5, l), 1.0);
}
