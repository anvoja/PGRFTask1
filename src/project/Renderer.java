package project;

import helpers.AbstractRenderer;
import lwjglutils.OGLBuffers;
import lwjglutils.OGLUtils;
import lwjglutils.ShaderUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import transforms.*;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

public class Renderer extends AbstractRenderer {
    private OGLBuffers buffers;

    private int renderMode = GL_POINTS;

    private int shaderProgram;

    private Mat4 projection;
    private Mat4 view;
    private Mat4 model;

    private int surfaceMode = 1;private int locProjection;

    private int locView;
    private int locModel;
    private int locTime;
    private int locSurfaceMode;
    private int locLightPosition;
    private int locEyePosition;

    private boolean mousePressed = false;

    private double lastMouseX;
    private double lastMouseY;

    private double azimuth = 0.0;
    private double zenith = 0.7;
    private double cameraDistance = 6.0;

    private GLFWKeyCallback   keyCallback = new GLFWKeyCallback() {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            if (action == GLFW_PRESS || action == GLFW_REPEAT){
                switch (key) {
                    case GLFW_KEY_M:
                        renderMode = GL_TRIANGLE_STRIP;
                        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                        break;
                    case GLFW_KEY_N:
                        renderMode = GL_TRIANGLES;
                        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                        break;
                    case GLFW_KEY_B:
                        renderMode = GL_TRIANGLES;
                        glPolygonMode(GL_FRONT_AND_BACK, GL_POINT);
                        break;
                    case GLFW_KEY_V:
                        renderMode = GL_TRIANGLES;
                        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
                        break;
                    case GLFW_KEY_1:
                        surfaceMode = 0; // plane
                        break;
                    case GLFW_KEY_2:
                        surfaceMode = 1; // wave
                        break;
                    case GLFW_KEY_3:
                        surfaceMode = 2; // hill
                        break;
                    case GLFW_KEY_4:
                        surfaceMode = 3; // saddle
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
        // grid resolution
        int N = 30;

        float[] vertexBufferData = new float[N * N * 2];
        int index = 0;

        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                float fx = -1f + 2f * x / (N - 1);
                float fy = -1f + 2f * y / (N - 1);

                vertexBufferData[index++] = fx;
                vertexBufferData[index++] = fy;
            }
        }

        int[] indexBufferData = new int[(N - 1) * (N - 1) * 6];
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
          new OGLBuffers.Attrib("inPosition", 2, 0)
        };

        buffers = new OGLBuffers(vertexBufferData, 2, attributes, indexBufferData);

        shaderProgram = ShaderUtils.loadProgram("/shader");
        locProjection = glGetUniformLocation(shaderProgram, "projection");
        locView = glGetUniformLocation(shaderProgram, "view");
        locModel = glGetUniformLocation(shaderProgram, "model");
        locTime = glGetUniformLocation(shaderProgram, "time");
        locSurfaceMode = glGetUniformLocation(shaderProgram, "surfaceMode");
        locLightPosition = glGetUniformLocation(shaderProgram, "lightPosition");
        locEyePosition = glGetUniformLocation(shaderProgram, "eyePosition");
    }

    @Override
    public void display() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, width, height);
        if (height == 0) return;
        projection = new Mat4PerspRH(
                60,
                (double) width / height,
                0.1,
                100.0
        );
        double camX = cameraDistance * Math.sin(zenith) * Math.sin(azimuth);
        double camY = cameraDistance * Math.sin(zenith) * Math.cos(azimuth);
        double camZ = cameraDistance * Math.cos(zenith);

        view = new Mat4ViewRH(
                new Vec3D(camX, camY, camZ),
                new Vec3D(0, 0, 0),
                new Vec3D(0, 0, 1)
        );
        model = new Mat4Identity();

        float time = (float) glfwGetTime();

        glUseProgram(shaderProgram);
        glUniformMatrix4fv(locModel, false, model.floatArray());
        glUniformMatrix4fv(locProjection, false, projection.floatArray());
        glUniformMatrix4fv(locView, false, view.floatArray());
        glUniform1f(locTime, time);
        glUniform1i(locSurfaceMode, surfaceMode);
        glUniform3f(locLightPosition, 2.0f, -3.0f, 4.0f);
        glUniform3f(locEyePosition, (float) camX, (float) camY, (float) camZ);

        buffers.draw(renderMode, shaderProgram);
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

            azimuth -= dx * 0.01;
            zenith += dy * 0.01;

            // prevent camera flipping upside down
            zenith = Math.max(0.1, Math.min(Math.PI - 0.1, zenith));
        }
    };

    private GLFWScrollCallback scrollCallback = new GLFWScrollCallback() {
        @Override
        public void invoke(long window, double dx, double dy) {
            cameraDistance -= dy * 0.5;

            cameraDistance = Math.max(2.0, Math.min(20.0, cameraDistance));
        }
    };

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
