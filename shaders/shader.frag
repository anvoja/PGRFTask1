#version 330

in float height;
in vec3 vNormal;
in vec3 vFragPos;

uniform vec3 lightPosition;

out vec4 outColor;

void main() {
    float t = clamp(height + 0.5, 0.0, 1.0);

    vec3 lowColor = vec3(0.0, 1.0, 0.2);
    vec3 highColor = vec3(1.0, 0.0, 0.2);
    vec3 baseColor = mix(highColor, lowColor, t);

    vec3 normal = normalize(vNormal);
    vec3 lightDir = normalize(lightPosition - vFragPos);

    float diffuse = max(dot(normal, lightDir), 0.0);

    vec3 ambient = baseColor * 0.25;
    vec3 diffuseColor = baseColor * diffuse;

    outColor = vec4(ambient + diffuseColor, 1.0);
}