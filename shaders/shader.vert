#version 330

in vec2 inPosition;

void main() {
    float x = inPosition.x;
    float y = inPosition.y;

    // SAFE wave (small amplitude!)
    float z = sin(5.0 * x) * cos(5.0 * y) * 0.2;

    gl_Position = vec4(x, y, z, 1.0);
}