#version 330

in vec3 inPosition;
in vec2 inTexCoord;

out vec3 vertColor;

uniform mat4 mat;

void main() {
    gl_Position = mat * vec4(inPosition * 1.0, 1.0);
    vertColor = vec3(inTexCoord, 0.5);
}