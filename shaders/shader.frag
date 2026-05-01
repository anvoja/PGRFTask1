#version 330

in float height;
in vec3 vNormal;
in vec3 vFragPos;

uniform vec3 lightPosition;
uniform vec3 eyePosition;

out vec4 outColor;

void main() {
    float t = clamp(height + 0.5, 0.0, 1.0);

    vec3 lowColor = vec3(0.0, 1.0, 0.2);
    vec3 highColor = vec3(1.0, 0.0, 0.2);
    vec3 baseColor = mix(highColor, lowColor, t);

    vec3 normal = normalize(vNormal);
    vec3 lightDir = normalize(lightPosition - vFragPos);
    vec3 viewDir = normalize(eyePosition - vFragPos);

    float diffuse = max(dot(normal, lightDir), 0.0);

    vec3 reflectDir = reflect(lightDir, -normal);
    float specular = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);

    vec3 ambient = baseColor * 0.25;
    vec3 diffuseColor = baseColor * diffuse;
    vec3 specularColor = vec3(1.0) * specular * 0.4;

    outColor = vec4(ambient + diffuseColor + specularColor, 1.0);
}