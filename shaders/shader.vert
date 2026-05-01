#version 330

#define PI 3.1415926538

in vec2 inPosition;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform float time;
uniform int surfaceMode;

out float height;
out vec3 vNormal;
out vec3 vFragPos;

vec3 plane(vec2 p) {
    return vec3(p.x, p.y, 0.0);
}

vec3 wave(vec2 p) {
    float x = p.x;
    float y = p.y;
    float z = sin(5.0 * x + time) * cos(5.0 * y + time) * 0.3;
    return vec3(x, y, z);
}

vec3 hill(vec2 p) {
    float x = p.x;
    float y = p.y;
    float z = 0.5 * exp(-(x * x + y * y) * 3.0);
    return vec3(x, y, z);
}

vec3 saddle(vec2 p) {
    float x = p.x;
    float y = p.y;
    float z = 0.4 * (x * x - y * y);
    return vec3(x, y, z);
}

vec3 getPosition(vec2 p) {
    switch(surfaceMode) {
        case 0: return plane(p);
        case 1: return wave(p);
        case 2: return hill(p);
        case 3: return saddle(p);
    }

    return plane(p);
}

vec3 getNormal(vec2 p) {
    float d = 0.001;

    vec3 p1 = getPosition(p + vec2(d, 0.0));
    vec3 p2 = getPosition(p - vec2(d, 0.0));
    vec3 p3 = getPosition(p + vec2(0.0, d));
    vec3 p4 = getPosition(p - vec2(0.0, d));

    vec3 dx = p1 - p2;
    vec3 dy = p3 - p4;

    return normalize(cross(dx, dy));
}

void main() {
    vec3 pos = getPosition(inPosition);

    height = pos.z;

    vNormal = mat3(transpose(inverse(model))) * getNormal(inPosition);
    vFragPos = vec3(model * vec4(pos, 1.0));

    gl_Position = projection * view * model * vec4(pos, 1.0);
}