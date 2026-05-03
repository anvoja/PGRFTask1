package project;

import helpers.AbstractRenderer;
import lwjglutils.*;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import transforms.*;

import java.util.ArrayList;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

public class Renderer extends AbstractRenderer {
    private OGLBuffers buffers;
    private OGLBuffers stripBuffers;
    private OGLBuffers objectBuffers;
    private OGLModelOBJ objectModel;

    private int renderMode = GL_TRIANGLES;
    // 0 = procedural surface
    // 1 = object
    private int sceneMode = 0;

    private int shaderProgram;
    private int objectShaderProgram;

    private Mat4 projection;
    private Mat4 view;
    private Mat4 model;

    private int surfaceMode = 1;

    private int colorMode = 0;

    private int locProjection;
    private int locView;
    private int locModel;
    private int locTime;
    private int locSurfaceMode;
    private int locLightPosition;
    private int locEyePosition;
    private int locObjMat;
    private int locColorMode;

    private double objectRotation = 0.0;

    private boolean perspectiveProjection = true;

    private boolean mousePressed = false;

    private double lastMouseX;
    private double lastMouseY;

    private double azimuth = Math.PI / 2 + Math.PI;
    private double zenith = 0.2;
    private Vec3D cameraPos = new Vec3D(0.1, 0, -0.5);
    private Vec3D lookForward;
    private Vec3D moveForward;
    private Vec3D right;
    private double speed = 0.1;

    // grid resolution
    private int N = 30;

