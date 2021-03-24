package org.telegram.ui.Animations.Background;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_NO_ERROR;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_CUBE_MAP;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_TRUE;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetError;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glTexParameterf;

public class GlUtils {
    private static final String TAG = GlUtils.class.getSimpleName();

    public static int createShader(@NonNull String shader, int type) {
        return loadShader(type, shader);
    }

    public static int createProgram(int vertexShader, int fragmentShader) {
        if (vertexShader == 0) {
            return 0;
        }
        if (fragmentShader == 0) {
            return 0;
        }
        int program = glCreateProgram();
        checkGlError("glCreateProgram");
        if (program == 0) {
            Log.e(TAG, "Could not create program");
        }
        glAttachShader(program, vertexShader);
        checkGlError("glAttachShader type=" + shaderTypeToString(GL_VERTEX_SHADER));
        glAttachShader(program, fragmentShader);
        checkGlError("glAttachShader type=" + shaderTypeToString(GL_FRAGMENT_SHADER));
        glLinkProgram(program);
        final int[] linkStatus = new int[1];
        glGetProgramiv(program, GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GL_TRUE) {
            Log.e(TAG, "Could not link program :");
            Log.e(TAG, glGetProgramInfoLog(program));
            glDeleteProgram(program);
            throw new IllegalStateException("Could not link program");
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
            Log.e(TAG, "Could not compile shader " + shaderType + ":");
            Log.e(TAG, glGetShaderInfoLog(shader));
            glDeleteShader(shader);
            throw new IllegalStateException("Could not compile shader " + shaderType);
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
            throw new IllegalStateException(
                    msg + ": GLES20 error: 0x" + Integer.toHexString(error));
        }
    }

    public static void setupSampler(final int target) {
        GLES20.glTexParameterf(target, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        GLES20.glTexParameterf(target, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        GLES20.glTexParameteri(target, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(target, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }

    public static int loadTextures(Context context, int[] resIds) {
        // создание объекта текстуры
        final int[] textureIds = new int[1];
        glGenTextures(1, textureIds, 0);
        if (textureIds[0] == 0) {
            return 0;
        }

        // получение Bitmap
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        Bitmap[] bitmaps = new Bitmap[6];
        for (int i = 0; i < 6; i++) {
            bitmaps[i] = BitmapFactory.decodeResource(
                    context.getResources(), resIds[i], options);

            if (bitmaps[i] == null) {
                glDeleteTextures(1, textureIds, 0);
                return 0;
            }
        }

        // настройка объекта текстуры
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_CUBE_MAP, textureIds[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, bitmaps[0], 0);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, bitmaps[1], 0);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, bitmaps[2], 0);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, bitmaps[3], 0);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, bitmaps[4], 0);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, bitmaps[5], 0);

        for (Bitmap bitmap : bitmaps) {
            bitmap.recycle();
        }

        // сброс target
        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);

        return textureIds[0];
    }

    public static int loadTexture(Context context, @DrawableRes int resId) {
        // создание объекта текстуры

        final int[] texturesIds = new int[1];
        glGenTextures(1, texturesIds, 0);
        if (texturesIds[0] == 0) {
            return 0;
        }
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId, options);

        if (bitmap == null) {
            glDeleteTextures(1, texturesIds, 0);
            return 0;
        }

        // настройка объекта текстуры
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texturesIds[0]);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();

        // сброс target
        glBindTexture(GL_TEXTURE_2D, 0);


        return texturesIds[0];
    }

    private static final int BYTES_PER_FLOAT = 4;
    private static final int BYTES_PER_SHORT = 2;


    public static FloatBuffer toBuffer(float[] data) {
        FloatBuffer buffer = ByteBuffer
                .allocateDirect(data.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(data).position(0);
        return buffer;
    }

    public static ShortBuffer toBuffer(short[] data) {
        ShortBuffer buffer = ByteBuffer
                .allocateDirect(data.length * BYTES_PER_SHORT)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        buffer.put(data).position(0);
        return buffer;
    }

    public static int createBuffer(final float[] data) {
        return createBuffer(toBuffer(data));
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
}
