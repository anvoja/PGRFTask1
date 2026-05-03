#version 330

in float height;
in vec3 oNormal;
in vec3 vNormal;
in vec3 vFragPos;
in vec3 viewPosition;
in vec2 texCoord;

uniform vec3 lightPosition;
uniform vec3 eyePosition;
uniform int colorMode;
uniform sampler2D textureSampler;

out vec4 outColor;

const float near = 0.1;
const float far = 100.0;

float linearizeDepth(float depth) {
    float z = depth * 2.0 - 1.0;
    return (2.0 * near * far) / (far + near - z * (far - near));
}

void main() {
    vec3 normal = normalize(vNormal);
    vec3 lightDir = normalize(lightPosition - vFragPos);
    vec3 viewDir = normalize(eyePosition - vFragPos);

    float diffuse = max(dot(normal, lightDir), 0.0);

    vec3 reflectDir = reflect(lightDir, -normal);
    float specular = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);

    vec3 color;

    switch(colorMode) {
        case 0:
            // 1) xyz in view space
            color = abs(normalize(viewPosition));
            break;
        case 1:
            // 2) depth
            float d = linearizeDepth(gl_FragCoord.z) / far;
            color = vec3(d);
            break;
        case 2:
            // 3) normals
            vec3 n = normalize(oNormal);
            color = n * 0.5 + 0.5;
            break;
        case 3:
            // 6) map texture with coordinates
            vec4 texColor = texture(textureSampler, texCoord);
            color = texColor.rgb;
            break;
        case 4:

        default:
            color = vec3(1.0, 0.0, 1.0); // debug pink
    }

    outColor = vec4(color, 1.0);
}