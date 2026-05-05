#version 330

in vec3 vFragPos;

uniform vec3 pointLightPosition;
uniform vec3 eyePosition;
uniform int lightMode;
uniform int renderMode;
uniform vec3 reflectorDirection;
uniform float reflectorInnerCutOff;
uniform float reflectorOuterCutOff;

out vec3 outColor;

void main() {
    if (renderMode == 1) {
        outColor = vec3(1.0, 1.0, 0.0);
        return;
    }

    vec3 baseColor = vec3(0.5, 0.7, 0.3);
    vec3 lightColor = vec3(1.0, 1.0, 1.0);

    vec3 normal = normalize(cross(dFdx(vFragPos), dFdy(vFragPos)));
    normal = faceforward(normal, normalize(eyePosition - vFragPos), normal);

    vec3 lightVector = pointLightPosition - vFragPos;
    float distanceFromLight = length(lightVector);
    vec3 lightDir = normalize(lightVector);

    float diffuseFactor = max(dot(normal, lightDir), 0.08);

    vec3 viewDir = normalize(eyePosition - vFragPos);
    vec3 reflectDir = reflect(-lightDir, normal);

    float shininess = 19.0;
    float specularFactor = pow(max(dot(viewDir, reflectDir), 0.0), shininess);

    float ambientIntensity = 0.15;
    float diffuseIntensity = 1.8;
    float specularIntensity = 1.8;

    float constant = 1.0;
    float linear = 0.09;
    float quadratic = 0.32;

    float attenuation = 1.0 / (constant + linear * distanceFromLight + quadratic * distanceFromLight * distanceFromLight);

    vec3 pointLightOnly = baseColor * lightColor;
    vec3 ambientLight = baseColor * lightColor * ambientIntensity;
    vec3 diffuseLight = lightColor * diffuseIntensity * diffuseFactor;
    vec3 specularLight = lightColor * specularIntensity * specularFactor;
    vec3 fullLight = ambientLight + diffuseLight + specularLight;

    // direction from light source to fragment
    vec3 lightToFrag = normalize(vFragPos - pointLightPosition);
    vec3 spotDirection = normalize(reflectorDirection);

    float theta = dot(lightToFrag, spotDirection);

    float spotIntensity = smoothstep(reflectorOuterCutOff, reflectorInnerCutOff, theta);

    switch (lightMode) {
        case 0:
            outColor = pointLightOnly;
            break;

        case 1:
            outColor = diffuseLight;
            break;

        case 2:
            outColor = ambientLight + diffuseLight;
            break;

        case 3:
            outColor = fullLight;
            break;

        case 4:
            outColor = fullLight * attenuation;
            break;

        case 5:
            // reflector / spotlight
            outColor = fullLight * attenuation * max(spotIntensity, 0.05);
            break;

        default:
            outColor = baseColor;
            break;
    }
}
