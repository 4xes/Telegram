package com.blackfox.surface.renderer.particle;

import static android.opengl.GLES10.glScissor;

import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLES31;
import android.opengl.Matrix;

import org.telegram.messenger.R;
import com.blackfox.surface.renderer.EglUtils;

import org.telegram.messenger.AndroidUtilities;

public class ImageRenderer {
    private static final float[] VERTICES_DATA = new float[]{
            // X, Y, Z, U, V
            -1.0f, 1.0f, 0.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 0.0f, 1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, -1.0f, 0.0f, 1.0f, 1.0f
    };

    private static final int FLOAT_SIZE_BYTES = 4;
    protected static final int VERTICES_DATA_POS_SIZE = 3;
    protected static final int VERTICES_DATA_UV_SIZE = 2;
    protected static final int VERTICES_DATA_STRIDE_BYTES = (VERTICES_DATA_POS_SIZE + VERTICES_DATA_UV_SIZE) * FLOAT_SIZE_BYTES;
    protected static final int VERTICES_DATA_POS_OFFSET = 0 * FLOAT_SIZE_BYTES;
    protected static final int VERTICES_DATA_UV_OFFSET = VERTICES_DATA_POS_OFFSET + VERTICES_DATA_POS_SIZE * FLOAT_SIZE_BYTES;

    private int vertexShader;
    private int fragmentShader;
    private int program;
    private int vertexBufferName;
    private int aPositionHandle = -1;
    private int aTextureCoordHandle = -1;
    private int uMatrixHandle = -1;
    private int uTextureHandle = -1;

    private int width = 0;
    private int height = 0;

    private float[] uMatrix = new float[16];

    private RectF src = new RectF(-1, -1, 1, 1);
    private RectF dst = new RectF();

    public ImageRenderer() {
        Matrix.setIdentityM(uMatrix, 0);
    }

    public void init() {
        vertexShader = EglUtils.createVertexShader(AndroidUtilities.readRes(R.raw.dust_image_vertex));
        fragmentShader = EglUtils.createFragmentShader(AndroidUtilities.readRes(R.raw.dust_image_fragment));
        program = EglUtils.createProgram(vertexShader, fragmentShader, null);
        vertexBufferName = EglUtils.createBuffer(VERTICES_DATA);
        GLES20.glUseProgram(program);

        aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        aTextureCoordHandle = GLES20.glGetAttribLocation(program, "aTextureCoord");
        // View projection transformation matrix handler
        uMatrixHandle = GLES20.glGetUniformLocation(program, "uMatrix");

        uTextureHandle = GLES20.glGetUniformLocation(program, "uTexture");
    }

    public void setPosition(DustRequest request) {
        Matrix.setIdentityM(uMatrix, 0);
        float scaleX = (float) request.getWidth() / width;
        float scaleY = (float) request.getHeight() / height;

        float translateX = (float) request.getOffsetX() / width;
        float translateY = (float) request.getOffsetY() / height;

        dst.left = src.left + src.width() * translateX;
        dst.top = src.top + src.height() * translateY;
        dst.right = dst.left + src.width() * scaleX;
        dst.bottom = dst.top + src.height() * scaleY;

        float translateMatrixX = dst.centerX() - src.centerX();
        float translateMatrixY = src.centerY() - dst.centerY();
        Matrix.translateM(uMatrix, 0, translateMatrixX, translateMatrixY, 0f);
        Matrix.scaleM(uMatrix, 0, scaleX, scaleY, 1f);
    }
    public void draw(DustRequest request, int textureIndex, float time, float deltaTime) {
        GLES20.glUseProgram(program);
        GLES20.glUniformMatrix4fv(uMatrixHandle, 1, false, uMatrix, 0);
        GLES20.glUniform1i(uTextureHandle, textureIndex);
        //Pass quadrant position to shader
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferName);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, VERTICES_DATA_POS_SIZE, GLES20.GL_FLOAT, false, VERTICES_DATA_STRIDE_BYTES, VERTICES_DATA_POS_OFFSET);
        GLES20.glEnableVertexAttribArray(aTextureCoordHandle);
        GLES20.glVertexAttribPointer(aTextureCoordHandle, VERTICES_DATA_UV_SIZE, GLES20.GL_FLOAT, false, VERTICES_DATA_STRIDE_BYTES, VERTICES_DATA_UV_OFFSET);

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glEnableVertexAttribArray(aTextureCoordHandle);

        //float effectFraction = Math.max(0.0f, Math.min(0.8f, time)) / 0.8f;

        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        float t = Math.min(time * 2.0f, 1.0f);
        int clip = request.getOffsetX() + (int) (request.getWidth() * t);
        glScissor(clip, 0, width - clip, height);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(aPositionHandle);
        GLES20.glDisableVertexAttribArray(aTextureCoordHandle);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void cleanup() {
        if (program != 0) {
            try { GLES31.glDeleteProgram(program); } catch (Exception e) { EglUtils.log(e); };
            program = 0;
        }
        if (vertexShader != 0) {
            try { GLES31.glDeleteShader(vertexShader); } catch (Exception e) { EglUtils.log(e); };
            vertexShader = 0;
        }
        if (fragmentShader != 0) {
            try { GLES31.glDeleteShader(fragmentShader); } catch (Exception e) { EglUtils.log(e); };
            vertexShader = 0;
        }
        if (vertexBufferName != 0) {
            try { GLES20.glDeleteBuffers(1, new int[]{vertexBufferName}, 0); } catch (Exception e) { EglUtils.log(e); };
            vertexBufferName = 0;
        }
    }
}