    private GLFWKeyCallback   keyCallback = new GLFWKeyCallback() {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            if (action == GLFW_PRESS || action == GLFW_REPEAT){
                switch (key) {
                    case GLFW_KEY_N:
                        // show object in lines
                        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                        break;
                    case GLFW_KEY_B:
                        // show object as wireframe
                        glPolygonMode(GL_FRONT_AND_BACK, GL_POINT);
                        break;
                    case GLFW_KEY_M:
                        // show object as solid
                        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
                        break;
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
                    case GLFW_KEY_P:
                        // object mode
                        sceneMode = 1; // object
                        break;
                    case GLFW_KEY_O:
                        // surface mode
                        sceneMode = 0; // surface
                        break;
                    case GLFW_KEY_L:
                        // object as regular triangles
                        renderMode = GL_TRIANGLES;
                        break;
                    case GLFW_KEY_K:
                        // object as strip of triangles
                        renderMode = GL_TRIANGLE_STRIP;
                        break;
                    case GLFW_KEY_LEFT:
                        objectRotation -= 0.2;
                        break;
                    case GLFW_KEY_RIGHT:
                        objectRotation += 0.2;
                        break;
                    case GLFW_KEY_W:
                        cameraPos = cameraPos.add(moveForward.mul(speed));
                        System.out.println(cameraPos);
                        break;
                    case GLFW_KEY_S:
                        cameraPos = cameraPos.sub(moveForward.mul(speed));
                        System.out.println(cameraPos);
                        break;
                    case GLFW_KEY_A:
                        cameraPos = cameraPos.sub(right.mul(speed));
                        System.out.println(cameraPos);
                        break;
                    case GLFW_KEY_D:
                        cameraPos = cameraPos.add(right.mul(speed));
                        System.out.println(cameraPos);
                        break;
                    case GLFW_KEY_SPACE:
                        cameraPos = cameraPos.add(new Vec3D(0, 0, speed));
                        break;
                    case GLFW_KEY_LEFT_SHIFT:
                        cameraPos = cameraPos.sub(new Vec3D(0, 0, speed));
                        break;
                    case GLFW_KEY_I:
                        // distant objects looks smaller
                        perspectiveProjection = true;
                        break;
                    case GLFW_KEY_U:
                        // size does not change with distance
                        perspectiveProjection = false;
                        break;
                    case GLFW_KEY_C:
                        colorMode = (colorMode + 1) % 8;
                        break;
                }
            }
        }
    };

    @Override
    public void init() {
        OGLUtils.printOGLparameters();
        OGLUtils.printLWJLparameters();
        OGLUtils.printJAVAparameters();
        OGLUtils.shaderCheck();
        glPointSize(5f);

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

        OGLBuffers.Attrib[] attributes = {
          new OGLBuffers.Attrib("inPosition", 3, 0)
        };

        objectModel = new OGLModelOBJ("/obj/vase.obj");
        objectBuffers = objectModel.getBuffers();

        buffers = new OGLBuffers(vertexBufferData, 3, attributes, indexBufferData);
        stripBuffers = new OGLBuffers(vertexBufferData, 3, attributes, stripIndices);

        shaderProgram = ShaderUtils.loadProgram("/shader");
        objectShaderProgram = ShaderUtils.loadProgram("/obj");

        locProjection = glGetUniformLocation(shaderProgram, "projection");
        locView = glGetUniformLocation(shaderProgram, "view");
        locModel = glGetUniformLocation(shaderProgram, "model");
        locTime = glGetUniformLocation(shaderProgram, "time");
        locSurfaceMode = glGetUniformLocation(shaderProgram, "surfaceMode");
        locLightPosition = glGetUniformLocation(shaderProgram, "lightPosition");
        locEyePosition = glGetUniformLocation(shaderProgram, "eyePosition");
        locColorMode = glGetUniformLocation(shaderProgram, "colorMode");
        locObjMat = glGetUniformLocation(objectShaderProgram, "mat");

        textRenderer = new OGLTextRenderer(width, height);
        textRenderer.resize(width, height);
    }

    @Override
    public void display() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, width, height);

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

        view = new Mat4ViewRH(
                cameraPos,
                cameraPos.add(lookForward),
                new Vec3D(0, 0, 1)
        );

        model = new Mat4Transl(-1.5, 0, 0)
                .mul(new Mat4RotX(objectRotation))
                .mul(new Mat4Scale(0.7));

        float time = (float) glfwGetTime();

        if (sceneMode == 0) {
            glUseProgram(shaderProgram);
            glUniform1f(locTime, time);
            glUniform1i(locSurfaceMode, surfaceMode);
            glUniformMatrix4fv(locView, false, view.floatArray());
            glUniformMatrix4fv(locProjection, false, projection.floatArray());
            glUniform3f(locEyePosition, (float) cameraPos.getX(), (float) cameraPos.getY(), (float) cameraPos.getZ());
            glUniform3f(locLightPosition, 2.0f, -3.0f, 4.0f);
            glUniformMatrix4fv(locModel, false, model.floatArray());
            glUniform1i(locColorMode, colorMode);

            if (surfaceMode == 1) {
                model = new Mat4Transl(-2, -1.5, 0);
                glUniformMatrix4fv(locModel, false, model.floatArray());
                glUniform1i(locSurfaceMode, 1);
                buffers.draw(GL_TRIANGLES, shaderProgram);
                if (renderMode == GL_TRIANGLES) {
                    buffers.draw(GL_TRIANGLES, shaderProgram);
                } else {
                    stripBuffers.draw(GL_TRIANGLE_STRIP, shaderProgram);
                }

                model = new Mat4Transl(-2, 2, 0);
                glUniformMatrix4fv(locModel, false, model.floatArray());
                glUniform1i(locSurfaceMode, 5);
                buffers.draw(GL_TRIANGLES, shaderProgram);
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
        } else {
            glUseProgram(objectShaderProgram);
            model = new Mat4Scale(1.0);
            Mat4 rotate= new Mat4(new double[] {
                    1,  0,  0, 0,
                    0, -1,  0, 0,
                    0,  0,  1, 0,
                    0,  0,  0, 1,
            });

            Mat4 mat = rotate.mul(view).mul(projection);

            glUniformMatrix4fv(locObjMat, false, ToFloatArray.convert(mat));

            objectBuffers.draw(objectModel.getTopology(), objectShaderProgram);
        }

        textRenderer.clear();
        textRenderer.addStr2D(20, 20, getColorModeText());
        textRenderer.draw();
    }

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
        }
    };

    private String getColorModeText() {
        switch (colorMode) {
            case 0: return "Color mode: XYZ in observer coordinates";
            case 1: return "Color mode: Depth buffer";
            case 2: return "Color mode: Normal XYZ";
            case 3: return "Color mode: Texture RGBA";
            case 4: return "Color mode: Texture UV";
            case 5: return "Color mode: Lighting without texture";
            case 6: return "Color mode: Complete lighting with texture";
            case 7: return "Color mode: Distance from light";
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
