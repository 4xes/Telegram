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

    public GradientRenderer(Context context) {
        shader = Shader.read(context, R.raw.gradient_vertex_shader,  R.raw.gradient_fragment_shader);
    }

    private void initVertexData() {
        float[] vertices = {
                -1,  1,
                -1, -1,
                1,  1,
                1, -1
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
