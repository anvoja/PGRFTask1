package project;

import helpers.AbstractRenderer;
import lwjglutils.OGLBuffers;
import lwjglutils.OGLUtils;
import lwjglutils.ShaderUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
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
    private int locProjection;
    private int locView;
    private int locModel;

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
        int N = 20;

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
        System.out.println("locProjection = " + locProjection);
        System.out.println("locView = " + locView);
        System.out.println("width = " + width + ", height = " + height);
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
        view = new Mat4ViewRH(
                new Vec3D(1.5, 0, -2),
                new Vec3D(0, 0, 0),
                new Vec3D(0, 0, 1)
        );
        model = new Mat4Identity();

        glUseProgram(shaderProgram);
        glUniformMatrix4fv(locModel, false, model.floatArray());
        glUniformMatrix4fv(locProjection, false, projection.floatArray());
        glUniformMatrix4fv(locView, false, view.floatArray());

        buffers.draw(renderMode, shaderProgram);
    }

    @Override
    public GLFWKeyCallback getKeyCallback() {
        return keyCallback;
    }

    protected GLFWCursorPosCallback cursorPosCallback = new GLFWCursorPosCallback() {
        @Override
        public void invoke(long window, double x, double y) {
            // System.out.println("Cursor position [" + x + ", " + y + "]");
        }
    };
}
