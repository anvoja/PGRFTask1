#version 330

in vec2 inPosition;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main() {
    float x = inPosition.x;
    float y = inPosition.y;
    float z = sin(5.0 * x) * cos(5.0 * y) * 0.3;

    vec4 position = vec4(x, y, z, 1.0);

    gl_Position = projection * view * model * position;
}