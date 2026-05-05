#version 330

in vec3 vFragPos;

out vec4 outColor;

void main() {
    vec3 normal = normalize(cross(dFdx(vFragPos), dFdy(vFragPos)));

    normal = normal * 0.5 + 0.5;

    outColor = vec4(normal, 1.0);
}