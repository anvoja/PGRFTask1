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
out vec3 oNormal;
out vec3 vFragPos;
out vec3 viewPosition;
out vec2 texCoord;

vec3 pos;

// Cartesian plane
vec3 plane(vec2 p) {
    return vec3(p.x, p.y, 0.0);
}

// Cartesian wave
vec3 wave(vec2 p) {
    float x = p.x;
    float y = p.y;
    float z = sin(5.0 * x + time) * cos(5.0 * y + time) * 0.3;
    return vec3(x, y, z);
}

// Spherical sphere
vec3 sphere(vec2 p) {
    float u = (p.x + 1.0) * PI;
    float v = (p.y + 1.0) * PI * 0.5;

    float r = 1.0;

    float x = r * sin(v) * cos(u);
    float y = r * sin(v) * sin(u);
    float z = r * cos(v);

    return vec3(x, y, z);
}

// Spherical flower
vec3 flower(vec2 p) {
    float u = (p.x + 1.0) * PI;
    float v = (p.y + 1.0) * PI * 0.5;

    float r = 1.0 + 0.25 * sin(6.0 * u + time) * cos(4.0 * v);

    float x = r * sin(v) * cos(u);
    float y = r * sin(v) * sin(u);
    float z = r * cos(v);

    return vec3(x, y, z);
}

vec3 cylinder(vec2 p) {
    float u = (p.x + 1.0) * PI;
    float z = p.y * 1.5;

    float r = 1.0;

    float x = r * cos(u);
    float y = r * sin(u);

    return vec3(x, y, z);
}

vec3 twistedCylinder(vec2 p) {
    float u = (p.x + 1.0) * PI;
    float z = p.y * 1.5;

    float r = 0.8 + 0.2 * sin(6.0 * u + time + z * 3.0);

    float x = r * cos(u);
    float y = r * sin(u);

    return vec3(x, y, z);
}

vec3 getPosition(vec2 p) {
    switch(surfaceMode) {
        case 0: return plane(p);          // Cartesian
        case 1: return wave(p);           // Cartesian
        case 2: return sphere(p);         // Spherical
        case 3: return flower(p);         // Spherical
        case 4: return cylinder(p);       // Cylindrical
        case 5: return twistedCylinder(p);// Cylindrical
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
    pos = getPosition(inPosition);

    height = pos.z;

    oNormal = mat3(transpose(inverse(view * model))) * getNormal(inPosition);
    vNormal = mat3(transpose(inverse(model))) * getNormal(inPosition);
    vFragPos = vec3(model * vec4(pos, 1.0));

    vec4 worldPosition = model * vec4(pos, 1.0);
    vec4 cameraPosition = view * worldPosition;

    viewPosition = cameraPosition.xyz;

    texCoord = inPosition.xy * 0.5 + 0.5;

    gl_Position = projection * cameraPosition;
}