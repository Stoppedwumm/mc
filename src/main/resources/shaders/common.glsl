// Shared lighting / atmosphere code. All colours are linear HDR radiance.
uniform vec3 uSunDir;
uniform float uTime;
uniform float uRain;

const float PI = 3.14159265;
const vec3 SKY_BASE = vec3(0.39, 0.57, 1.0);

vec3 toLinear(vec3 c) { return pow(c, vec3(2.2)); }

float zenithDensity(float y) { return 0.7 / pow(max(y + 0.08, 0.0035), 0.75); }
vec3 skyAbsorption(float d) { return exp2(-SKY_BASE * d) * 2.0; }

// Colour of direct sunlight after passing through the atmosphere (reddens near the horizon).
vec3 sunlightColor() {
    vec3 a = skyAbsorption(zenithDensity(max(uSunDir.y, 0.0) + 0.04));
    float up = smoothstep(-0.06, 0.08, uSunDir.y);
    return a * 1.35 * up * (1.0 - uRain * 0.85);
}

vec3 moonlightColor() {
    return vec3(0.045, 0.06, 0.1) * smoothstep(-0.06, 0.1, -uSunDir.y) * (1.0 - uRain * 0.7);
}

// Sky radiance: Rayleigh-like zenith/horizon gradient, Mie halo around the sun, sunset glow and night sky.
vec3 atmosphere(vec3 dir) {
    vec3 sun = uSunDir;
    float sy = sun.y;
    float y = max(dir.y, 0.0);
    float day = smoothstep(-0.22, 0.12, sy);
    float cosT = dot(dir, sun);

    vec3 zenith = vec3(0.075, 0.2, 0.62);
    vec3 horizon = vec3(0.5, 0.66, 0.9);
    vec3 col = mix(zenith, horizon, pow(1.0 - y, 4.0));

    // Twilight: warm band along the horizon, strongest towards the sun
    float twilight = 1.0 - smoothstep(0.02, 0.4, abs(sy + 0.03));
    float towardSun = pow(max(cosT, 0.0) * 0.5 + 0.5, 3.0);
    vec3 sunsetCol = mix(vec3(1.0, 0.35, 0.1), vec3(1.0, 0.62, 0.3), towardSun);
    col = mix(col, sunsetCol, twilight * pow(1.0 - y, 5.0) * (0.25 + 0.75 * towardSun));
    col = mix(col, col * vec3(0.55, 0.6, 0.85), twilight * 0.4 * (1.0 - towardSun));

    // Mie scattering halo
    vec3 sunCol = sunlightColor() + vec3(0.001);
    col += sunCol * (pow(max(cosT, 0.0), 10.0) * 0.18 + pow(max(cosT, 0.0), 120.0) * 0.8);
    col *= day * 1.7;

    // Moonlit night sky
    vec3 night = vec3(0.004, 0.007, 0.018) * (0.5 + 0.5 * y) + vec3(0.012, 0.016, 0.03) * pow(max(dot(dir, -sun), 0.0), 16.0);
    col += night * (1.0 - day);

    if (dir.y < 0.0) col *= mix(1.0, 0.5, smoothstep(0.0, -0.3, dir.y));

    // Overcast when raining
    float lum = dot(col, vec3(0.3, 0.59, 0.11));
    col = mix(col, vec3(lum) * vec3(0.85, 0.9, 0.95) * 0.6, uRain * 0.85);
    return col;
}

// Hemispherical ambient light from the sky, used for surfaces lit by skylight.
vec3 skyAmbient() {
    vec3 a = atmosphere(normalize(vec3(0.3, 1.0, 0.2))) * 0.5 + atmosphere(normalize(vec3(-0.6, 0.25, 0.1))) * 0.3;
    return a + vec3(0.01, 0.012, 0.018);
}
