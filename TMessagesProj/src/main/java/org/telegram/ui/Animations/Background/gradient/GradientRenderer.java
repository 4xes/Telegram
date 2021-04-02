package org.telegram.ui.Animations.Background.gradient;

import android.content.Context;
import android.graphics.Color;

import org.telegram.messenger.R;
import org.telegram.ui.Animations.Background.GLTextureView;
import org.telegram.ui.Animations.Background.GlUtils;
import org.telegram.ui.Animations.Background.Shader;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform2f;
import static android.opengl.GLES20.glUniform2fv;
import static android.opengl.GLES20.glUniform3f;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;


public class GradientRenderer implements GLTextureView.Renderer {
    private final Shader shader;
    private int programId;

    private FloatBuffer vertexData;

    private int aPositionLocation;
    private int uResolutionLocation;

    private int uColor1Location;
    private int uColor2Location;
    private int uColor3Location;
    private int uColor4Location;

    private int uPoint1Location;
    private int uPoint2Location;
    private int uPoint3Location;
    private int uPoint4Location;

    public final int[] colors = new int[4];
    public final float[][] points = new float[4][2];


    private static final String defaultVertexShader =
            "attribute vec4 a_Position;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = a_Position;\n" +
            "}";
    private static final String gradientFragmentShader =
            "precision highp float;\n" +
            "\n" +
            "uniform vec3 color1;\n" +
            "uniform vec3 color2;\n" +
            "uniform vec3 color3;\n" +
            "uniform vec3 color4;\n" +
            "uniform vec2 p1;\n" +
            "uniform vec2 p2;\n" +
            "uniform vec2 p3;\n" +
            "uniform vec2 p4;\n" +
            "uniform vec2 resolution;\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 uv = gl_FragCoord.xy / resolution;\n" +
            "    uv.y = 1.0 - uv.y;\n" +
            "\n" +
            "    float dp1 = distance(uv, p1);\n" +
            "    float dp2 = distance(uv, p2);\n" +
            "    float dp3 = distance(uv, p3);\n" +
            "    float dp4 = distance(uv, p4);\n" +
            "    float minD = min(dp1, min(dp2, min(dp3, dp4)));\n" +
            "    float p = 5.0;\n" +
            "    dp1 = pow(1.0 - (dp1 - minD), p);\n" +
            "    dp2 = pow(1.0 - (dp2 - minD), p);\n" +
            "    dp3 = pow(1.0 - (dp3 - minD), p);\n" +
            "    dp4 = pow(1.0 - (dp4 - minD), p);\n" +
            "    float sumDp = dp1 + dp2 + dp3 + dp4;\n" +
            "\n" +
            "    vec3 color = (color1 * dp1 + color2 * dp2 + color3 * dp3 + color4 * dp4) / sumDp;\n" +
            "    gl_FragColor = vec4(color, 1.0);\n" +
            "}";

    public GradientRenderer(Context context) {
        //shader = Shader.read(context, R.raw.gradient_vertex_shader,  R.raw.gradient_fragment_shader);
        shader = new Shader(defaultVertexShader, gradientFragmentShader);
    }

    private void initVertexData() {
        float[] vertices = {
                -1f,  1f,
                -1f, -1f,
                1f,  1f,
                1f, -1f
        };
        vertexData = GlUtils.toBuffer(vertices);
    }

    private void bindVertexData() {
        aPositionLocation = glGetAttribLocation(programId, "a_Position");
        vertexData.position(0);
        glVertexAttribPointer(aPositionLocation, 2, GL_FLOAT,
                false, 0, vertexData);
        glEnableVertexAttribArray(aPositionLocation);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(0f, 0f, 0f, 1f);
        int vertexShader = GlUtils.createShader(shader.vertexShader, GL_VERTEX_SHADER);
        int fragmentShader = GlUtils.createShader(shader.fragmentShader, GL_FRAGMENT_SHADER);
        programId = GlUtils.createProgram(vertexShader, fragmentShader);
        glUseProgram(programId);

        getLocations();
        initVertexData();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        glViewport(0, 0, width, height);
        bindVertexData();
        bindResolution(width, height);
        bindValues();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        glClear(GL_COLOR_BUFFER_BIT);
        bindValues();

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }

    private void getLocations() {
        aPositionLocation = glGetAttribLocation(programId, "a_position");
        uResolutionLocation = glGetUniformLocation(programId, "resolution");
        uColor1Location = glGetUniformLocation(programId, "color1");
        uColor2Location = glGetUniformLocation(programId, "color2");
        uColor3Location = glGetUniformLocation(programId, "color3");
        uColor4Location = glGetUniformLocation(programId, "color4");

        uPoint1Location = glGetUniformLocation(programId, "p1");
        uPoint2Location = glGetUniformLocation(programId, "p2");
        uPoint3Location = glGetUniformLocation(programId, "p3");
        uPoint4Location = glGetUniformLocation(programId, "p4");
    }

    private void bindResolution(int width, int height) {
        glUniform2f(uResolutionLocation, width, height);
    }

    public void setColors(int[] colors) {
        for (int i = 0; i < 4; i++) {
            this.colors[i] = colors[i];
        }
    }

    public void setPoints(float[][] points) {
        for (int i = 0; i < 4; i++) {
            this.points[i][0] = points[i][0];
            this.points[i][1] = points[i][1];
        }
    }

    private void bindValues() {
        setColor(uColor1Location, colors[0]);
        setColor(uColor2Location, colors[1]);
        setColor(uColor3Location, colors[2]);
        setColor(uColor4Location, colors[3]);
        setPoint(uPoint1Location, points[0]);
        setPoint(uPoint2Location, points[1]);
        setPoint(uPoint3Location, points[2]);
        setPoint(uPoint4Location, points[3]);
    }

    private void setColor(int location, int color) {
        glUniform3f(location, Color.red(color) / 255f, Color.green(color) / 255f, Color.blue(color) / 255f);
    }

    private void setPoint(int location, float[] point) {
        glUniform2fv(location, 1, point, 0);
    }

    private static final String TAG = GradientRenderer.class.getSimpleName();
}
