#version 330

in float height;

out vec4 outColor;

void main() {
    float h = height;

    // normalize height roughly from <-0.5, 0.5> to <0, 1>
    float t = h + 0.5;
    t = clamp(t, 0.0, 1.0);

    // low = green-ish, high = red-ish
    vec3 lowColor = vec3(0.0, 1.0, 0.2);
    vec3 highColor = vec3(1.0, 0.0, 0.2);

    vec3 finalColor = mix(highColor, lowColor, t);

    outColor = vec4(finalColor, 1.0);
}