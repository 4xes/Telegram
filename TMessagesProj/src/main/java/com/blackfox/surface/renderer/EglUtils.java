package com.blackfox.surface.renderer;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_NO_ERROR;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.GL_TRUE;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetError;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLES31;
import android.opengl.GLUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class EglUtils {

    private final static String TAG = "EglErrors";
    public static final int NO_TEXTURE = -1;

    public static void log(Exception e) {
        Log.e(TAG, e.toString());
    }

    public static void log(String message) {
        Log.e(TAG, message);
    }

    public static void checkGlErrors(String message) {
        int err;
        while ((err = GLES31.glGetError()) != GLES31.GL_NO_ERROR) {
            Log.e(TAG, "GL error  " + message + ": " + err);
        }
    }
    public static int createVertexShader(@NonNull String shader) {
        return loadShader(GLES31.GL_VERTEX_SHADER, shader);
    }

    public static int createFragmentShader(@NonNull String shader) {
        return loadShader(GL_FRAGMENT_SHADER, shader);
    }

    public static int createProgram(int vertexShader, int fragmentShader, @Nullable String[] feedbackVaryings) {
        if (vertexShader == 0) {
            return ERROR_PROGRAM;
        }
        if (fragmentShader == 0) {
            return ERROR_PROGRAM;
        }
        int program = glCreateProgram();
        checkGlError("glCreateProgram");
        if (program == 0) {
            log("Could not create program");
            log(glGetProgramInfoLog(program));
            return ERROR_PROGRAM;
        }
        glAttachShader(program, vertexShader);
        checkGlError("glAttachShader type=" + shaderTypeToString(GL_VERTEX_SHADER));
        glAttachShader(program, fragmentShader);
        checkGlError("glAttachShader type=" + shaderTypeToString(GL_FRAGMENT_SHADER));
        if (feedbackVaryings != null) {
            GLES31.glTransformFeedbackVaryings(program, feedbackVaryings, GLES31.GL_INTERLEAVED_ATTRIBS);
        }
        glLinkProgram(program);
        final int[] linkStatus = new int[1];
        glGetProgramiv(program, GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GL_TRUE) {
            log("Could not link program :");
            log(glGetProgramInfoLog(program));
            glDeleteProgram(program);
            log("Could not link program");
            return 0;
        }

        return program;
    }

    private static int loadShader(int shaderType, String shaderSource) {
        final int shader = glCreateShader(shaderType);
        checkGlError("glCreateShader type=" + shaderTypeToString(shaderType));
        glShaderSource(shader, shaderSource);
        glCompileShader(shader);

        final int[] compileStatus = new int[1];
        glGetShaderiv(shader, GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] != GL_TRUE) {
            final String infoLog = glGetShaderInfoLog(shader);
            log("Could not compile shader " + shaderType + ":");
            log(infoLog);
            glDeleteShader(shader);
            log("Could not compile shader " + shaderType  + " " + infoLog);
            return 0;
        }
        return shader;
    }

    private static String shaderTypeToString(int shaderType) {
        if (shaderType == GL_VERTEX_SHADER) {
            return "GL_VERTEX_SHADER";
        } else {
            return "GL_FRAGMENT_SHADER";
        }
    }

    private static void checkGlError(String msg) {
        int error;
        //noinspection LoopStatementThatDoesntLoop
        while ((error = glGetError()) != GL_NO_ERROR) {
            log(msg + ": GLES error: 0x" + Integer.toHexString(error));
        }
    }

    public static int loadTexture(final Bitmap img) {
        if (img == null) {
            return NO_TEXTURE;
        }
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, img, 0);
        return textures[0];
    }

    public static final int BYTES_PER_FLOAT = 4;
    public static final int BYTES_PER_SHORT = 2;

    public static FloatBuffer toFloatBuffer(final float[] data) {
        final FloatBuffer buffer = ByteBuffer
                .allocateDirect(data.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(data).position(0);
        return buffer;
    }

    public static int createBuffer(final float[] data) {
        return createBuffer(toFloatBuffer(data));
    }

    public static int createBuffer(final FloatBuffer data) {
        final int[] buffers = new int[1];
        GLES20.glGenBuffers(buffers.length, buffers, 0);
        updateBufferData(buffers[0], data);
        return buffers[0];
    }

    public static void updateBufferData(final int bufferName, final FloatBuffer data) {
        GLES20.glBindBuffer(GL_ARRAY_BUFFER, bufferName);
        GLES20.glBufferData(GL_ARRAY_BUFFER, data.capacity() * BYTES_PER_FLOAT, data, GL_STATIC_DRAW);
        GLES20.glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public static int ERROR_PROGRAM = 0;
}