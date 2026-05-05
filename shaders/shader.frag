#version 330

in float height;
in vec3 oNormal;
in vec3 vNormal;
in vec3 vFragPos;
in vec3 viewPosition;
in vec2 texCoord;

// Point light position in world space.
uniform vec3 lightPosition;
// Camera position in world space.
uniform vec3 eyePosition;
// Selects the coloring mode.
uniform int colorMode;
uniform sampler2D textureSampler;

out vec4 outColor;

// depth visualization.
const float near = 0.1;
const float far = 100.0;

// Converts non-linear depth buffer value into linear depth.
// gl_FragCoord.z is non-linear because of perspective projection.
float linearizeDepth(float depth) {
    float z = depth * 2.0 - 1.0;
    return (2.0 * near * far) / (far + near - z * (far - near));
}

vec3 getLightingColor(vec3 baseColor, float attenuation) {
    vec3 normal = normalize(vNormal);
    vec3 lightDir = normalize(lightPosition - vFragPos);
    vec3 viewDir = normalize(eyePosition - vFragPos);

    // The dot product gives cosine of the angle between normal and light direction.
    float diffuse = max(dot(normal, lightDir), 0.0);

    // Specular/mirror component.
    // Reflection direction is compared with the viewing direction.
    vec3 reflectDir = reflect(lightDir, -normal);
    float specular = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);

    // Ambient component makes surfaces visible even when not directly lit.
    vec3 ambientColor = baseColor * 0.25;
    // Diffuse component depends on light direction and attenuation.
    vec3 diffuseColor = baseColor * diffuse * attenuation;
    // Specular component is white and also affected by attenuation.
    vec3 specularColor = vec3(1.0) * specular * 0.4 * attenuation;

    return ambientColor + diffuseColor + specularColor;
}

// Calculates distance attenuation for a point light.
float getAttenuation(float distanceFromLight) {
    float constant = 1.0;
    float linear = 0.09;
    float quadratic = 0.032;

    return 1.0 / (
        constant +
        linear * distanceFromLight +
        quadratic * distanceFromLight * distanceFromLight
        );
}


// Calculates lighting including attenuation based on distance from light source.
vec3 getLightWithDistance(vec3 baseColor) {
    float distanceFromLight = length(lightPosition - vFragPos);

    float attenuation = getAttenuation(distanceFromLight);

    return getLightingColor(baseColor, attenuation);
}

void main() {
    vec3 normal = normalize(vNormal);
    vec3 lightDir = normalize(lightPosition - vFragPos);
    vec3 viewDir = normalize(eyePosition - vFragPos);

    float diffuse = max(dot(normal, lightDir), 0.0);

    vec3 reflectDir = reflect(lightDir, -normal);
    float specular = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);

    vec3 color;
    vec3 baseColor;

    // Select output mode.
    switch(colorMode) {
        case 0:
            // xyz in view space
            // abs() makes negative coordinates visible as positive colors.
            color = abs(normalize(viewPosition));
            break;
        case 1:
            // depth
            // Depth is linearized first because depth buffer values are non-linear.
            float d = linearizeDepth(gl_FragCoord.z) / far;
            color = vec3(d);
            break;
        case 2:
            // normals
            vec3 n = normalize(oNormal);
            color = n * 0.5 + 0.5;
            break;
        case 3:
            // map texture with coordinates
            vec4 texColor = texture(textureSampler, texCoord);
            color = texColor.rgb;
            break;
        case 4:
            // Lighting without texture.
            // A constant gray base color is used.
            baseColor = vec3(0.6, 0.6, 0.6);
            color = getLightingColor(baseColor, 1);
            break;
        case 5:
            // texture
            baseColor = texture(textureSampler, texCoord).rgb;
            color = getLightingColor(baseColor, 1);
            break;
        case 6:
            // texture with light and distance
            baseColor = texture(textureSampler, texCoord).rgb;
            color = getLightWithDistance(baseColor);
            break;

        default:
            color = vec3(1.0, 0.0, 1.0); // debug pink
    }

    outColor = vec4(color, 1.0);
}
