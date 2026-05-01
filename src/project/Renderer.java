package project;

import helpers.AbstractRenderer;
import lwjglutils.OGLBuffers;
import lwjglutils.OGLUtils;
import lwjglutils.ShaderUtils;
import org.lwjgl.glfw.GLFWKeyCallback;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glUseProgram;

public class Renderer extends AbstractRenderer {
    private OGLBuffers buffers;
    private int renderMode = GL_POINTS;
    private int shaderProgram;

    private GLFWKeyCallback   keyCallback = new GLFWKeyCallback() {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            if (action == GLFW_PRESS || action == GLFW_REPEAT){
                switch (key) {
                    case GLFW_KEY_M:
                        renderMode = GL_POINTS;
                        break;
                    case GLFW_KEY_N:
                        renderMode = GL_LINES;
                        break;
                    case GLFW_KEY_B:
                        renderMode = GL_TRIANGLES;
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
        // grid resolution
        int N = 20;

        float[] vertexBufferData = {
                -0.5f, -0.5f,
                0.5f, -0.5f,
                0.0f,  0.5f
        };

        int[] indexBufferData = {
                0, 1, 2
        };

        OGLBuffers.Attrib[] attributes = {
          new OGLBuffers.Attrib("inPosition", 2, 0)
        };

        buffers = new OGLBuffers(vertexBufferData, 2, attributes, indexBufferData);

        shaderProgram = ShaderUtils.loadProgram("/shader");
    }

    @Override
    public void display() {
        glUseProgram(shaderProgram);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, width, height);
        buffers.draw(renderMode, shaderProgram);
    }

    @Override
    public GLFWKeyCallback getKeyCallback() {
        return keyCallback;
    }
}
