#version 330

in vec3 vFragPos;

// Position of the light source in world space.
uniform vec3 pointLightPosition;
// Camera position in world space.
uniform vec3 eyePosition;
// Selects light mode
uniform int lightMode;
// Selects what is currently being rendered.
// 0 = wave surface
// 1 = visible reflector object
uniform int renderMode;
// Direction in which the reflector points.
uniform vec3 reflectorDirection;
// Inner and outer cutoff values for the reflector cone.
uniform float reflectorInnerCutOff;
uniform float reflectorOuterCutOff;

out vec3 outColor;

void main() {
    // 1 -> drawing the reflector object itself.
    // rendered as a simple yellow object without lighting.
    if (renderMode == 1) {
        outColor = vec3(1.0, 1.0, 0.0);
        return;
    }

    // Basic material color of the wave surface.
    vec3 baseColor = vec3(0.5, 0.7, 0.3);
    // Color of the light source.
    vec3 lightColor = vec3(1.0, 1.0, 1.0);

    // Calculate surface normal from screen-space derivatives.
    // dFdx and dFdy approximate tangent vectors of the surface.
    // Their cross product gives the fragment normal.
    vec3 normal = normalize(cross(dFdx(vFragPos), dFdy(vFragPos)));
    // Make sure the normal points toward the camera.
    normal = faceforward(normal, normalize(eyePosition - vFragPos), normal);

    // Vector from the fragment to the light source.
    vec3 lightVector = pointLightPosition - vFragPos;

    // Distance from fragment to light source for attenuation
    float distanceFromLight = length(lightVector);

    // Normalized light direction.
    vec3 lightDir = normalize(lightVector);

    // The minimum value 0.1 prevents the surface from becoming completely black.
    float diffuseFactor = max(dot(normal, lightDir), 0.1);

    // Direction from fragment to camera.
    vec3 viewDir = normalize(eyePosition - vFragPos);
    // Reflection direction used for specular/mirror lighting.
    vec3 reflectDir = reflect(-lightDir, normal);

    // Shininess controls the size of the specular highlight.
    // Higher value = smaller and sharper highlight.
    float shininess = 19.0;

    // Specular factor based on the angle between view direction and reflection direction.
    float specularFactor = pow(max(dot(viewDir, reflectDir), 0.0), shininess);

    // Strength of individual lighting components.
    float ambientIntensity = 0.15;
    float diffuseIntensity = 1.8;
    float specularIntensity = 1.8;

    // Attenuation coefficients for point light distance falloff.
    float constant = 1.0;
    float linear = 0.09;
    float quadratic = 0.32;

    // Point light attenuation.
    // Light becomes weaker as the distance from the light source increases.
    float attenuation = 1.0 / (constant + linear * distanceFromLight + quadratic * distanceFromLight * distanceFromLight);

    // Point light without directional lighting or attenuation.
    // This shows only the base material color affected by light color.
    vec3 pointLightOnly = baseColor * lightColor;

    // Ambient light is constant and independent of light direction.
    vec3 ambientLight = baseColor * lightColor * ambientIntensity;

    // Diffuse light depends on surface normal and light direction.
    vec3 diffuseLight = baseColor * lightColor * diffuseIntensity * diffuseFactor;

    // Specular light represents mirror-like reflection.
    vec3 specularLight = lightColor * specularIntensity * specularFactor;

    // Combined lighting without attenuation.
    vec3 fullLight = ambientLight + diffuseLight + specularLight;

    // direction from light source to fragment
    vec3 lightToFrag = normalize(vFragPos - pointLightPosition);
    vec3 spotDirection = normalize(reflectorDirection);

    // Cosine of the angle between reflector direction and fragment direction.
    // If theta is high, the fragment is close to the center of the reflector beam.
    float theta = dot(lightToFrag, spotDirection);

    // Smooth transition between inner and outer reflector cone.
    // Inside inner cutoff: fully lit.
    // Outside outer cutoff: mostly dark.
    // Between them: smoothly faded.
    float spotIntensity = smoothstep(reflectorOuterCutOff, reflectorInnerCutOff, theta);

    // Select which lighting model should be displayed.
    switch (lightMode) {
        case 0:
            // Basic point light visualization.
            // No diffuse, ambient, specular or attenuation is applied.
            outColor = pointLightOnly;
            break;
        case 1:
            // Diffuse component only.
            // Shows how the surface orientation affects lighting.
            outColor = diffuseLight;
            break;
        case 2:
            // Ambient + diffuse lighting.
            // Ambient light makes dark parts slightly visible.
            outColor = ambientLight + diffuseLight;
            break;
        case 3:
            // Ambient + diffuse + specular/mirror component.
            outColor = fullLight;
            break;
        case 4:
            // Full lighting with distance attenuation.
            outColor = fullLight * attenuation;
            break;

        case 5:
            // Reflector / spotlight.
            // The light is limited by a cone defined by reflectorDirection
            // and inner/outer cutoff angles.
            //
            // max(spotIntensity, 0.05) keeps a very small amount of light
            // outside the cone so the surface does not disappear completely.
            outColor = fullLight * attenuation * max(spotIntensity, 0.05);
            break;

        default:
            outColor = baseColor;
            break;
    }
}
