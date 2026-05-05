#version 330

in vec3 inPosition;

uniform mat4 model;
uniform float time;
uniform mat4 projection;
uniform mat4 view;
uniform int renderMode;

out vec3 vFragPos;

void main() {
    vec3 pos;

    if (renderMode == 0) {
        float x = inPosition.x;
        float y = inPosition.y;
        float z = sin(5.0 * x + time) * cos(5.0 * y + time) * 0.3;
//        float z = 0;
        pos = vec3(x, y, z);
    } else {
        pos = inPosition;
    }

    vec4 worldPosition = model * vec4(pos, 1.0);
    vFragPos = worldPosition.xyz;
    gl_Position = projection * view * worldPosition;;
}
