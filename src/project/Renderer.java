package project;

import helpers.AbstractRenderer;
import lwjglutils.*;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import transforms.*;

import java.io.IOException;
import java.util.ArrayList;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Renderer extends AbstractRenderer {
    // Main geometry buffers for procedural surfaces.
    // triangle indices
    private OGLBuffers buffers;
    //triangle strip indices.
    private OGLBuffers stripBuffers;

    // Buffer and model for the predefined object
    private OGLBuffers objectBuffers;
    private OGLModelOBJ objectModel;

    // Buffer for the visible light source marker
    private OGLBuffers reflectorBuffer;

    // Render target used for rendering the scene into a texture.
    // This is used in the ambient occlusion
    private OGLRenderTarget renderTarget;

    // Text renderer for displaying current mode information.
    private OGLTextRenderer textRenderer;

    // texture
    private OGLTexture2D.Viewer textureViewer;
    private OGLTexture2D texture;

    // GL_TRIANGLES = regular triangle mesh
    // GL_TRIANGLE_STRIP = triangle strip mesh
    private int renderMode = GL_TRIANGLES;

    // 0 = procedural surface - colorMode pick
    // 1 = object
    // 2 = light - light sources on wave
    // 3 = ambient occlusion
    private int sceneMode = 0;

    private int shaderProgram;          // procedural surface
    private int objectShaderProgram;    // predefined object
    private int lightShaderProgram;     // light sources
    private int aoShaderProgram;        // ambient occlusion

    // transformation matrices
    private Mat4 projection;
    private Mat4 view;
    private Mat4 model;

    // procedural surface.
    // 0 = plane
    // 1 = wave
    // 2 = sphere
    // 3 = flower
    // 4 = cylinder
    private int surfaceMode = 1;

    // mode for frag of procedural surface shader
    private int colorMode = 0;
    // mode for frag of light shader
    private int lightMode = 0;
    // mode for picking object view
    private int polygonMode = GL_FILL;
    // mode for Ambient oclustion
    private int aoMode = 0;

    // Uniform locations for the procedural surface shader.
    private int locProjection;
    private int locView;
    private int locModel;
    private int locTime;
    private int locSurfaceMode;
    private int locLightPosition;
    private int locEyePosition;
    private int locColorMode;

    // Uniform location for the predefined object shader.
    private int locObjMat;

    // Uniform locations for the lighting shader.
    private int locLightMode;
    private int locLightProjection;
    private int locLightModel;
    private int locLightTime;
    private int locLightView;
    private int locLightPointLightPosition;
    private int locLightEyePosition;
    private int locLightRenderMode;
    private int locLightReflectorDirection;
    private int locLightReflectorInnerCutOff;
    private int locLightReflectorOuterCutOff;

    // Uniform locations for the ambient occlusion shader.
    private int locAoProjection;
    private int locAoView;
    private int locAoModel;
    private int locAoTime;

    private double objectRotation = 0.0;

    // Projection type.
    // true = perspective projection
    // false = orthographic projection
    private boolean perspectiveProjection = true;

    private boolean mousePressed = false;
    private double lastMouseX;
    private double lastMouseY;

    // Camera orientation and position.
    // azimuth = horizontal camera angle
    // zenith = vertical camera angle
    private double azimuth = Math.PI / 2 + Math.PI;
    private double zenith = 0.2;
    private Vec3D cameraPos = new Vec3D(0.2, 0.0, -0.5);

    // Camera direction vectors calculated every frame.
    private Vec3D lookForward;
    private Vec3D moveForward;
    private Vec3D right;

    // changes speed
    private double speed = 0.1;

    // position of the light source
    private Vec3D pointLightSourcePosition = new Vec3D(-1.5, -1.5, -2.0);

    // reflector direction angles
    // reflectorAzimuth controls left/right rotation.
    // reflectorZenith controls up/down rotation.
    private double reflectorAzimuth = Math.toRadians(90.0);
    private double reflectorZenith = Math.toRadians(-35.0);
    // Reflector cone angles.
    private double reflectorInnerAngle = 8.0;
    private double reflectorOuterAngle = 18.0;

    // grid resolution
    private int N = 150;

    private GLFWKeyCallback   keyCallback = new GLFWKeyCallback() {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            if (action == GLFW_PRESS || action == GLFW_REPEAT){
                switch (key) {
                    // polygon display mode selection
                    case GLFW_KEY_N:
                        // show object in lines
                        polygonMode = GL_LINE;
                        break;
                    case GLFW_KEY_B:
                        // show object as wireframe
                        polygonMode = GL_POINT;
                        break;
                    case GLFW_KEY_M:
                        // show object as solid
                        polygonMode = GL_FILL;
                        break;

                    // procedural surface selection
                    case GLFW_KEY_1:
                        surfaceMode = 0; // Cartesian plane
                        break;
                    case GLFW_KEY_2:
                        surfaceMode = 1; // Cartesian wave
                        break;
                    case GLFW_KEY_3:
                        surfaceMode = 2; // Spherical sphere
                        break;
                    case GLFW_KEY_4:
                        surfaceMode = 3; // Spherical flower
                        break;
                    case GLFW_KEY_5:
                        surfaceMode = 4; // Cylindrical cylinder
                        break;

                    // switch between scenes
                    case GLFW_KEY_P:
                        // object mode
                        sceneMode = (sceneMode + 1) % 4;
                        switch (sceneMode) {
                            case 0:
                                // procedural surfaces
                                cameraPos = new Vec3D(0.2, 0.0, -0.5);
                                azimuth = Math.PI / 2 + Math.PI;
                                zenith = 0.2;
                                break;
                            case 1:
                                // predefined object
                                cameraPos = new Vec3D(0.2, -0.8, -0.5);
                                azimuth = 6;
                                zenith = 0.5;
                                break;
                            case 2:
                                // light demonstration
                                cameraPos = new Vec3D(0.5, -0, -0.7);
                                azimuth = Math.PI / 2 + Math.PI;
                                zenith = 0.5;
                                break;
                            case 3:
                                // ambient occlusion
                                cameraPos = new Vec3D(0.0, -0.2, -0.8);
                                azimuth = 2;
                                zenith = 1.5;
                                break;
                        }
                        break;

                    // triangle or triangle strip rendering
                    case GLFW_KEY_L:
                        // object as regular triangles
                        renderMode = GL_TRIANGLES;
                        break;
                    case GLFW_KEY_K:
                        // object as strip of triangles
                        renderMode = GL_TRIANGLE_STRIP;
                        break;

                    // object rotation
                    case GLFW_KEY_LEFT:
                        objectRotation -= 0.2;
                        break;
                    case GLFW_KEY_RIGHT:
                        objectRotation += 0.2;
                        break;

                    // camera movement
                    case GLFW_KEY_W:
                        cameraPos = cameraPos.add(moveForward.mul(speed));
                        System.out.println(cameraPos);
                        break;
                    case GLFW_KEY_S:
                        cameraPos = cameraPos.sub(moveForward.mul(speed));
                        System.out.println(cameraPos);
                        break;
                    case GLFW_KEY_A:
                        cameraPos = cameraPos.add(right.mul(speed));
                        System.out.println(cameraPos);
                        break;
                    case GLFW_KEY_D:
                        cameraPos = cameraPos.sub(right.mul(speed));
                        System.out.println(cameraPos);
                        break;
                    case GLFW_KEY_SPACE:
                        cameraPos = cameraPos.add(new Vec3D(0, 0, speed));
                        break;
                    case GLFW_KEY_LEFT_SHIFT:
                        cameraPos = cameraPos.sub(new Vec3D(0, 0, speed));
                        break;

                    // projection perspective or orthographic
                    case GLFW_KEY_I:
                        // distant objects looks smaller
                        perspectiveProjection = true;
                        break;
                    case GLFW_KEY_U:
                        // size does not change with distance
                        perspectiveProjection = false;
                        break;

                    // color mode selection for procedural surfaces
                    case GLFW_KEY_C:
                        colorMode = (colorMode + 1) % 6;
                        break;

                    // light mode selection for light demonstration
                    case GLFW_KEY_H:
                        lightMode = (lightMode + 1) % 6;
                        break;

                    // light position movement for light demonstration
                    case GLFW_KEY_KP_4:
                        // X left
                        pointLightSourcePosition = pointLightSourcePosition.add(new Vec3D(-speed, 0, 0));
                        break;
                    case GLFW_KEY_KP_6:
                        // X right
                        pointLightSourcePosition = pointLightSourcePosition.add(new Vec3D(speed, 0, 0));
                        break;
                    case GLFW_KEY_KP_8:
                        // Y forward
                        pointLightSourcePosition = pointLightSourcePosition.add(new Vec3D(0, speed, 0));
                        break;
                    case GLFW_KEY_KP_2:
                        // Y backward
                        pointLightSourcePosition = pointLightSourcePosition.add(new Vec3D(0, -speed, 0));
                        break;
                    case GLFW_KEY_KP_5:
                        // Y backward
                        pointLightSourcePosition = pointLightSourcePosition.add(new Vec3D(0, 0, speed));
                        break;
                    case GLFW_KEY_KP_0:
                        // Y backward
                        pointLightSourcePosition = pointLightSourcePosition.add(new Vec3D(0, 0, -speed));
                        break;

                    // rotate reflector direction
                    case GLFW_KEY_KP_7:
                        // rotate reflector left
                        reflectorAzimuth -= 0.05;
                        break;
                    case GLFW_KEY_KP_9:
                        // rotate reflector right
                        reflectorAzimuth += 0.05;
                        break;
                    case GLFW_KEY_KP_1:
                        // rotate reflector down
                        reflectorZenith -= 0.05;
                        reflectorZenith = Math.min(Math.PI / 2 - 0.01, reflectorZenith);
                        break;
                    case GLFW_KEY_KP_3:
                        // rotate reflector up
                        reflectorZenith += 0.05;
                        reflectorZenith = Math.max(-Math.PI / 2 + 0.01, reflectorZenith);
                        break;

                    // modify reflector cone angle
                    case GLFW_KEY_Y:
                        // narrower reflector cone
                        reflectorInnerAngle = Math.max(2.0, reflectorInnerAngle - 1.0);
                        reflectorOuterAngle = Math.max(reflectorInnerAngle + 2.0, reflectorOuterAngle - 1.0);
                        break;
                    case GLFW_KEY_R:
                        // wider reflector cone
                        reflectorInnerAngle = Math.min(40.0, reflectorInnerAngle + 1.0);
                        reflectorOuterAngle = Math.min(60.0, reflectorOuterAngle + 1.0);
                        break;

                    // aim reflector to the center of the surface
                    case GLFW_KEY_T:
                        aimReflectorAt(new Vec3D(-1.5, 0.0, 0.0));
                        break;

                    // ambient occlusion mode selection
                    case GLFW_KEY_O:
                        aoMode = (aoMode + 1) % 3;
                        break;
                }
            }
        }
    };

    @Override
    public void init() {
        // print info
        OGLUtils.printOGLparameters();
        OGLUtils.printLWJLparameters();
        OGLUtils.printJAVAparameters();
        OGLUtils.shaderCheck();

        // modify point size for better visibility in points mode
        glPointSize(5f);

        // ------------------------------------------------------------
        // Create procedural grid vertices.
        // The grid is created in the interval [-1, 1] x [-1, 1].
        // The z-coordinate is initially 0; the shader later modifies it.
        // ------------------------------------------------------------
        float[] vertexBufferData = new float[N * N * 3];
        int index = 0;

        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                float fx = -1f + 2f * x / (N - 1);
                float fy = -1f + 2f * y / (N - 1);

                vertexBufferData[index++] = fx;
                vertexBufferData[index++] = fy;
                vertexBufferData[index++] = 0f;
            }
        }

        // ------------------------------------------------------------
        // Create triangle index buffer.
        // ------------------------------------------------------------
        int[] indexBufferData = new int[(N - 1) * (N - 1) * 6];
        ArrayList<Integer> stripList = new ArrayList<>();

        for (int y = 0; y < N - 1; y++) {
            if (y > 0) {
                stripList.add(y * N);
            }

            for (int x = 0; x < N; x++) {
                stripList.add(y * N + x);
                stripList.add((y + 1) * N + x);
            }

            if (y < N - 2) {
                stripList.add((y + 1) * N + (N - 1));
            }
        }

        // ------------------------------------------------------------
        // Create triangle-strip index buffer.
        // Degenerate vertices are inserted between rows.
        // ------------------------------------------------------------
        int[] stripIndices = stripList.stream().mapToInt(i -> i).toArray();
        index = 0;

        for (int y = 0; y < N - 1; y++) {
            for (int x = 0; x < N - 1; x++) {
                int i = y * N + x;

                indexBufferData[index++] = i;
                indexBufferData[index++] = i + 1;
                indexBufferData[index++] = i + N;

                indexBufferData[index++] = i + 1;
                indexBufferData[index++] = i + N + 1;
                indexBufferData[index++] = i + N;
            }
        }

        // Vertex attribute layout.
        // The shader expects input variable named "inPosition".
        OGLBuffers.Attrib[] attributes = {
          new OGLBuffers.Attrib("inPosition", 3, 0)
        };

        // ------------------------------------------------------------
        // Create simple reflector geometry.
        // The tip points in local +X direction.
        // Later, the model matrix rotates it toward reflectorDirection.
        // ------------------------------------------------------------
        float[] reflectorData = {
            // tip
            0.35f, 0.0f, 0.0f,

            // back square
            0.0f, -0.15f, -0.15f,
            0.0f,  0.15f, -0.15f,
            0.0f,  0.15f,  0.15f,
            0.0f, -0.15f,  0.15f
        };

        int[] reflectorIndices = {
            // sides
            0, 1, 2,
            0, 2, 3,
            0, 3, 4,
            0, 4, 1,

            // back face
            1, 4, 3,
            1, 3, 2
        };

        OGLBuffers.Attrib[] reflectorAttributes = {
                new OGLBuffers.Attrib("inPosition", 3, 0)
        };

        // Load texture used by the procedural surface shader.
        try {
            texture = new OGLTexture2D("textures/mosaic.jpg");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Load predefined OBJ model.
        objectModel = new OGLModelOBJ("/obj/vase.obj");
        objectBuffers = objectModel.getBuffers();

        // Create OpenGL buffers from generated vertex/index data.
        buffers = new OGLBuffers(vertexBufferData, 3, attributes, indexBufferData);
        stripBuffers = new OGLBuffers(vertexBufferData, 3, attributes, stripIndices);
        reflectorBuffer = new OGLBuffers( reflectorData, 3, reflectorAttributes, reflectorIndices);

        // Render target for render-to-texture.
        // Used by the ambient occlusion / deferred shading scene.
        renderTarget = new OGLRenderTarget(width, height);
        textureViewer = new OGLTexture2D.Viewer();

        // Load shader programs.
        shaderProgram = ShaderUtils.loadProgram("/shader");
        objectShaderProgram = ShaderUtils.loadProgram("/obj");
        lightShaderProgram = ShaderUtils.loadProgram("/light");
        aoShaderProgram = ShaderUtils.loadProgram("/ao");

        // Get uniform locations for all shader programs
        locProjection = glGetUniformLocation(shaderProgram, "projection");
        locView = glGetUniformLocation(shaderProgram, "view");
        locModel = glGetUniformLocation(shaderProgram, "model");
        locTime = glGetUniformLocation(shaderProgram, "time");
        locSurfaceMode = glGetUniformLocation(shaderProgram, "surfaceMode");
        locLightPosition = glGetUniformLocation(shaderProgram, "lightPosition");
        locEyePosition = glGetUniformLocation(shaderProgram, "eyePosition");
        locColorMode = glGetUniformLocation(shaderProgram, "colorMode");

        locObjMat = glGetUniformLocation(objectShaderProgram, "mat");

        locLightMode = glGetUniformLocation(lightShaderProgram, "lightMode");
        locLightProjection = glGetUniformLocation(lightShaderProgram, "projection");
        locLightModel = glGetUniformLocation(lightShaderProgram, "model");
        locLightTime = glGetUniformLocation(lightShaderProgram, "time");
        locLightView = glGetUniformLocation(lightShaderProgram, "view");
        locLightPointLightPosition = glGetUniformLocation(lightShaderProgram, "pointLightPosition");
        locLightEyePosition = glGetUniformLocation(lightShaderProgram, "eyePosition");
        locLightRenderMode = glGetUniformLocation(lightShaderProgram, "renderMode");
        locLightReflectorDirection = glGetUniformLocation(lightShaderProgram, "reflectorDirection");
        locLightReflectorInnerCutOff = glGetUniformLocation(lightShaderProgram, "reflectorInnerCutOff");
        locLightReflectorOuterCutOff = glGetUniformLocation(lightShaderProgram, "reflectorOuterCutOff");

        locAoProjection = glGetUniformLocation(aoShaderProgram, "projection");
        locAoView = glGetUniformLocation(aoShaderProgram, "view");
        locAoModel = glGetUniformLocation(aoShaderProgram, "model");
        locAoTime = glGetUniformLocation(aoShaderProgram, "time");

        // Initialize 2D text renderer.
        textRenderer = new OGLTextRenderer(width, height);
        textRenderer.resize(width, height);

        // Set initial reflector direction to point at the surface center.
        aimReflectorAt(new Vec3D(-1.5, 0.0, 0.0));
    }

    @Override
    public void display() {
        // Clear color and depth buffers at the beginning of every frame.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        // Set viewport to the whole window.
        glViewport(0, 0, width, height);
        // Apply selected polygon rendering mode.
        glPolygonMode(GL_FRONT_AND_BACK, polygonMode);

        // ------------------------------------------------------------
        // Calculate camera direction vectors from azimuth and zenith.
        // lookForward is used for looking direction.
        // moveForward is used for walking without vertical movement.
        // right is used for side movement.
        // ------------------------------------------------------------
        lookForward = new Vec3D(
                Math.cos(zenith) * Math.sin(azimuth),
                Math.cos(zenith) * Math.cos(azimuth),
                Math.sin(zenith)
        ).normalized().get();

        // walking direction - no Z movement
        moveForward = new Vec3D(
                Math.sin(azimuth),
                Math.cos(azimuth),
                0
        ).normalized().get();

        right = moveForward.cross(new Vec3D(0, 0, 1)).normalized().get();

        if (height == 0) return;
        double aspect = (double) width / height;

        // Create projection matrix.
        // Perspective projection makes distant objects smaller.
        // Orthographic projection keeps object size independent of distance.
        if (perspectiveProjection) {
            projection = new Mat4PerspRH(
                    60,
                    aspect,
                    0.1,
                    100.0
            );
        } else {
            projection = new Mat4OrthoRH(
                    6 * aspect,
                    6,
                    0.1,
                    100.0
            );
        }

        // Create view matrix from camera position and viewing direction.
        view = new Mat4ViewRH(
                cameraPos,
                cameraPos.add(lookForward),
                new Vec3D(0, 0, 1)
        );

        // Default model matrix for procedural surface.
        model = new Mat4Transl(-1.5, 0, 0)
                .mul(new Mat4RotX(objectRotation))
                .mul(new Mat4Scale(0.7));

        float time = (float) glfwGetTime();

        // ------------------------------------------------------------
        // Render selected scene.
        // ------------------------------------------------------------
        switch (sceneMode) {
            // procedural surface scene
            case 0:
                glUseProgram(shaderProgram);
                glUniform1f(locTime, time);
                glUniform1i(locSurfaceMode, surfaceMode);
                glUniformMatrix4fv(locView, false, view.floatArray());
                glUniformMatrix4fv(locProjection, false, projection.floatArray());
                glUniform3f(locEyePosition, (float) cameraPos.getX(), (float) cameraPos.getY(), (float) cameraPos.getZ());
                glUniform3f(locLightPosition, 2.0f, -3.0f, 4.0f);
                glUniformMatrix4fv(locModel, false, model.floatArray());
                glUniform1i(locColorMode, colorMode);
                texture.bind(shaderProgram, "textureSampler", 0);

                if (surfaceMode == 1) {
                    // two different objects in one scene
                    model = new Mat4Transl(-2, -1.5, 0);
                    glUniformMatrix4fv(locModel, false, model.floatArray());
                    glUniform1i(locSurfaceMode, 1);
                    if (renderMode == GL_TRIANGLES) {
                        buffers.draw(GL_TRIANGLES, shaderProgram);
                    } else {
                        stripBuffers.draw(GL_TRIANGLE_STRIP, shaderProgram);
                    }

                    model = new Mat4Transl(-2, 2, 0);
                    glUniformMatrix4fv(locModel, false, model.floatArray());
                    glUniform1i(locSurfaceMode, 5);
                    if (renderMode == GL_TRIANGLES) {
                        buffers.draw(GL_TRIANGLES, shaderProgram);
                    } else {
                        stripBuffers.draw(GL_TRIANGLE_STRIP, shaderProgram);
                    }
                } else {
                    if (renderMode == GL_TRIANGLES) {
                        buffers.draw(GL_TRIANGLES, shaderProgram);
                    } else {
                        stripBuffers.draw(GL_TRIANGLE_STRIP, shaderProgram);
                    }
                }
                break;
            case 1:
                // object scene
                glUseProgram(objectShaderProgram);

                // Coordinate correction matrix for the imported model.
                Mat4 rotate= new Mat4(new double[] {
                        1,  0,  0, 0,
                        0, -1,  0, 0,
                        0,  0,  1, 0,
                        0,  0,  0, 1,
                });

                Mat4 objectModelMatrix = new Mat4Scale(0.2);
                Mat4 mat = rotate.mul(view).mul(objectModelMatrix).mul(rotate);

                glUniformMatrix4fv(locObjMat, false, ToFloatArray.convert(mat));

                objectBuffers.draw(objectModel.getTopology(), objectShaderProgram);
                break;

            // lighting scene
            case 2:
                glUseProgram(lightShaderProgram);

                glUniform1f(locLightTime, time);
                glUniformMatrix4fv(locLightProjection, false, projection.floatArray());
                glUniformMatrix4fv(locLightView, false, view.floatArray());
                glUniform3f(locLightPointLightPosition, (float) pointLightSourcePosition.getX(), (float) pointLightSourcePosition.getY(), (float) pointLightSourcePosition.getZ());
                glUniform3f(locLightEyePosition, (float) cameraPos.getX(), (float) cameraPos.getY(), (float) cameraPos.getZ());
                glUniform1i(locLightMode, lightMode);

                // Calculate reflector direction from reflectorAzimuth and reflectorZenith.
                Vec3D reflectorDirection = getReflectorDirection();

                glUniform3f(locLightReflectorDirection, (float) reflectorDirection.getX(), (float) reflectorDirection.getY(), (float) reflectorDirection.getZ());
                glUniform1f(locLightReflectorInnerCutOff, (float) Math.cos(Math.toRadians(reflectorInnerAngle)));
                glUniform1f(locLightReflectorOuterCutOff, (float) Math.cos(Math.toRadians(reflectorOuterAngle)));

                // wave
                glUniform1i(locLightRenderMode, 0);
                glUniformMatrix4fv(locLightModel, false, model.floatArray());
                buffers.draw(GL_TRIANGLES, lightShaderProgram);

                // reflector
                Mat4 reflectorModel = createReflectorModel(pointLightSourcePosition, reflectorDirection);

                glUniform1i(locLightRenderMode, 1);
                glUniformMatrix4fv(locLightModel, false, reflectorModel.floatArray());

                reflectorBuffer.draw(GL_TRIANGLES, lightShaderProgram);

                break;

            // ambient occlusion
            case 3:
                renderTarget.bind();

                glViewport(0, 0, width, height);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

                glUseProgram(aoShaderProgram);

                glUniform1f(locAoTime, time);
                glUniformMatrix4fv(locAoProjection, false, projection.floatArray());
                glUniformMatrix4fv(locAoView, false, view.floatArray());
                glUniformMatrix4fv(locAoModel, false, model.floatArray());

                glViewport(0, 0, width, height);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

                buffers.draw(GL_TRIANGLES, aoShaderProgram);

                glBindFramebuffer(GL_FRAMEBUFFER, 0);

                textureViewer.view(renderTarget.getColorTexture(), -1, -1, 2, 2);
                break;
        }

        // Make sure text is rendered to the main window, not into the render target.
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, width, height);
        glUseProgram(0);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        // Draw current mode description.
        textRenderer.clear();
        textRenderer.addStr2D(20, 20, getColorModeText());
        textRenderer.draw();
    }

    /**
     * Calculates reflector direction from spherical angles.
     *
     * reflectorAzimuth controls horizontal direction.
     * reflectorZenith controls vertical direction.
     *
     * @return normalized direction vector of the reflector
     */
    private Vec3D getReflectorDirection() {
        return new Vec3D(
                Math.cos(reflectorZenith) * Math.sin(reflectorAzimuth),
                Math.cos(reflectorZenith) * Math.cos(reflectorAzimuth),
                Math.sin(reflectorZenith)
        ).normalized().get();
    }

    /**
     * Aims the reflector at a selected world-space target.
     * The direction from the light source to the target is converted
     * into azimuth and zenith angles.
     *
     * @param target world-space position where reflector should point
     */
    private void aimReflectorAt(Vec3D target) {
        Vec3D d = target.sub(pointLightSourcePosition).normalized().get();

        reflectorAzimuth = Math.atan2(d.getX(), d.getY());
        reflectorZenith = Math.asin(d.getZ());
    }

    /**
     * Creates model matrix for the visible reflector object.
     * The reflector mesh is modeled to point in local +X direction.
     * This method rotates it so that it points in the actual reflector direction.
     *
     * @param position world-space position of the reflector
     * @param direction direction where reflector should point
     * @return model matrix for reflector object
     */
    private Mat4 createReflectorModel(Vec3D position, Vec3D direction) {
        Vec3D d = direction.normalized().get();

        double yaw = Math.atan2(d.getY(), d.getX());
        double pitch = -Math.asin(d.getZ());

        return new Mat4Transl(
                position.getX(),
                position.getY(),
                position.getZ()
        )
                .mul(new Mat4RotZ(yaw))
                .mul(new Mat4RotY(pitch))
                .mul(new Mat4Scale(0.8));
    }

    /**
     * Mouse button callback.
     * Stores whether the left mouse button is pressed.
     * When the button is pressed, the current cursor position is saved
     * so that camera rotation can be calculated from mouse movement.
     */
    private GLFWMouseButtonCallback mouseCallback = new GLFWMouseButtonCallback() {
        @Override
        public void invoke(long window, int button, int action, int mods) {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                mousePressed = action == GLFW_PRESS;

                double[] x = new double[1];
                double[] y = new double[1];
                glfwGetCursorPos(window, x, y);

                lastMouseX = x[0];
                lastMouseY = y[0];
            }
        }
    };

    /**
     * Mouse movement callback.
     * If the left mouse button is pressed, mouse movement changes
     * camera azimuth and zenith.
     */
    private GLFWCursorPosCallback cursorPosCallback = new GLFWCursorPosCallback() {
        @Override
        public void invoke(long window, double x, double y) {
            if (!mousePressed) return;

            double dx = x - lastMouseX;
            double dy = y - lastMouseY;

            lastMouseX = x;
            lastMouseY = y;

            azimuth += dx * 0.01;
            zenith -= dy * 0.01;

            // prevent camera flipping upside down
            zenith = Math.max(-Math.PI / 2 + 0.01, Math.min(Math.PI / 2 - 0.01, zenith));

            System.out.println("azimuth: " + azimuth + " zenith: " + zenith);
        }
    };

    /**
     * Returns text description of the currently selected scene and mode.
     * This text is rendered in the top-left corner of the window.
     *
     * @return description of current render mode
     */
    private String getColorModeText() {
        switch (sceneMode) {
            case 0:
                switch (colorMode) {
                    case 0: return "Color mode: XYZ in observer coordinates";
                    case 1: return "Color mode: Depth buffer";
                    case 2: return "Color mode: Normal XYZ";
                    case 3: return "Color mode: Texture";
                    case 4: return "Color mode: Lighting without texture";
                    case 5: return "Color mode: Complete lighting with texture";
                    case 6: return "Color mode: Distance from light";
                    default: return "Color mode: Unknown";
                }
            case 1:
                return "Predefined object";
            case 2:
                switch (lightMode) {
                    case 0: return "Point light source";
                    case 1: return "Point light + diffuse";
                    case 2: return "Point light + diffuse + ambient";
                    case 3: return "Point light + diffuse + ambient + mirror";
                    case 4: return "Point light + diffuse + ambient + mirror + attenuation";
                    case 5: return "Reflector / spotlight";
                }
            case 3:
                switch (aoMode) {
                    case 0: return "Ambient occlusion: deferred shading first pass";
                }
            default: return "Color mode: Unknown";
        }
    }

    @Override
    public GLFWMouseButtonCallback getMouseCallback() {
        return mouseCallback;
    }

    @Override
    public GLFWCursorPosCallback getCursorPosCallback() {
        return cursorPosCallback;
    }

    @Override
    public GLFWKeyCallback getKeyCallback() {
        return keyCallback;
    }

    @Override
    public GLFWScrollCallback getScrollCallback() {
        return scrollCallback;
    }
}
