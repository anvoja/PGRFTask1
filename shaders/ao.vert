#version 330

in vec3 inPosition;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform float time;

out vec3 vFragPos;

void main() {
    float x = inPosition.x;
    float y = inPosition.y;

    float z = sin(5.0 * x + time) * cos(5.0 * y + time) * 0.3;

    vec3 pos = vec3(x, y, z);

    vec4 worldPosition = model * vec4(pos, 1.0);

    vFragPos = worldPosition.xyz;

    gl_Position = projection * view * worldPosition;
}